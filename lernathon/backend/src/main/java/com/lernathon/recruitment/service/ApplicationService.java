package com.lernathon.recruitment.service;

import com.lernathon.recruitment.dto.MatchingResponse;
import com.lernathon.recruitment.dto.UpdateStatusRequest;
import com.lernathon.recruitment.entity.Application;
import com.lernathon.recruitment.entity.Candidate;
import com.lernathon.recruitment.entity.Job;
import com.lernathon.recruitment.entity.User;
import com.lernathon.recruitment.repository.ApplicationRepository;
import com.lernathon.recruitment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CandidateService candidateService;
    private final JobService jobService;
    private final MatchingService matchingService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<Application> getAllApplications() {
        return applicationRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public Application getApplicationById(Long id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + id));
        // Force initialization of lazy relationships
        if (application.getCandidate() != null) {
            application.getCandidate().getFirstName();
        }
        if (application.getJob() != null) {
            application.getJob().getTitle();
        }
        return application;
    }

    @Transactional
    public Application createApplication(Long candidateId, Long jobId, Application application) {
        if (applicationRepository.existsByCandidateIdAndJobId(candidateId, jobId)) {
            throw new RuntimeException("Application already exists for this candidate and job");
        }
        
        Candidate candidate = candidateService.getCandidateById(candidateId);
        Job job = jobService.getJobById(jobId);
        
        // Check if applications are allowed for this job
        if (!jobService.isApplicationAllowed(jobId)) {
            throw new RuntimeException("Applications are not allowed for this job: " + jobService.getApplicationStatusMessage(jobId));
        }
        
        application.setCandidate(candidate);
        application.setJob(job);
        
        if (application.getStatus() == null) {
            application.setStatus(Application.ApplicationStatus.SUBMITTED);
        }
        
        return applicationRepository.save(application);
    }

    @Transactional
    public Application updateApplication(Long id, Application applicationDetails) {
        Application application = getApplicationById(id);
        
        application.setStatus(applicationDetails.getStatus());
        application.setMatchScore(applicationDetails.getMatchScore());
        application.setCoverLetter(applicationDetails.getCoverLetter());
        application.setNotes(applicationDetails.getNotes());
        
        return applicationRepository.save(application);
    }

    @Transactional
    public void deleteApplication(Long id) {
        Application application = getApplicationById(id);
        applicationRepository.delete(application);
    }

    @Transactional(readOnly = true)
    public List<Application> getApplicationsByCandidate(Long candidateId) {
        List<Application> applications = applicationRepository.findByCandidateId(candidateId);
        applications.forEach(app -> {
            if (app.getCandidate() != null) app.getCandidate().getFirstName();
            if (app.getJob() != null) app.getJob().getTitle();
        });
        return applications;
    }

    @Transactional(readOnly = true)
    public List<Application> getApplicationsByJob(Long jobId) {
        List<Application> applications = applicationRepository.findByJobId(jobId);
        applications.forEach(app -> {
            if (app.getCandidate() != null) app.getCandidate().getFirstName();
            if (app.getJob() != null) app.getJob().getTitle();
        });
        return applications;
    }

    @Transactional(readOnly = true)
    public List<Application> getApplicationsByStatus(Application.ApplicationStatus status) {
        List<Application> applications = applicationRepository.findByStatus(status);
        applications.forEach(app -> {
            if (app.getCandidate() != null) app.getCandidate().getFirstName();
            if (app.getJob() != null) app.getJob().getTitle();
        });
        return applications;
    }

    @Transactional(readOnly = true)
    public List<MatchingResponse> getMatchingCandidatesForJob(Long jobId) {
        Job job = jobService.getJobById(jobId);
        List<Application> applications = getApplicationsByJob(jobId);
        
        return applications.stream()
                .map(app -> {
                    Candidate candidate = app.getCandidate();
                    double matchScore = app.getMatchScore() != null ? app.getMatchScore() : 
                            matchingService.calculateMatchScore(candidate, job);
                    
                    Set<String> candidateSkills = extractSkills(candidate.getSkills());
                    Set<String> requiredSkills = extractSkills(job.getSkillsRequired());
                    
                    List<String> matched = candidateSkills.stream()
                            .filter(requiredSkills::contains)
                            .collect(Collectors.toList());
                    
                    List<String> missing = requiredSkills.stream()
                            .filter(skill -> !candidateSkills.contains(skill))
                            .collect(Collectors.toList());
                    
                    // Detect if any company in the verification JSON is SUSPICIOUS (FAKE)
                    String evJson = candidate.getEmploymentVerificationJson();
                    boolean suspect = evJson != null && evJson.contains("\"SUSPICIOUS\"");
                    int verifiedExp = matchingService.calculateVerifiedExperienceYears(candidate);

                    return MatchingResponse.builder()
                            .candidateId(candidate.getId())
                            .candidateName(candidate.getFirstName() + " " + candidate.getLastName())
                            .email(candidate.getEmail())
                            .matchScore(matchScore)
                            .matchedSkills(matched)
                            .missingSkills(missing)
                            .experienceYears(candidate.getExperienceYears())
                            .verifiedExperienceYears(verifiedExp)
                            .status(app.getStatus().name())
                            .employmentVerificationJson(evJson)
                            .experienceSuspect(suspect)
                            .build();
                })
                .sorted((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()))
                .collect(Collectors.toList());
    }

    @Transactional
    public Application submitApplicationWithResume(String userEmail, Long jobId, 
                                                   MultipartFile resume, String firstName, String lastName,
                                                   String phone, String skills, Integer experienceYears,
                                                   String coverLetter, String currentCompany,
                                                   String employmentVerificationJson) {
        // Find or create candidate profile for this user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Job job = jobService.getJobById(jobId);
        
        // Check if application already exists
        Candidate candidate = candidateService.getCandidateByEmail(userEmail);
        if (candidate == null) {
            // Create new candidate profile from user
            candidate = Candidate.builder()
                    .email(user.getEmail())
                    .firstName(firstName != null && !firstName.isEmpty() ? firstName : user.getFirstName())
                    .lastName(lastName != null && !lastName.isEmpty() ? lastName : user.getLastName())
                    .phone(phone != null ? phone : "")
                    .skills(skills != null && !skills.isEmpty() ? skills : "")
                    .experienceYears(experienceYears != null && experienceYears > 0 ? experienceYears : 0)
                    .education("")
                    .currentCompany(currentCompany)
                    .employmentVerificationJson(employmentVerificationJson)
                    .build();
            candidate = candidateService.createCandidate(candidate);
        } else {
            // Update candidate info if provided (manually entered values take priority)
            boolean updated = false;
            if (firstName != null && !firstName.isEmpty()) {
                candidate.setFirstName(firstName);
                updated = true;
            }
            if (lastName != null && !lastName.isEmpty()) {
                candidate.setLastName(lastName);
                updated = true;
            }
            if (phone != null && !phone.isEmpty()) {
                candidate.setPhone(phone);
                updated = true;
            }
            if (skills != null && !skills.isEmpty()) {
                candidate.setSkills(skills);
                updated = true;
            }
            if (experienceYears != null && experienceYears > 0) {
                candidate.setExperienceYears(experienceYears);
                updated = true;
            }
            if (currentCompany != null && !currentCompany.isEmpty()) {
                candidate.setCurrentCompany(currentCompany);
                updated = true;
            }
            if (employmentVerificationJson != null && !employmentVerificationJson.isEmpty()) {
                candidate.setEmploymentVerificationJson(employmentVerificationJson);
                updated = true;
            }
            if (updated) {
                candidate = candidateService.updateCandidate(candidate.getId(), candidate);
            }
        }
        
        // Check for existing application
        if (applicationRepository.existsByCandidateIdAndJobId(candidate.getId(), jobId)) {
            throw new RuntimeException("You have already applied for this job");
        }
        
        // Save resume file
        String resumePath = null;
        if (resume != null && !resume.isEmpty()) {
            resumePath = saveResumeFile(resume, user.getId());
        }
        
        // Create application
        Application application = Application.builder()
                .candidate(candidate)
                .job(job)
                .status(Application.ApplicationStatus.SUBMITTED)
                .coverLetter(coverLetter)
                .notes(resumePath != null ? "Resume: " + resumePath : null)
                .build();
        
        Application savedApplication = applicationRepository.save(application);
        
        // Calculate match score
        matchingService.updateApplicationMatchScore(savedApplication);
        
        // Send application confirmation email asynchronously
        notificationService.notifyApplicationSubmitted(
                candidate.getEmail(),
                candidate.getFirstName() + " " + candidate.getLastName(),
                job.getTitle()
        );
        
        // Auto-mark EXAM_ELIGIBLE and notify if match score >= 80%
        if (savedApplication.getMatchScore() != null && savedApplication.getMatchScore() >= 80.0) {
            try {
                savedApplication.setStatus(Application.ApplicationStatus.EXAM_ELIGIBLE);
                savedApplication = applicationRepository.save(savedApplication);
                log.info("Candidate {} marked EXAM_ELIGIBLE (Match: {}%).",
                        candidate.getId(), savedApplication.getMatchScore());
                notificationService.notifyStatusUpdate(savedApplication);
            } catch (Exception e) {
                log.error("Failed to update exam eligibility for application {}", savedApplication.getId(), e);
            }
        } else {
            // AI score < 80% → reject immediately and notify candidate
            try {
                double score = savedApplication.getMatchScore() != null ? savedApplication.getMatchScore() : 0.0;
                savedApplication.setStatus(Application.ApplicationStatus.REJECTED);
                savedApplication = applicationRepository.save(savedApplication);
                log.info("Candidate {} REJECTED by AI score (Match: {}%).",
                        candidate.getId(), score);
                notificationService.notifyRejectedByAIScore(
                        candidate.getEmail(),
                        candidate.getFirstName() + " " + candidate.getLastName(),
                        job.getTitle(),
                        score
                );
            } catch (Exception e) {
                log.error("Failed to set REJECTED status for application {}", savedApplication.getId(), e);
            }
        }

        return savedApplication;
    }
    
    @Transactional
    public Application updateApplicationStatus(Long id, UpdateStatusRequest request) {
        Application application = getApplicationById(id);

        application.setStatus(request.getStatus());

        if (request.getExamScore() != null) {
            application.setExamScore(request.getExamScore());
        }
        if (request.getInterviewDate() != null) {
            application.setInterviewDate(request.getInterviewDate());
        }
        if (request.getInterviewResult() != null && !request.getInterviewResult().isBlank()) {
            application.setInterviewResult(request.getInterviewResult());
        }

        Application saved = applicationRepository.save(application);

        // Send notification email
        notificationService.notifyStatusUpdate(saved);

        return saved;
    }

    private String saveResumeFile(MultipartFile file, Long userId) {
        try {
            java.nio.file.Path uploadPath = java.nio.file.Paths.get("uploads", "resumes");
            java.nio.file.Files.createDirectories(uploadPath);
            String originalFilename = file.getOriginalFilename();
            String extension = (originalFilename != null && originalFilename.contains("."))
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String filename = userId + "_" + java.util.UUID.randomUUID() + extension;
            java.nio.file.Path filePath = uploadPath.resolve(filename);
            java.nio.file.Files.copy(file.getInputStream(), filePath,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to save resume file: " + e.getMessage());
        }
    }

    private Set<String> extractSkills(String skillsText) {
        if (skillsText == null || skillsText.trim().isEmpty()) {
            return Set.of();
        }
        return Arrays.stream(skillsText.split("[,;\\n]"))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}
