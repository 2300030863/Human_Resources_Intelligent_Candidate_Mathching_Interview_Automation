package com.lernathon.recruitment.controller;

import com.lernathon.recruitment.dto.AIResumeScreeningResponse;
import com.lernathon.recruitment.dto.CandidateDashboardResponse;
import com.lernathon.recruitment.dto.ResumeUploadResponse;
import com.lernathon.recruitment.entity.Application;
import com.lernathon.recruitment.entity.Candidate;
import com.lernathon.recruitment.repository.ApplicationRepository;
import com.lernathon.recruitment.service.CandidateService;
import com.lernathon.recruitment.service.ResumeParserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/candidates")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService candidateService;
    private final ResumeParserService resumeParserService;
    private final ApplicationRepository applicationRepository;

    @GetMapping
    public ResponseEntity<List<Candidate>> getAllCandidates() {
        return ResponseEntity.ok(candidateService.getAllCandidates());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Candidate> getCandidateById(@PathVariable Long id) {
        return ResponseEntity.ok(candidateService.getCandidateById(id));
    }

    @PostMapping
    public ResponseEntity<Candidate> createCandidate(@RequestBody Candidate candidate) {
        return ResponseEntity.ok(candidateService.createCandidate(candidate));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Candidate> updateCandidate(
            @PathVariable Long id,
            @RequestBody Candidate candidate
    ) {
        return ResponseEntity.ok(candidateService.updateCandidate(id, candidate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCandidate(@PathVariable Long id) {
        candidateService.deleteCandidate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<Candidate>> searchCandidates(@RequestParam String keyword) {
        return ResponseEntity.ok(candidateService.searchCandidates(keyword));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Candidate>> getCandidatesByStatus(
            @PathVariable Candidate.CandidateStatus status
    ) {
        return ResponseEntity.ok(candidateService.getCandidatesByStatus(status));
    }

    @PostMapping("/upload-resume")
    public ResponseEntity<ResumeUploadResponse> uploadResume(@RequestParam("file") MultipartFile file) {
        try {
            ResumeParserService.ParsedResume parsed = resumeParserService.parseResume(file);
            
            ResumeUploadResponse response = ResumeUploadResponse.builder()
                    .name(parsed.getName())
                    .email(parsed.getEmail())
                    .phone(parsed.getPhone())
                    .experienceYears(parsed.getExperienceYears())
                    .education(parsed.getEducation())
                    .skills(parsed.getSkills())
                    .message("Resume parsed successfully")
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/parse-text")
    public ResponseEntity<ResumeUploadResponse> parseResumeText(@RequestBody String content) {
        ResumeParserService.ParsedResume parsed = resumeParserService.parseContent(content);
        
        ResumeUploadResponse response = ResumeUploadResponse.builder()
                .name(parsed.getName())
                .email(parsed.getEmail())
                .phone(parsed.getPhone())
                .experienceYears(parsed.getExperienceYears())
                .education(parsed.getEducation())
                .skills(parsed.getSkills())
                .message("Resume parsed successfully")
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/ai-screening")
    public ResponseEntity<AIResumeScreeningResponse> aiResumeScreening(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            // Get authenticated user's email
            String userEmail = authentication.getName();
            
            // Parse resume using AI service
            ResumeParserService.ParsedResume parsed = resumeParserService.parseResume(file);
            
            // Extract name parts from resume
            String fullName = parsed.getName() != null ? parsed.getName() : "";
            String[] nameParts = fullName.split("\\s+", 2);
            String firstName = nameParts.length > 0 ? nameParts[0] : "";
            String lastName = nameParts.length > 1 ? nameParts[1] : "";
            
            // Check if candidate already exists using authenticated user's email (not resume email)
            Candidate existingCandidate = candidateService.getCandidateByEmail(userEmail);
            boolean isNew = (existingCandidate == null);
            
            // Determine which values to use (existing candidate values or resume values)
            String finalFirstName = (existingCandidate != null && existingCandidate.getFirstName() != null && !existingCandidate.getFirstName().isEmpty()) 
                ? existingCandidate.getFirstName() : firstName;
            String finalLastName = (existingCandidate != null && existingCandidate.getLastName() != null && !existingCandidate.getLastName().isEmpty()) 
                ? existingCandidate.getLastName() : lastName;
            String finalPhone = (existingCandidate != null && existingCandidate.getPhone() != null && !existingCandidate.getPhone().isEmpty()) 
                ? existingCandidate.getPhone() : parsed.getPhone();
            Integer finalExperience = (parsed.getExperienceYears() != null && parsed.getExperienceYears() > 0) 
                ? parsed.getExperienceYears() : (existingCandidate != null ? existingCandidate.getExperienceYears() : 0);
            String finalEducation = (parsed.getEducation() != null && !parsed.getEducation().isEmpty()) 
                ? parsed.getEducation() : (existingCandidate != null ? existingCandidate.getEducation() : "");
            
            // Create or update candidate using authenticated user's email
            Candidate candidate = candidateService.createOrUpdateFromResume(
                finalFirstName,
                finalLastName,
                userEmail,  // Use authenticated user's email, not resume email
                finalPhone,
                parsed.getSkills(),
                finalExperience,
                finalEducation,
                parsed.getRawContent()
            );
            
            // Build response with parsed data for auto-fill
            AIResumeScreeningResponse response = AIResumeScreeningResponse.builder()
                    .candidateId(candidate.getId())
                    .name(firstName + " " + lastName) // Return parsed name for potential auto-fill
                    .email(userEmail)  // Return authenticated user's email
                    .phone(parsed.getPhone())  // Return parsed phone for potential auto-fill
                    .experienceYears(parsed.getExperienceYears())
                    .education(parsed.getEducation())
                    .skills(parsed.getSkills())
                    .status(candidate.getStatus().name())
                    .isNewCandidate(isNew)
                    .currentCompany(parsed.getCurrentCompany())
                    .message(isNew ? 
                        "New candidate profile created successfully with AI-detected skills" : 
                        "Candidate profile updated successfully with AI-detected skills")
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/dashboard/{candidateId}")
    public ResponseEntity<CandidateDashboardResponse> getDashboard(@PathVariable Long candidateId) {
        List<Application> allApps = applicationRepository.findByCandidateId(candidateId);

        // Recent applications (sorted by appliedAt, newest first, max 10)
        List<CandidateDashboardResponse.ApplicationSummary> recentApplications = allApps.stream()
                .sorted((a, b) -> {
                    if (a.getAppliedAt() == null) return 1;
                    if (b.getAppliedAt() == null) return -1;
                    return b.getAppliedAt().compareTo(a.getAppliedAt());
                })
                .limit(10)
                .map(app -> CandidateDashboardResponse.ApplicationSummary.builder()
                        .applicationId(app.getId())
                        .jobTitle(app.getJob() != null ? app.getJob().getTitle() : "")
                        .company(app.getJob() != null ? app.getJob().getDepartment() : "")
                        .status(app.getStatus() != null ? app.getStatus().name() : "")
                        .appliedAt(app.getAppliedAt())
                        .matchScore(app.getMatchScore())
                        .build())
                .collect(Collectors.toList());

        // Exam results (EXAM_PASSED or EXAM_FAILED)
        List<CandidateDashboardResponse.ExamResultSummary> examResults = allApps.stream()
                .filter(app -> app.getStatus() == Application.ApplicationStatus.EXAM_PASSED
                        || app.getStatus() == Application.ApplicationStatus.EXAM_FAILED)
                .map(app -> CandidateDashboardResponse.ExamResultSummary.builder()
                        .applicationId(app.getId())
                        .jobTitle(app.getJob() != null ? app.getJob().getTitle() : "")
                        .examScore(app.getExamScore())
                        .result(app.getStatus() == Application.ApplicationStatus.EXAM_PASSED ? "PASSED" : "FAILED")
                        .updatedAt(app.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        // Upcoming interviews (INTERVIEW_SCHEDULED with future date)
        LocalDateTime now = LocalDateTime.now();
        List<CandidateDashboardResponse.InterviewSummary> upcomingInterviews = allApps.stream()
                .filter(app -> app.getStatus() == Application.ApplicationStatus.INTERVIEW_SCHEDULED
                        && app.getInterviewDate() != null
                        && app.getInterviewDate().isAfter(now))
                .sorted((a, b) -> a.getInterviewDate().compareTo(b.getInterviewDate()))
                .map(app -> CandidateDashboardResponse.InterviewSummary.builder()
                        .applicationId(app.getId())
                        .jobTitle(app.getJob() != null ? app.getJob().getTitle() : "")
                        .interviewDate(app.getInterviewDate())
                        .status(app.getStatus().name())
                        .interviewResult(app.getInterviewResult())
                        .build())
                .collect(Collectors.toList());

        CandidateDashboardResponse response = CandidateDashboardResponse.builder()
                .recentApplications(recentApplications)
                .examResults(examResults)
                .upcomingInterviews(upcomingInterviews)
                .build();

        return ResponseEntity.ok(response);
    }
}
