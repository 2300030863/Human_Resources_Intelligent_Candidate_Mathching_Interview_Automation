package com.lernathon.recruitment.service;

import com.lernathon.recruitment.dto.CheatEventRequest;
import com.lernathon.recruitment.dto.CheatEventResponse;
import com.lernathon.recruitment.entity.CheatEvent;
import com.lernathon.recruitment.entity.ExamAttempt;
import com.lernathon.recruitment.repository.CheatEventRepository;
import com.lernathon.recruitment.repository.ExamAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class CheatDetectionService {

    private final CheatEventRepository cheatEventRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamSubmissionService examSubmissionService;

    private static final int CHEATING_THRESHOLD = 3;

    @Transactional
    public CheatEventResponse recordCheatEvent(CheatEventRequest request) {
        log.info("Recording cheat event - ExamAttemptId: {}, CheatType: {}, SessionToken: {}", 
                request.getExamAttemptId(), request.getCheatType(), 
                request.getSessionToken() != null ? "present" : "null");

        // Validate input
        if (request.getSessionToken() == null || request.getSessionToken().isEmpty()) {
            log.error("Session token is null or empty");
            throw new RuntimeException("Session token is required");
        }

        if (request.getExamAttemptId() == null) {
            log.error("Exam attempt ID is null");
            throw new RuntimeException("Exam attempt ID is required");
        }

        if (request.getCheatType() == null || request.getCheatType().isEmpty()) {
            log.error("Cheat type is null or empty");
            throw new RuntimeException("Cheat type is required");
        }

        // Validate session token
        ExamAttempt exam = examAttemptRepository.findBySessionToken(request.getSessionToken())
                .orElseThrow(() -> {
                    log.error("Invalid session token: {}", request.getSessionToken());
                    return new RuntimeException("Invalid session token: " + request.getSessionToken());
                });

        log.info("Found exam: ID={}, Status={}, CandidateId={}", 
                exam.getId(), exam.getStatus(), exam.getCandidate().getId());

        if (!exam.getId().equals(request.getExamAttemptId())) {
            log.error("Session token mismatch - Expected ExamId: {}, Got: {}", 
                    exam.getId(), request.getExamAttemptId());
            throw new RuntimeException("Session token mismatch - exam ID does not match");
        }

        if (exam.getStatus() != ExamAttempt.ExamStatus.IN_PROGRESS) {
            log.error("Exam is not in progress - Status: {}", exam.getStatus());
            throw new RuntimeException("Exam is not in progress. Current status: " + exam.getStatus());
        }

        // Parse cheat type
        CheatEvent.CheatType cheatType;
        try {
            cheatType = CheatEvent.CheatType.valueOf(request.getCheatType());
        } catch (IllegalArgumentException e) {
            log.error("Invalid cheat type: {}", request.getCheatType());
            throw new RuntimeException("Invalid cheat type: " + request.getCheatType());
        }

        // Record cheat event
        CheatEvent cheatEvent = CheatEvent.builder()
                .examAttempt(exam)
                .type(cheatType)
                .penaltyScore(cheatType.getPenalty())
                .description(cheatType.getDescription())
                .metadata(request.getMetadata())
                .build();
        
        cheatEventRepository.save(cheatEvent);

        // Update exam attempt counters
        updateExamCounters(exam, cheatType);

        // Add penalty to cheating score
        int previousScore = exam.getCheatingScore();
        int newScore = previousScore + cheatType.getPenalty();
        exam.setCheatingScore(newScore);
        examAttemptRepository.save(exam);

        log.warn("Cheat event detected - Exam: {}, Type: {}, Score: {} -> {}", 
                exam.getId(), cheatType, previousScore, newScore);

        // Check if threshold breached
        boolean examTerminated = false;
        String message;

        if (newScore >= CHEATING_THRESHOLD) {
            // Auto-terminate exam
            examTerminated = true;
            exam.setStatus(ExamAttempt.ExamStatus.DISQUALIFIED);
            exam.setDisqualifiedAt(LocalDateTime.now());
            exam.setAutoSubmitted(true);
            examAttemptRepository.save(exam);

            // Auto-submit exam
            examSubmissionService.autoSubmitExam(exam.getId());

            message = String.format(
                    "EXAM TERMINATED: Cheating threshold breached (%d/%d). Your exam has been automatically submitted and you are disqualified.",
                    newScore, CHEATING_THRESHOLD
            );

            log.error("Exam {} terminated due to cheating. Score: {}", exam.getId(), newScore);
        } else {
            int warningsRemaining = CHEATING_THRESHOLD - newScore;
            message = String.format(
                    "WARNING: %s detected. Cheating score: %d/%d. %d warning%s remaining before automatic termination.",
                    cheatType.getDescription(),
                    newScore,
                    CHEATING_THRESHOLD,
                    warningsRemaining,
                    warningsRemaining == 1 ? "" : "s"
            );
        }

        return CheatEventResponse.builder()
                .currentCheatingScore(newScore)
                .warningsRemaining(Math.max(0, CHEATING_THRESHOLD - newScore))
                .examTerminated(examTerminated)
                .message(message)
                .build();
    }

    private void updateExamCounters(ExamAttempt exam, CheatEvent.CheatType cheatType) {
        switch (cheatType) {
            case TAB_SWITCH:
                exam.setTabSwitchCount(exam.getTabSwitchCount() + 1);
                break;
            case FULLSCREEN_EXIT:
                exam.setFullscreenExitCount(exam.getFullscreenExitCount() + 1);
                break;
            case COPY_ATTEMPT:
            case PASTE_ATTEMPT:
                exam.setCopyPasteAttempts(exam.getCopyPasteAttempts() + 1);
                break;
            case WINDOW_BLUR:
                exam.setWindowBlurCount(exam.getWindowBlurCount() + 1);
                break;
            case NO_FACE_DETECTED:
            case MULTIPLE_FACES:
            case RIGHT_CLICK:
            case KEYBOARD_SHORTCUT:
                // These don't have specific counters, penalty already added to cheating score
                break;
        }
    }

    public int getCurrentCheatingScore(Long examAttemptId) {
        ExamAttempt exam = examAttemptRepository.findById(examAttemptId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));
        return exam.getCheatingScore();
    }

    public boolean isExamDisqualified(Long examAttemptId) {
        ExamAttempt exam = examAttemptRepository.findById(examAttemptId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));
        return exam.getStatus() == ExamAttempt.ExamStatus.DISQUALIFIED;
    }
}
