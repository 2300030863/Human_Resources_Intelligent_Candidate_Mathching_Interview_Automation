package com.lernathon.recruitment.dto;

import com.lernathon.recruitment.entity.Interview;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewDTO {
    private Long id;
    private Long applicationId;
    private ApplicationSummaryDTO application;
    private UserSummaryDTO interviewer;
    private Interview.InterviewType type;
    private Interview.InterviewStatus status;
    private LocalDateTime scheduledAt;
    private Integer durationMinutes;
    private String location;
    private String meetingLink;
    private String feedback;
    private Integer rating;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationSummaryDTO {
        private Long id;
        private CandidateSummaryDTO candidate;
        private JobSummaryDTO job;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CandidateSummaryDTO {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobSummaryDTO {
        private Long id;
        private String title;
        private String department;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummaryDTO {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
    }

    // Factory method to convert Interview entity to DTO
    public static InterviewDTO fromEntity(Interview interview) {
        InterviewDTO dto = new InterviewDTO();
        dto.setId(interview.getId());
        dto.setType(interview.getType());
        dto.setStatus(interview.getStatus());
        dto.setScheduledAt(interview.getScheduledAt());
        dto.setDurationMinutes(interview.getDurationMinutes());
        dto.setLocation(interview.getLocation());
        dto.setMeetingLink(interview.getMeetingLink());
        dto.setFeedback(interview.getFeedback());
        dto.setRating(interview.getRating());
        dto.setNotes(interview.getNotes());
        dto.setCreatedAt(interview.getCreatedAt());
        dto.setUpdatedAt(interview.getUpdatedAt());

        if (interview.getApplication() != null) {
            var app = interview.getApplication();
            dto.setApplicationId(app.getId());
            
            ApplicationSummaryDTO appDto = new ApplicationSummaryDTO();
            appDto.setId(app.getId());
            appDto.setStatus(app.getStatus() != null ? app.getStatus().name() : null);
            
            if (app.getCandidate() != null) {
                var candidate = app.getCandidate();
                CandidateSummaryDTO candidateDto = new CandidateSummaryDTO();
                candidateDto.setId(candidate.getId());
                candidateDto.setFirstName(candidate.getFirstName());
                candidateDto.setLastName(candidate.getLastName());
                candidateDto.setEmail(candidate.getEmail());
                appDto.setCandidate(candidateDto);
            }
            
            if (app.getJob() != null) {
                var job = app.getJob();
                JobSummaryDTO jobDto = new JobSummaryDTO();
                jobDto.setId(job.getId());
                jobDto.setTitle(job.getTitle());
                jobDto.setDepartment(job.getDepartment());
                appDto.setJob(jobDto);
            }
            
            dto.setApplication(appDto);
        }

        if (interview.getInterviewer() != null) {
            var interviewer = interview.getInterviewer();
            UserSummaryDTO interviewerDto = new UserSummaryDTO();
            interviewerDto.setId(interviewer.getId());
            interviewerDto.setEmail(interviewer.getEmail());
            interviewerDto.setFirstName(interviewer.getFirstName());
            interviewerDto.setLastName(interviewer.getLastName());
            dto.setInterviewer(interviewerDto);
        }

        return dto;
    }
}
