package com.lernathon.recruitment.config;

import com.lernathon.recruitment.entity.User;
import com.lernathon.recruitment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Create default admin user
        if (userRepository.findByEmail("siveshkommalapati167@gmail.com").isEmpty()) {
            log.info("Creating default admin user...");
            
            User admin = User.builder()
                    .email("siveshkommalapati167@gmail.com")
                    .password(passwordEncoder.encode("Sivesh@167")) // Default password: admin123
                    .firstName("SIVESH")
                    .lastName("KOMMALAPATI")
                    .role(User.Role.ADMIN)
                    .enabled(true)
                    .build();
            
            userRepository.save(admin);
            log.info("Default admin user created successfully!");
            log.info("Email: siveshkommalapati167@gmail.com | Password: Sivesh@167");
        } else {
            log.info("Admin user already exists.");
        }
        
        // Create sample recruiter user for testing
        if (userRepository.findByEmail("recruiter@lernathon.com").isEmpty()) {
            log.info("Creating sample recruiter user...");
            
            User recruiter = User.builder()
                    .email("recruiter@lernathon.com")
                    .password(passwordEncoder.encode("recruiter123"))
                    .firstName("Sarah")
                    .lastName("Johnson")
                    .role(User.Role.RECRUITER)
                    .enabled(true)
                    .build();
            
            userRepository.save(recruiter);
            log.info("Sample recruiter created: recruiter@lernathon.com | Password: recruiter123");
        }
        
       
        log.info("=== User Creation Summary ===");
        log.info("ADMIN: siveshkommalapati167@gmail.com / Sivesh@167");
        log.info("RECRUITER: recruiter@lernathon.com / recruiter123");
        
        log.info("Please change passwords after first login for security.");
    }
}
