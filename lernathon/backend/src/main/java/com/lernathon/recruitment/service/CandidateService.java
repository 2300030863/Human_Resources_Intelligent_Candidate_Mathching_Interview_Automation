package com.lernathon.recruitment.service;

import com.lernathon.recruitment.entity.Candidate;
import com.lernathon.recruitment.repository.CandidateRepository;
import com.lernathon.recruitment.repository.ExamAttemptRepository;
import com.lernathon.recruitment.repository.ExamAnswerRepository;
import com.lernathon.recruitment.repository.CheatEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final CheatEventRepository cheatEventRepository;

    public List<Candidate> getAllCandidates() {
        return candidateRepository.findAll();
    }

    public Candidate getCandidateById(Long id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidate not found with id: " + id));
    }

    public Candidate getCandidateByEmail(String email) {
        return candidateRepository.findByEmail(email).orElse(null);
    }

    @Transactional
    public Candidate createCandidate(Candidate candidate) {
        if (candidate.getStatus() == null) {
            candidate.setStatus(Candidate.CandidateStatus.NEW);
        }
        return candidateRepository.save(candidate);
    }

    @Transactional
    public Candidate updateCandidate(Long id, Candidate candidateDetails) {
        Candidate candidate = getCandidateById(id);
        
        candidate.setFirstName(candidateDetails.getFirstName());
        candidate.setLastName(candidateDetails.getLastName());
        candidate.setEmail(candidateDetails.getEmail());
        candidate.setPhone(candidateDetails.getPhone());
        candidate.setResume(candidateDetails.getResume());
        candidate.setResumeUrl(candidateDetails.getResumeUrl());
        candidate.setSkills(candidateDetails.getSkills());
        candidate.setExperienceYears(candidateDetails.getExperienceYears());
        candidate.setEducation(candidateDetails.getEducation());
        candidate.setLocation(candidateDetails.getLocation());
        candidate.setCurrentCompany(candidateDetails.getCurrentCompany());
        candidate.setCurrentRole(candidateDetails.getCurrentRole());
        candidate.setStatus(candidateDetails.getStatus());
        candidate.setNotes(candidateDetails.getNotes());
        
        return candidateRepository.save(candidate);
    }

    @Transactional
    public void deleteCandidate(Long id) {
        Candidate candidate = getCandidateById(id);
        
        // Delete related exam data first to avoid foreign key constraints
        candidate.getApplications().forEach(application -> {
            // Delete cheat events for exam attempts
            examAttemptRepository.findByApplication_Id(application.getId()).forEach(examAttempt -> {
                cheatEventRepository.deleteByExamAttemptId(examAttempt.getId());
                examAnswerRepository.deleteByExamAttemptId(examAttempt.getId());
            });
            // Delete exam attempts
            examAttemptRepository.deleteByApplication_Id(application.getId());
        });
        
        // Delete exam attempts directly linked to candidate (not through application)
        examAttemptRepository.findByCandidate_Id(id).forEach(examAttempt -> {
            cheatEventRepository.deleteByExamAttemptId(examAttempt.getId());
            examAnswerRepository.deleteByExamAttemptId(examAttempt.getId());
        });
        examAttemptRepository.deleteByCandidate_Id(id);
        
        // Now delete the candidate (cascade will handle applications)
        candidateRepository.delete(candidate);
    }

    public List<Candidate> searchCandidates(String keyword) {
        return candidateRepository.searchCandidates(keyword);
    }

    public List<Candidate> getCandidatesByStatus(Candidate.CandidateStatus status) {
        return candidateRepository.findByStatus(status);
    }
    
    @Transactional
    public Candidate createOrUpdateFromResume(String firstName, String lastName, String email, 
                                              String phone, List<String> skills, Integer experienceYears, 
                                              String education, String resumeContent) {
        // Check if candidate already exists by email
        Candidate candidate = getCandidateByEmail(email);
        
        if (candidate == null) {
            // Create new candidate
            candidate = Candidate.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .phone(phone)
                    .skills(skills != null ? String.join(", ", skills) : "")
                    .experienceYears(experienceYears)
                    .education(education)
                    .resume(resumeContent)
                    .status(Candidate.CandidateStatus.NEW)
                    .build();
        } else {
            // Update existing candidate with new information
            if (firstName != null && !firstName.isEmpty()) candidate.setFirstName(firstName);
            if (lastName != null && !lastName.isEmpty()) candidate.setLastName(lastName);
            if (phone != null && !phone.isEmpty()) candidate.setPhone(phone);
            if (skills != null && !skills.isEmpty()) {
                candidate.setSkills(String.join(", ", skills));
            }
            if (experienceYears != null) candidate.setExperienceYears(experienceYears);
            if (education != null && !education.isEmpty()) candidate.setEducation(education);
            if (resumeContent != null && !resumeContent.isEmpty()) candidate.setResume(resumeContent);
            
            // Update status to SCREENING if it's NEW
            if (candidate.getStatus() == Candidate.CandidateStatus.NEW) {
                candidate.setStatus(Candidate.CandidateStatus.SCREENING);
            }
        }
        
        return candidateRepository.save(candidate);
    }
}
