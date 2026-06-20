package com.lernathon.recruitment.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "interviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    @JsonIgnoreProperties({"interviews", "coverLetter", "notes"})
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewer_id")
    @JsonIgnoreProperties({"password", "applications", "interviews"})
    private User interviewer;

    @Enumerated(EnumType.STRING)
    private InterviewType type;

    @Enumerated(EnumType.STRING)
    private InterviewStatus status;

    private LocalDateTime scheduledAt;

    private Integer durationMinutes;

    private String location;

    private String meetingLink;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum InterviewType {
        PHONE_SCREEN, VIDEO, TECHNICAL, BEHAVIORAL, PANEL, FINAL
    }

    public enum InterviewStatus {
        SCHEDULED, COMPLETED, CANCELLED, NO_SHOW, RESCHEDULED
    }
}
