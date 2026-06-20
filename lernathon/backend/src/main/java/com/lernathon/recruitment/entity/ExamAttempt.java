package com.lernathon.recruitment.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "exam_attempts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private Application application;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ExamStatus status = ExamStatus.NOT_STARTED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DifficultyLevel currentDifficulty;

    @Column(nullable = false)
    @Builder.Default
    private Integer cheatingScore = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer consecutiveCorrect = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer consecutiveWrong = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer tabSwitchCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer fullscreenExitCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer copyPasteAttempts = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer windowBlurCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean autoSubmitted = false;

    @Column
    @Builder.Default
    private Double finalScore = 0.0;

    @Column
    @Builder.Default
    private Integer totalQuestions = 0;

    @Column
    @Builder.Default
    private Integer answeredQuestions = 0;

    @Column
    @Builder.Default
    private Integer correctAnswers = 0;

    @Column
    private Boolean qualifiedForInterview;

    @Column
    private String sessionToken;

    @Column
    private String ipAddress;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    @Column
    private LocalDateTime disqualifiedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @JsonProperty("candidateId")
    public Long getCandidateId() {
        return candidate != null ? candidate.getId() : null;
    }

    @JsonProperty("jobId")
    public Long getJobId() {
        return job != null ? job.getId() : null;
    }

    public enum ExamStatus {
        NOT_STARTED, IN_PROGRESS, COMPLETED, DISQUALIFIED, EXPIRED
    }

    public enum DifficultyLevel {
        EASY, MEDIUM, HARD, ADVANCED
    }
}
