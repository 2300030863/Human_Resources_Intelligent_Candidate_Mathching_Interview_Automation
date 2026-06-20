package com.lernathon.recruitment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "exam_answers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_attempt_id", nullable = false)
    private ExamAttempt examAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(columnDefinition = "TEXT")
    private String candidateAnswer;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isCorrect = false;

    @Column
    @Builder.Default
    private Integer pointsEarned = 0;

    @Column
    private Integer timeTaken; // in seconds

    @Column(columnDefinition = "TEXT")
    private String feedback; // For coding questions: stores test case results like "8/10 tests passed (80%)"

    @Column
    @Builder.Default
    private Integer attemptNumber = 1;

    @Column(updatable = false)
    private LocalDateTime answeredAt;

    @PrePersist
    protected void onCreate() {
        answeredAt = LocalDateTime.now();
    }
}
