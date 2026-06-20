package com.lernathon.recruitment.service;

import com.lernathon.recruitment.dto.InterviewDTO;
import com.lernathon.recruitment.dto.InterviewResultDTO;
import com.lernathon.recruitment.entity.Application;
import com.lernathon.recruitment.entity.Candidate;
import com.lernathon.recruitment.entity.Interview;
import com.lernathon.recruitment.repository.InterviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplicationService applicationService;
    private final CandidateService candidateService;

    public List<Interview> getAllInterviews() {
        return interviewRepository.findAll();
    }

    public Interview getInterviewById(Long id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Interview not found with id: " + id));
    }

    @Transactional
    public Interview createInterview(Long applicationId, Interview interview) {
        Application application = applicationService.getApplicationById(applicationId);
        
        interview.setApplication(application);
        
        if (interview.getStatus() == null) {
            interview.setStatus(Interview.InterviewStatus.SCHEDULED);
        }
        
        return interviewRepository.save(interview);
    }

    @Transactional
    public Interview updateInterview(Long id, Interview interviewDetails) {
        Interview interview = getInterviewById(id);
        
        interview.setType(interviewDetails.getType());
        interview.setStatus(interviewDetails.getStatus());
        interview.setScheduledAt(interviewDetails.getScheduledAt());
        interview.setDurationMinutes(interviewDetails.getDurationMinutes());
        interview.setLocation(interviewDetails.getLocation());
        interview.setMeetingLink(interviewDetails.getMeetingLink());
        interview.setFeedback(interviewDetails.getFeedback());
        interview.setRating(interviewDetails.getRating());
        interview.setNotes(interviewDetails.getNotes());
        
        Interview saved = interviewRepository.save(interview);

        // Auto-update application and candidate status when interview is COMPLETED
        if (interviewDetails.getStatus() == Interview.InterviewStatus.COMPLETED) {
            Application application = interview.getApplication();
            if (application != null) {
                Integer rating = interviewDetails.getRating();
                boolean passed = rating != null && rating >= 7;
                // Also check notes/feedback for PASSED keyword
                String feedback = interviewDetails.getFeedback() != null ? interviewDetails.getFeedback().toUpperCase() : "";
                String notes = interviewDetails.getNotes() != null ? interviewDetails.getNotes().toUpperCase() : "";
                if (feedback.contains("PASSED") || notes.contains("PASSED")) passed = true;
                if (feedback.contains("FAILED") || notes.contains("FAILED")) passed = false;

                if (passed) {
                    application.setStatus(Application.ApplicationStatus.OFFERED);
                    application.setNotes("Passed " + interview.getType() + " interview - ready for offer");
                    if (application.getCandidate() != null) {
                        Candidate candidate = application.getCandidate();
                        candidate.setStatus(Candidate.CandidateStatus.OFFERED);
                        candidateService.updateCandidate(candidate.getId(), candidate);
                    }
                    log.info("[updateInterview] Application {} -> OFFERED after completing interview {}", application.getId(), id);
                } else if (rating != null && rating < 7) {
                    application.setStatus(Application.ApplicationStatus.REJECTED);
                    application.setNotes("Did not pass " + interview.getType() + " interview");
                    if (application.getCandidate() != null) {
                        Candidate candidate = application.getCandidate();
                        candidate.setStatus(Candidate.CandidateStatus.REJECTED);
                        candidateService.updateCandidate(candidate.getId(), candidate);
                    }
                    log.info("[updateInterview] Application {} -> REJECTED after failing interview {}", application.getId(), id);
                }
                applicationService.updateApplication(application.getId(), application);
            }
        }

        return saved;
    }

    @Transactional
    public void deleteInterview(Long id) {
        Interview interview = getInterviewById(id);
        interviewRepository.delete(interview);
    }

    public List<Interview> getInterviewsByApplication(Long applicationId) {
        return interviewRepository.findByApplication_Id(applicationId);
    }

    public List<Interview> getInterviewsByCandidate(Long candidateId) {
        return interviewRepository.findByApplication_CandidateId(candidateId);
    }

    public List<Interview> getInterviewsByInterviewer(Long interviewerId) {
        return interviewRepository.findByInterviewerId(interviewerId);
    }

    public List<Interview> getInterviewsByDateRange(LocalDateTime start, LocalDateTime end) {
        return interviewRepository.findByScheduledAtBetween(start, end);
    }

    @Transactional
    public Interview submitInterviewResult(Long interviewId, InterviewResultDTO resultDTO) {
        Interview interview = getInterviewById(interviewId);
        Application application = interview.getApplication();
        
        // Update interview with results
        interview.setStatus(resultDTO.getStatus() != null ? resultDTO.getStatus() : Interview.InterviewStatus.COMPLETED);
        interview.setFeedback(resultDTO.getFeedback());
        interview.setRating(resultDTO.getRating());
        
        // Combine notes with strengths/weaknesses/recommendation
        StringBuilder notes = new StringBuilder();
        if (resultDTO.getNotes() != null) {
            notes.append(resultDTO.getNotes()).append("\n\n");
        }
        if (resultDTO.getStrengths() != null) {
            notes.append("Strengths: ").append(resultDTO.getStrengths()).append("\n");
        }
        if (resultDTO.getWeaknesses() != null) {
            notes.append("Weaknesses: ").append(resultDTO.getWeaknesses()).append("\n");
        }
        if (resultDTO.getRecommendation() != null) {
            notes.append("Recommendation: ").append(resultDTO.getRecommendation());
        }
        interview.setNotes(notes.toString().trim());
        
        Interview savedInterview = interviewRepository.save(interview);
        
        // Update application status based on interview result
        if (resultDTO.getPassed() != null && resultDTO.getPassed()) {
            // Any passed interview moves the application to OFFERED
            application.setStatus(Application.ApplicationStatus.OFFERED);
            application.setNotes(String.format("Passed %s interview - ready for offer", interview.getType()));
            // Also update the candidate's own status
            if (application.getCandidate() != null) {
                Candidate candidate = application.getCandidate();
                candidate.setStatus(Candidate.CandidateStatus.OFFERED);
                candidateService.updateCandidate(candidate.getId(), candidate);
            }
            log.info("Application {} moved to OFFERED status after passing {} interview {}", 
                application.getId(), interview.getType(), interview.getId());
        } else if (resultDTO.getPassed() != null && !resultDTO.getPassed()) {
            // Failed interview - reject
            application.setStatus(Application.ApplicationStatus.REJECTED);
            application.setNotes(String.format("Did not pass %s interview", interview.getType()));
            // Also update the candidate's own status
            if (application.getCandidate() != null) {
                Candidate candidate = application.getCandidate();
                candidate.setStatus(Candidate.CandidateStatus.REJECTED);
                candidateService.updateCandidate(candidate.getId(), candidate);
            }
            log.info("Application {} rejected after failing {} interview {}", 
                application.getId(), interview.getType(), interview.getId());
        }
        
        applicationService.updateApplication(application.getId(), application);
        
        log.info("Interview result submitted for interview {} - Status: {}, Rating: {}", 
            interviewId, resultDTO.getStatus(), resultDTO.getRating());
        
        return savedInterview;
    }

    // DTO conversion methods
    public List<InterviewDTO> getAllInterviewsAsDTO() {
        return interviewRepository.findAll().stream()
                .map(InterviewDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public InterviewDTO getInterviewByIdAsDTO(Long id) {
        Interview interview = getInterviewById(id);
        return InterviewDTO.fromEntity(interview);
    }

    public List<InterviewDTO> getInterviewsByApplicationAsDTO(Long applicationId) {
        return interviewRepository.findByApplication_Id(applicationId).stream()
                .map(InterviewDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<InterviewDTO> getInterviewsByCandidateAsDTO(Long candidateId) {
        return interviewRepository.findByApplication_CandidateId(candidateId).stream()
                .map(InterviewDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<InterviewDTO> getInterviewsByInterviewerAsDTO(Long interviewerId) {
        return interviewRepository.findByInterviewerId(interviewerId).stream()
                .map(InterviewDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<InterviewDTO> getInterviewsByDateRangeAsDTO(LocalDateTime start, LocalDateTime end) {
        return interviewRepository.findByScheduledAtBetween(start, end).stream()
                .map(InterviewDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
