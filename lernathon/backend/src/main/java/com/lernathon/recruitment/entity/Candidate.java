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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "candidates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    private String phone;

    @Column(columnDefinition = "TEXT")
    private String resume;

    private String resumeUrl;

    @Column(columnDefinition = "TEXT")
    private String skills;

    private Integer experienceYears;

    @Column(columnDefinition = "TEXT")
    private String education;

    private String location;

    private String currentCompany;

    private String currentRole;

    /** JSON string from Flask /upload-and-verify — stores per-company verification results */
    @Column(columnDefinition = "TEXT")
    private String employmentVerificationJson;

    @Enumerated(EnumType.STRING)
    private CandidateStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<Application> applications = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum CandidateStatus {
        NEW, SCREENING, INTERVIEWING, OFFERED, HIRED, REJECTED, WITHDRAWN
    }
}
