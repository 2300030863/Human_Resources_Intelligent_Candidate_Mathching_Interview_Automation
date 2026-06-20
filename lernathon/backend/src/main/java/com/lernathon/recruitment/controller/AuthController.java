package com.lernathon.recruitment.controller;

import com.lernathon.recruitment.dto.AuthRequest;
import com.lernathon.recruitment.dto.AuthResponse;
import com.lernathon.recruitment.dto.ForgotPasswordRequest;
import com.lernathon.recruitment.dto.RegisterRequest;
import com.lernathon.recruitment.dto.ResetPasswordRequest;
import com.lernathon.recruitment.entity.Candidate;
import com.lernathon.recruitment.entity.User;
import com.lernathon.recruitment.repository.CandidateRepository;
import com.lernathon.recruitment.repository.UserRepository;
import com.lernathon.recruitment.security.JwtService;
import com.lernathon.recruitment.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor@SuppressWarnings("null")public class AuthController {

    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().build();
        }

        var user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(request.getRole() != null ? request.getRole() : User.Role.CANDIDATE)
                .enabled(true)
                .build();

        userRepository.save(user);
        var jwtToken = jwtService.generateToken(user);

        // Create Candidate profile if user is a candidate
        Long candidateId = null;
        if (user.getRole() == User.Role.CANDIDATE) {
            var candidate = Candidate.builder()
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .phone("")
                    .skills("")
                    .experienceYears(0)
                    .status(Candidate.CandidateStatus.NEW)
                    .build();
            candidateId = candidateRepository.save(candidate).getId();
        }

        return ResponseEntity.ok(AuthResponse.builder()
                .token(jwtToken)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .candidateId(candidateId)
                .userId(user.getId())
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();
        var jwtToken = jwtService.generateToken(user);

        // Get or create candidate ID if user is a candidate
        Long candidateId = null;
        if (user.getRole() == User.Role.CANDIDATE) {
            var existingCandidate = candidateRepository.findByEmail(user.getEmail());
            if (existingCandidate.isPresent()) {
                candidateId = existingCandidate.get().getId();
            } else {
                // Create Candidate profile for existing user who doesn't have one
                var candidate = Candidate.builder()
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .phone("")
                        .skills("")
                        .experienceYears(0)
                        .status(Candidate.CandidateStatus.NEW)
                        .build();
                candidateId = candidateRepository.save(candidate).getId();
            }
        }

        return ResponseEntity.ok(AuthResponse.builder()
                .token(jwtToken)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .candidateId(candidateId)
                .userId(user.getId())
                .build());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<java.util.Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.initiatePasswordReset(request.getEmail());
        return ResponseEntity.ok(java.util.Map.of(
            "message", "If this email is registered, a reset link has been sent."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<java.util.Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(java.util.Map.of("message", "Password reset successful"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }
}
