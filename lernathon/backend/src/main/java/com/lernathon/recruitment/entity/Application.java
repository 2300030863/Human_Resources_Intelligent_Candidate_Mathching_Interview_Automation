package com.lernathon.recruitment.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "applications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("candidateId")
    @Column(name = "candidate_id", insertable = false, updatable = false)
    private Long candidateId;

    @JsonProperty("jobId")
    @Column(name = "job_id", insertable = false, updatable = false)
    private Long jobId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    @JsonIgnoreProperties({"applications", "interviews"})
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    @JsonIgnoreProperties({"applications", "hiringManager"})
    private Job job;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    private Double matchScore;

    // Exam tracking
    private Integer examScore;

    // Interview tracking
    private java.time.LocalDateTime interviewDate;
    private String interviewResult;

    @Column(columnDefinition = "TEXT")
    private String coverLetter;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<Interview> interviews = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum ApplicationStatus {
        SUBMITTED, UNDER_REVIEW, SCREENING, INTERVIEWING, OFFERED, ACCEPTED, REJECTED, WITHDRAWN,
        // Recruitment pipeline statuses
        EXAM_ELIGIBLE, EXAM_PASSED, EXAM_FAILED, INTERVIEW_SCHEDULED, SELECTED
    }
}
