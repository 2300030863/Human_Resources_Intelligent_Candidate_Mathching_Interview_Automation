package com.lernathon.recruitment.controller;

import com.lernathon.recruitment.dto.MatchingResponse;
import com.lernathon.recruitment.dto.UpdateStatusRequest;
import com.lernathon.recruitment.entity.Application;
import com.lernathon.recruitment.service.ApplicationService;
import com.lernathon.recruitment.service.MatchingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final MatchingService matchingService;

    @GetMapping
    public ResponseEntity<List<Application>> getAllApplications() {
        return ResponseEntity.ok(applicationService.getAllApplications());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Application> getApplicationById(@PathVariable Long id) {
        return ResponseEntity.ok(applicationService.getApplicationById(id));
    }

    @PostMapping
    public ResponseEntity<Application> createApplication(
            @RequestParam Long candidateId,
            @RequestParam Long jobId,
            @RequestBody Application application
    ) {
        return ResponseEntity.ok(applicationService.createApplication(candidateId, jobId, application));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Application> updateApplication(
            @PathVariable Long id,
            @RequestBody Application application
    ) {
        return ResponseEntity.ok(applicationService.updateApplication(id, application));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable Long id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/submit")
    public ResponseEntity<Application> submitApplication(
            @RequestParam Long jobId,
            @RequestParam("resume") MultipartFile resume,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String skills,
            @RequestParam(required = false) Integer experienceYears,
            @RequestParam(required = false) String coverLetter,
            @RequestParam(required = false) String currentCompany,
            @RequestParam(required = false) String employmentVerificationJson,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(applicationService.submitApplicationWithResume(
                userEmail, jobId, resume, firstName, lastName, phone, skills,
                experienceYears, coverLetter, currentCompany, employmentVerificationJson
        ));
    }

    @GetMapping("/candidate/{candidateId}")
    public ResponseEntity<List<Application>> getApplicationsByCandidate(@PathVariable Long candidateId) {
        return ResponseEntity.ok(applicationService.getApplicationsByCandidate(candidateId));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<Application>> getApplicationsByJob(@PathVariable Long jobId) {
        return ResponseEntity.ok(applicationService.getApplicationsByJob(jobId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Application>> getApplicationsByStatus(
            @PathVariable Application.ApplicationStatus status
    ) {
        return ResponseEntity.ok(applicationService.getApplicationsByStatus(status));
    }

    @GetMapping("/job/{jobId}/matches")
    public ResponseEntity<List<MatchingResponse>> getMatchingCandidates(@PathVariable Long jobId) {
        return ResponseEntity.ok(applicationService.getMatchingCandidatesForJob(jobId));
    }

    @PostMapping("/{id}/calculate-match")
    public ResponseEntity<Application> calculateMatchScore(@PathVariable Long id) {
        Application application = applicationService.getApplicationById(id);
        matchingService.updateApplicationMatchScore(application);
        return ResponseEntity.ok(application);
    }

    @PostMapping("/job/{jobId}/recalculate-all")
    public ResponseEntity<Void> recalculateAllMatches(@PathVariable Long jobId) {
        List<Application> applications = applicationService.getApplicationsByJob(jobId);
        applications.forEach(matchingService::updateApplicationMatchScore);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Application> updateApplicationStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(applicationService.updateApplicationStatus(id, request));
    }
}
