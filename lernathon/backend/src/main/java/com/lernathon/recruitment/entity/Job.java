package com.lernathon.recruitment.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    private String department;

    private String location;

    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    private String salaryRange;

    private Integer experienceRequired;

    @Column(columnDefinition = "TEXT")
    private String skillsRequired;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    private Integer openings;

    // Application deadline and dates
    @Column(name = "last_date")
    private LocalDate lastDate;

    @Column(name = "application_start_date")
    private LocalDateTime applicationStartDate;

    @Column(name = "application_end_date")
    private LocalDateTime applicationEndDate;

    // Exam and Interview Configuration
    @Column(name = "exam_pass_rate")
    private Double examPassRate; // Minimum pass rate for exam (0-100)

    @Column(name = "interview_pass_rate")
    private Double interviewPassRate; // Minimum pass rate for interview (0-100)

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_question_mode")
    private InterviewQuestionMode interviewQuestionMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hiring_manager_id")
    @JsonIgnoreProperties({"password", "applications", "interviews"})
    private User hiringManager;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<Application> applications = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime closedAt;

    public enum EmploymentType {
        FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP
    }

    public enum JobStatus {
        DRAFT, OPEN, CLOSED, ON_HOLD
    }

    public enum InterviewQuestionMode {
        BASIC, INTERMEDIATE, ADVANCED
    }
}
