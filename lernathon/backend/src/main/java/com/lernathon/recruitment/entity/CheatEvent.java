package com.lernathon.recruitment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "cheat_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheatEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_attempt_id", nullable = false)
    private ExamAttempt examAttempt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheatType type;

    @Column(nullable = false)
    private Integer penaltyScore;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private String metadata; // JSON for additional data

    @Column(updatable = false)
    private LocalDateTime detectedAt;

    @PrePersist
    protected void onCreate() {
        detectedAt = LocalDateTime.now();
    }

    public enum CheatType {
        TAB_SWITCH(1, "Switched to another tab"),
        FULLSCREEN_EXIT(1, "Exited fullscreen mode"),
        COPY_ATTEMPT(1, "Attempted to copy content"),
        PASTE_ATTEMPT(1, "Attempted to paste content"),
        WINDOW_BLUR(1, "Minimized or switched window"),
        MULTIPLE_FACES(3, "Multiple faces detected"),
        NO_FACE_DETECTED(2, "No face detected in webcam"),
        RIGHT_CLICK(1, "Right-click attempted"),
        KEYBOARD_SHORTCUT(1, "Suspicious keyboard shortcut");

        private final int penalty;
        private final String description;

        CheatType(int penalty, String description) {
            this.penalty = penalty;
            this.description = description;
        }

        public int getPenalty() {
            return penalty;
        }

        public String getDescription() {
            return description;
        }
    }
}
