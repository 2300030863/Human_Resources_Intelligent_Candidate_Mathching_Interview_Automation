package com.lernathon.recruitment.service;

import com.lernathon.recruitment.dto.*;
import com.lernathon.recruitment.entity.*;
import com.lernathon.recruitment.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@SuppressWarnings("null")
public class ExamSubmissionService {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final QuestionRepository questionRepository;
    private final CheatEventRepository cheatEventRepository;
    private final CodeExecutionService codeExecutionService;
    private final ApplicationRepository applicationRepository;
    private final InterviewService interviewService;
    private final NotificationService notificationService;
    
    // Manual constructor for debugging
    public ExamSubmissionService(
            ExamAttemptRepository examAttemptRepository,
            ExamAnswerRepository examAnswerRepository,
            QuestionRepository questionRepository,
            CheatEventRepository cheatEventRepository,
            CodeExecutionService codeExecutionService,
            ApplicationRepository applicationRepository,
            InterviewService interviewService,
            NotificationService notificationService) {
        this.examAttemptRepository = examAttemptRepository;
        this.examAnswerRepository = examAnswerRepository;
        this.questionRepository = questionRepository;
        this.cheatEventRepository = cheatEventRepository;
        this.codeExecutionService = codeExecutionService;
        this.applicationRepository = applicationRepository;
        this.interviewService = interviewService;
        this.notificationService = notificationService;
    }

    private static final int CONSECUTIVE_FOR_UPGRADE = 3;
    private static final int CONSECUTIVE_FOR_DOWNGRADE = 2;

    // Inner class for evaluation results
    private static class EvaluationResult {
        boolean isCorrect;
        int pointsEarned;
        String feedback;

        EvaluationResult(boolean isCorrect, int pointsEarned, String feedback) {
            this.isCorrect = isCorrect;
            this.pointsEarned = pointsEarned;
            this.feedback = feedback;
        }
    }

    @Transactional
    public SubmitAnswerResponse submitAnswer(SubmitAnswerRequest request) {
        log.info("Submitting answer for exam {} question {}", request.getExamAttemptId(), request.getQuestionId());
        
        // Validate request
        if (request.getExamAttemptId() == null) {
            throw new IllegalArgumentException("Exam attempt ID is required");
        }
        if (request.getQuestionId() == null) {
            throw new IllegalArgumentException("Question ID is required");
        }
        if (request.getAnswer() == null || request.getAnswer().trim().isEmpty()) {
            throw new IllegalArgumentException("Answer is required");
        }
        if (request.getSessionToken() == null || request.getSessionToken().trim().isEmpty()) {
            throw new IllegalArgumentException("Session token is required");
        }
        
        // Validate session token
        ExamAttempt exam = examAttemptRepository.findBySessionToken(request.getSessionToken())
                .orElseThrow(() -> new RuntimeException("Invalid session token"));

        if (!exam.getId().equals(request.getExamAttemptId())) {
            throw new RuntimeException("Session token mismatch");
        }

        if (exam.getStatus() != ExamAttempt.ExamStatus.IN_PROGRESS) {
            throw new RuntimeException("Exam is not in progress. Current status: " + exam.getStatus());
        }

        // Fetch the question
        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Question not found: " + request.getQuestionId()));

        // Evaluate answer and calculate points
        EvaluationResult evalResult = evaluateAnswerWithDetails(question, request.getAnswer());
        boolean isCorrect = evalResult.isCorrect;
        int pointsEarned = evalResult.pointsEarned;

        // Update consecutive counters
        if (isCorrect) {
            exam.setConsecutiveCorrect(exam.getConsecutiveCorrect() + 1);
            exam.setConsecutiveWrong(0);
            exam.setCorrectAnswers(exam.getCorrectAnswers() + 1);
        } else {
            exam.setConsecutiveWrong(exam.getConsecutiveWrong() + 1);
            exam.setConsecutiveCorrect(0);
        }

        exam.setAnsweredQuestions(exam.getAnsweredQuestions() + 1);

        // Adaptive difficulty adjustment
        String difficultyChanged = null;

        if (exam.getConsecutiveCorrect() >= CONSECUTIVE_FOR_UPGRADE) {
            if (exam.getCurrentDifficulty() != ExamAttempt.DifficultyLevel.ADVANCED) {
                exam.setCurrentDifficulty(upgradeDifficulty(exam.getCurrentDifficulty()));
                exam.setConsecutiveCorrect(0);
                difficultyChanged = "INCREASED to " + exam.getCurrentDifficulty();
                log.info("Difficulty increased for exam {} to {}", exam.getId(), exam.getCurrentDifficulty());
            }
        } else if (exam.getConsecutiveWrong() >= CONSECUTIVE_FOR_DOWNGRADE) {
            if (exam.getCurrentDifficulty() != ExamAttempt.DifficultyLevel.EASY) {
                exam.setCurrentDifficulty(downgradeDifficulty(exam.getCurrentDifficulty()));
                exam.setConsecutiveWrong(0);
                difficultyChanged = "DECREASED to " + exam.getCurrentDifficulty();
                log.info("Difficulty decreased for exam {} to {}", exam.getId(), exam.getCurrentDifficulty());
            }
        }

        examAttemptRepository.save(exam);
        
        ExamAnswer answer = ExamAnswer.builder()
                .examAttempt(exam)
                .question(question)
                .candidateAnswer(request.getAnswer())
                .isCorrect(isCorrect)
                .pointsEarned(pointsEarned)
                .feedback(evalResult.feedback)
                .timeTaken(request.getTimeTaken())
                .build();
        
        examAnswerRepository.save(answer);
        
        log.info("Answer saved successfully for exam {} question {}. Correct: {}, Points: {}", 
                request.getExamAttemptId(), request.getQuestionId(), isCorrect, pointsEarned);

        // Don't reveal correctness during exam - only after completion
        // This ensures candidates don't know if they're right or wrong until the end
        return SubmitAnswerResponse.builder()
                .isCorrect(null) // Hidden during exam
                .pointsEarned(null) // Hidden during exam
                .explanation("Answer submitted successfully.") // Generic message
                .consecutiveCorrect(exam.getConsecutiveCorrect())
                .consecutiveWrong(exam.getConsecutiveWrong())
                .difficultyChanged(difficultyChanged)
                .nextQuestion(null)
                .build();
    }

    /**
     * Evaluates the candidate's answer with detailed results including points
     */
    private EvaluationResult evaluateAnswerWithDetails(Question question, String candidateAnswer) {
        int maxPoints = question.getPoints() != null ? question.getPoints() : 10;
        
        if (candidateAnswer == null || candidateAnswer.trim().isEmpty()) {
            return new EvaluationResult(false, 0, "No answer provided");
        }
        
        // For MCQ questions
        if (question.getType() == Question.QuestionType.MCQ) {
            boolean isCorrect = evaluateAnswer(question, candidateAnswer);
            int points = isCorrect ? maxPoints : 0;
            return new EvaluationResult(isCorrect, points, "");
        }
        
        // For CODING questions - execute test cases
        if (question.getType() == Question.QuestionType.CODING) {
            log.info("Evaluating CODING question {} with test cases", question.getId());
            
            if (question.getTestCases() == null || question.getTestCases().trim().isEmpty()) {
                log.warn("No test cases defined for coding question {}", question.getId());
                // Fallback: give partial credit for substantial answer
                boolean hasSubstantialCode = candidateAnswer.trim().length() > 50;
                int points = hasSubstantialCode ? (maxPoints / 2) : 0;
                return new EvaluationResult(hasSubstantialCode, points, 
                        "No test cases defined - partial credit given");
            }
            
            // Execute test cases
            CodeExecutionService.TestResult result = codeExecutionService.executeTestCases(
                    candidateAnswer, question.getTestCases(), detectLanguage(candidateAnswer));
            
            log.info("Test execution result: {}/{} tests passed ({}%)", 
                    result.passedTests, result.totalTests, result.passPercentage);
            
            // Calculate proportional points based on pass percentage
            int earnedPoints = (int) Math.round(maxPoints * (result.passPercentage / 100.0));
            
            // Consider it "correct" if > 50% tests passed
            boolean isCorrect = result.passPercentage >= 50.0;
            
            String feedback = String.format("%d/%d test cases passed (%.1f%%)", 
                    result.passedTests, result.totalTests, result.passPercentage);
            
            log.info("Coding question evaluated: {} - {} points awarded", feedback, earnedPoints);
            
            return new EvaluationResult(isCorrect, earnedPoints, feedback);
        }
        
        // For SCENARIO and other question types
        boolean hasSubstantialAnswer = candidateAnswer.trim().length() > 50;
        int points = hasSubstantialAnswer ? maxPoints : 0;
        return new EvaluationResult(hasSubstantialAnswer, points, 
                hasSubstantialAnswer ? "Substantial answer provided" : "Answer too short");
    }

    /**
     * Detect programming language from code
     */
    private String detectLanguage(String code) {
        String lowerCode = code.toLowerCase();
        
        if (lowerCode.contains("public class") || lowerCode.contains("public static void main")) {
            return "java";
        } else if (lowerCode.contains("def ") || lowerCode.contains("import ") && lowerCode.contains("python")) {
            return "python";
        } else if (lowerCode.contains("function ") || lowerCode.contains("const ") || lowerCode.contains("let ")) {
            return "javascript";
        }
        
        // Default to java for this system
        return "java";
    }

    /**
     * Evaluates the candidate's answer against the correct answer.
     * For MCQ: checks if the answer matches the correct answer (case-insensitive)
     * For CODING/SCENARIO: checks if answer is not empty (manual review needed)
     */
    private boolean evaluateAnswer(Question question, String candidateAnswer) {
        log.info("===== EVALUATE ANSWER CALLED =====");
        log.info("Question ID: {}, Type: {}", question.getId(), question.getType());
        log.info("Candidate Answer: '{}'", candidateAnswer);
        log.info("Correct Answer: '{}'", question.getCorrectAnswer());
        
        if (candidateAnswer == null || candidateAnswer.trim().isEmpty()) {
            log.info("Answer is null or empty, returning false");
            return false;
        }
        
        // For MCQ questions, check exact match with correct answer
        if (question.getType() == Question.QuestionType.MCQ) {
            log.info("Processing MCQ question");
            if (question.getCorrectAnswer() == null) {
                log.warn("Question {} has no correct answer stored", question.getId());
                return false;
            }
            
            log.info("Evaluating MCQ question {}: candidate='{}' correct='{}'", 
                    question.getId(), candidateAnswer, question.getCorrectAnswer());
            
            // Extract option letters from both answers
            String candidateLetter = extractOptionLetter(candidateAnswer);
            String correctLetter = extractOptionLetter(question.getCorrectAnswer());
            
            // Primary comparison: letter-based (A, B, C, D)
            if (candidateLetter != null && correctLetter != null) {
                boolean letterMatch = candidateLetter.equalsIgnoreCase(correctLetter);
                log.info("Letter comparison: candidate='{}' correct='{}' result={}", 
                        candidateLetter, correctLetter, letterMatch);
                return letterMatch;
            }
            
            // Fallback: Full text comparison (normalized, without option labels)
            String normalizedCorrect = normalizeAnswer(question.getCorrectAnswer());
            String normalizedCandidate = normalizeAnswer(candidateAnswer);
            boolean textMatch = normalizedCorrect.equalsIgnoreCase(normalizedCandidate);
            log.info("Text comparison: candidate='{}' correct='{}' result={}", 
                    normalizedCandidate, normalizedCorrect, textMatch);
            
            return textMatch;
        }
        
        // For CODING and SCENARIO questions, mark as submitted (requires manual review)
        // But give points if the answer is substantial (more than 50 characters)
        return candidateAnswer.trim().length() > 50;
    }
    
    /**
     * Extracts the option letter (A, B, C, D) from an answer string.
     * Handles: "A", "A)", "A.", "(A)", "A) text here", etc.
     */
    private String extractOptionLetter(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return null;
        }
        
        String trimmed = answer.trim().toUpperCase();
        
        // Pattern 1: Just the letter "A", "B", "C", "D"
        if (trimmed.matches("^[A-D]$")) {
            return trimmed;
        }
        
        // Pattern 2: Letter with punctuation "A)", "A.", "(A)", "A:", etc.
        if (trimmed.matches("^[(]?[A-D][).:].*")) {
            // Extract just the letter
            for (char c : trimmed.toCharArray()) {
                if (c >= 'A' && c <= 'D') {
                    return String.valueOf(c);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Normalizes answer by removing option labels and trimming whitespace
     * Handles formats like: "A)", "A.", "(A)", "A:" followed by text
     */
    private String normalizeAnswer(String answer) {
        if (answer == null) return "";
        
        String normalized = answer.trim();
        
        // Remove common option prefixes like "A)", "B)", "A.", "B.", "(A)", "A:", etc.
        // Pattern: optional "(", letter A-D, optional ")" or "." or ":", trim following spaces
        normalized = normalized.replaceAll("^[(]?[A-Da-d][).:]\\s*", "");
        
        return normalized.trim();
    }

    private ExamAttempt.DifficultyLevel upgradeDifficulty(ExamAttempt.DifficultyLevel current) {
        return switch (current) {
            case EASY -> ExamAttempt.DifficultyLevel.MEDIUM;
            case MEDIUM -> ExamAttempt.DifficultyLevel.HARD;
            case HARD -> ExamAttempt.DifficultyLevel.ADVANCED;
            case ADVANCED -> ExamAttempt.DifficultyLevel.ADVANCED;
        };
    }

    private ExamAttempt.DifficultyLevel downgradeDifficulty(ExamAttempt.DifficultyLevel current) {
        return switch (current) {
            case ADVANCED -> ExamAttempt.DifficultyLevel.HARD;
            case HARD -> ExamAttempt.DifficultyLevel.MEDIUM;
            case MEDIUM -> ExamAttempt.DifficultyLevel.EASY;
            case EASY -> ExamAttempt.DifficultyLevel.EASY;
        };
    }

    /**
     * Get the exam pass rate from the job.
     * Uses the direct job FK on ExamAttempt (more reliable than application→job chain).
     */
    private double getExamPassRate(ExamAttempt exam) {
        try {
            // Primary: use the direct job reference on the exam attempt
            if (exam.getJob() != null) {
                Double passRate = exam.getJob().getExamPassRate();
                if (passRate != null && passRate > 0 && passRate <= 100) {
                    log.debug("Using job {} pass rate: {}%", exam.getJob().getId(), passRate);
                    return passRate;
                }
            }
            // Fallback: try through application → job
            if (exam.getApplication() != null && exam.getApplication().getJob() != null) {
                Double passRate = exam.getApplication().getJob().getExamPassRate();
                if (passRate != null && passRate > 0 && passRate <= 100) {
                    return passRate;
                }
            }
        } catch (Exception e) {
            log.warn("Could not retrieve exam pass rate from job, using default: {}", e.getMessage());
        }
        log.warn("No valid exam pass rate found for exam attempt {}, using default 60%", exam.getId());
        return 60.0; // Default pass rate
    }

    @Transactional
    public ExamResultResponse completeExam(Long examAttemptId, String sessionToken) {
        // Validate session token
        ExamAttempt exam = examAttemptRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new RuntimeException("Invalid session token"));

        if (!exam.getId().equals(examAttemptId)) {
            throw new RuntimeException("Session token mismatch");
        }

        if (exam.getStatus() != ExamAttempt.ExamStatus.IN_PROGRESS) {
            throw new RuntimeException("Exam is not in progress");
        }

        // Calculate final score
        double finalScore = calculateFinalScore(exam);
        exam.setFinalScore(finalScore);
        exam.setStatus(ExamAttempt.ExamStatus.COMPLETED);
        exam.setCompletedAt(LocalDateTime.now());
        
        // Set qualified for interview flag
        double requiredPassRate = getExamPassRate(exam);
        exam.setQualifiedForInterview(finalScore >= requiredPassRate);
        
        examAttemptRepository.save(exam);

        log.info("Exam {} completed. Final score: {}", exam.getId(), finalScore);

        // Auto-progress to interview if candidate passes
        autoProgressToInterview(exam, finalScore);

        return buildExamResult(exam);
    }

    @Transactional
    public ExamResultResponse autoSubmitExam(Long examAttemptId) {
        ExamAttempt exam = examAttemptRepository.findById(examAttemptId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        double finalScore = calculateFinalScore(exam);
        exam.setFinalScore(finalScore);
        exam.setAutoSubmitted(true);
        exam.setCompletedAt(LocalDateTime.now());
        
        if (exam.getStatus() != ExamAttempt.ExamStatus.DISQUALIFIED) {
            exam.setStatus(ExamAttempt.ExamStatus.COMPLETED);
            // Set qualified for interview flag
            double requiredPassRate = getExamPassRate(exam);
            exam.setQualifiedForInterview(finalScore >= requiredPassRate);
        }

        examAttemptRepository.save(exam);

        log.info("Exam {} auto-submitted. Final score: {}", exam.getId(), finalScore);
        
        // Auto-progress to interview if candidate passes (even on auto-submit)
        if (exam.getStatus() == ExamAttempt.ExamStatus.COMPLETED) {
            autoProgressToInterview(exam, finalScore);
        }
        
        return buildExamResult(exam);
    }

    private double calculateFinalScore(ExamAttempt exam) {
        // Get all answers for this exam
        List<ExamAnswer> answers = examAnswerRepository.findByExamAttemptIdOrderById(exam.getId());
        
        // Calculate total points possible and earned points
        int totalPointsPossible = answers.stream()
                .mapToInt(answer -> answer.getQuestion().getPoints() != null ? answer.getQuestion().getPoints() : 10)
                .sum();
        
        int earnedPoints = answers.stream()
                .mapToInt(ExamAnswer::getPointsEarned)
                .sum();
        
        // If no answers yet, default calculation
        if (totalPointsPossible == 0) {
            totalPointsPossible = exam.getTotalQuestions() * 10;
            earnedPoints = exam.getCorrectAnswers() * 10;
        }

        // Calculate percentage
        double percentage = totalPointsPossible > 0 
                ? ((double) earnedPoints / totalPointsPossible) * 100 
                : 0.0;
        
        // Apply cheating penalty (reduce score by 10% per cheating point)
        double penalty = exam.getCheatingScore() * 10.0;
        
        double finalScore = Math.max(0, percentage - penalty);
        
        log.info("Calculated final score for exam {}: earned={}/{} ({}%), penalty={}, final={}", 
                exam.getId(), earnedPoints, totalPointsPossible, percentage, penalty, finalScore);
        
        return finalScore;
    }

    @Transactional(readOnly = true)
    public ExamResultResponse getExamResult(Long examAttemptId) {
        ExamAttempt exam = examAttemptRepository.findById(examAttemptId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        if (exam.getStatus() == ExamAttempt.ExamStatus.IN_PROGRESS) {
            throw new RuntimeException("Exam is still in progress");
        }

        return buildExamResult(exam);
    }

    private ExamResultResponse buildExamResult(ExamAttempt exam) {
        List<ExamAnswer> answers = examAnswerRepository.findByExamAttemptIdOrderById(exam.getId());
        
        List<AnswerDetailDTO> answerDetails = answers.stream()
                .map(answer -> {
                    Question question = answer.getQuestion();
                    String correctAnswer;
                    
                    // For coding questions, show test case results from feedback
                    if (question.getType() == Question.QuestionType.CODING) {
                        if (answer.getFeedback() != null && !answer.getFeedback().isEmpty()) {
                            correctAnswer = answer.getFeedback();
                        } else {
                            correctAnswer = "Manual Review Required";
                        }
                    } else {
                        // For MCQ and other types, show the actual correct answer
                        correctAnswer = question.getCorrectAnswer() != null 
                                ? question.getCorrectAnswer() 
                                : "N/A";
                    }
                    
                    return AnswerDetailDTO.builder()
                            .question(question.getQuestion())
                            .candidateAnswer(answer.getCandidateAnswer())
                            .correctAnswer(correctAnswer)
                            .isCorrect(answer.getIsCorrect())
                            .pointsEarned(answer.getPointsEarned())
                            .build();
                })
                .collect(Collectors.toList());

        // Get warning count from cheat events
        int warningCount = cheatEventRepository.countByExamAttemptId(exam.getId());

        String disqualificationReason = null;
        String message = null;
        // Use the stored qualified flag (set correctly during completeExam which is @Transactional)
        // Fall back to recalculating only if not yet persisted (e.g. very old records)
        boolean qualifiedForInterview = exam.getQualifiedForInterview() != null
                ? exam.getQualifiedForInterview()
                : false;
        
        if (exam.getStatus() == ExamAttempt.ExamStatus.DISQUALIFIED) {
            List<CheatEvent> cheatEvents = cheatEventRepository.findByExamAttemptIdOrderByDetectedAtDesc(exam.getId());
            disqualificationReason = String.format(
                    "Disqualified due to cheating (Score: %d, Warnings: %d). Events: %s",
                    exam.getCheatingScore(),
                    warningCount,
                    cheatEvents.stream()
                            .map(e -> e.getType().name())
                            .collect(Collectors.joining(", "))
            );
            message = "You have been disqualified from this exam due to multiple violations.";
        } else if (exam.getStatus() == ExamAttempt.ExamStatus.COMPLETED) {
            double requiredPassRate = getExamPassRate(exam);
            if (qualifiedForInterview) {
                message = String.format("Congratulations! You scored %.2f%% and qualified for the interview stage.", exam.getFinalScore());
            } else {
                message = String.format("You scored %.2f%%. Unfortunately, you did not meet the passing score of %.0f%%.", exam.getFinalScore(), requiredPassRate);
            }
        }

        return ExamResultResponse.builder()
                .examAttemptId(exam.getId())
                .status(exam.getStatus().name())
                .finalScore(exam.getFinalScore())
                .totalQuestions(exam.getTotalQuestions())
                .answeredQuestions(exam.getAnsweredQuestions())
                .correctAnswers(exam.getCorrectAnswers())
                .cheatingScore(exam.getCheatingScore())
                .warningCount(warningCount)
                .autoSubmitted(exam.getAutoSubmitted())
                .disqualificationReason(disqualificationReason)
                .qualifiedForInterview(qualifiedForInterview)
                .message(message)
                .answers(answerDetails)
                .build();
    }
    
    private void autoProgressToInterview(ExamAttempt exam, double finalScore) {
        try {
            // Get the required pass rate from job
            double requiredPassRate = getExamPassRate(exam);
            
            // Only progress if candidate passed and has an associated application
            if (finalScore >= requiredPassRate && exam.getApplication() != null) {
                Application application = exam.getApplication();
                
                // Only update if current status is SUBMITTED, SCREENING, UNDER_REVIEW, or EXAM_ELIGIBLE
                if (application.getStatus() == Application.ApplicationStatus.SUBMITTED ||
                    application.getStatus() == Application.ApplicationStatus.SCREENING ||
                    application.getStatus() == Application.ApplicationStatus.UNDER_REVIEW ||
                    application.getStatus() == Application.ApplicationStatus.EXAM_ELIGIBLE) {
                    
                    application.setStatus(Application.ApplicationStatus.INTERVIEWING);
                    application.setNotes(String.format(
                        "Auto-progressed to interview: Exam score %.2f%% (Warnings: %d)",
                        finalScore,
                        cheatEventRepository.countByExamAttemptId(exam.getId())
                    ));
                    applicationRepository.save(application);
                    
                    log.info("Application {} auto-progressed to INTERVIEWING status. Exam score: {}%", 
                        application.getId(), finalScore);
                    
                    // Create interview entity and store in database
                    Interview interview = new Interview();
                    interview.setType(Interview.InterviewType.TECHNICAL);
                    interview.setStatus(Interview.InterviewStatus.SCHEDULED);
                    // Schedule interview for 2 business days from now
                    interview.setScheduledAt(LocalDateTime.now().plusDays(2));
                    interview.setDurationMinutes(60); // Default 1 hour technical interview
                    interview.setNotes(String.format(
                        "Auto-scheduled after passing exam with score %.2f%%", 
                        finalScore
                    ));
                    
                    Interview savedInterview = interviewService.createInterview(application.getId(), interview);
                    
                    log.info("Interview {} automatically created for application {} - scheduled for {}", 
                        savedInterview.getId(), application.getId(), savedInterview.getScheduledAt());

                    // Send exam passed + interview scheduled email
                    try {
                        notificationService.notifyExamResult(
                            application.getCandidate().getEmail(),
                            application.getCandidate().getFirstName() + " " + application.getCandidate().getLastName(),
                            application.getJob().getTitle(),
                            finalScore,
                            requiredPassRate,
                            true
                        );
                    } catch (Exception ne) {
                        log.error("Failed to send exam-pass email for application {}: {}", application.getId(), ne.getMessage());
                    }
                }
            } else if (finalScore < requiredPassRate && exam.getApplication() != null) {
                // Failed exam - keep in SCREENING/EXAM_ELIGIBLE or move to REJECTED
                Application application = exam.getApplication();
                if (application.getStatus() == Application.ApplicationStatus.SCREENING ||
                    application.getStatus() == Application.ApplicationStatus.EXAM_ELIGIBLE) {
                    application.setNotes(String.format(
                        "Exam failed with score %.2f%% (Minimum: %.0f%%, Warnings: %d)",
                        finalScore,
                        requiredPassRate,
                        cheatEventRepository.countByExamAttemptId(exam.getId())
                    ));
                    applicationRepository.save(application);
                    
                    log.info("Application {} remains in SCREENING - exam score below threshold: {}%", 
                        application.getId(), finalScore);

                    // Send exam failed email
                    try {
                        notificationService.notifyExamResult(
                            application.getCandidate().getEmail(),
                            application.getCandidate().getFirstName() + " " + application.getCandidate().getLastName(),
                            application.getJob().getTitle(),
                            finalScore,
                            requiredPassRate,
                            false
                        );
                    } catch (Exception ne) {
                        log.error("Failed to send exam-fail email for application {}: {}", application.getId(), ne.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error auto-progressing application to interview: {}", e.getMessage(), e);
            // Don't throw - we still want to complete the exam even if status update fails
        }
    }

    /**
     * Recalculates qualifiedForInterview for all completed exams for a given job,
     * using the job's CURRENT examPassRate. Called when admin changes the pass rate.
     */
    @Transactional
    public Map<String, Object> recalculateJobQualifications(Long jobId) {
        List<ExamAttempt> allExams = examAttemptRepository.findByJob_IdOrderByFinalScoreDesc(jobId);
        
        // Get the current pass rate from the job (using first exam's job reference)
        double passRate = 60.0;
        Optional<ExamAttempt> anyWithJob = allExams.stream()
                .filter(e -> e.getJob() != null && e.getJob().getExamPassRate() != null)
                .findFirst();
        if (anyWithJob.isPresent()) {
            Double rate = anyWithJob.get().getJob().getExamPassRate();
            if (rate > 0 && rate <= 100) passRate = rate;
        }

        int total = 0, updated = 0, nowQualified = 0, nowDisqualified = 0;

        for (ExamAttempt exam : allExams) {
            if (exam.getStatus() != ExamAttempt.ExamStatus.COMPLETED) continue;
            total++;

            boolean wasQualified = Boolean.TRUE.equals(exam.getQualifiedForInterview());
            boolean shouldQualify = exam.getFinalScore() >= passRate;

            // Also fix candidates already qualified but stuck in EXAM_ELIGIBLE with no interview
            if (wasQualified && shouldQualify && exam.getApplication() != null) {
                Application application = exam.getApplication();
                if (application.getStatus() == Application.ApplicationStatus.EXAM_ELIGIBLE) {
                    try {
                        List<Interview> existingInterviews = interviewService.getInterviewsByApplication(application.getId());
                        if (existingInterviews.isEmpty()) {
                            application.setStatus(Application.ApplicationStatus.INTERVIEWING);
                            application.setNotes(String.format(
                                "Auto-fixed: Interview scheduled after recalculation (score %.1f%% >= threshold %.0f%%)",
                                exam.getFinalScore(), passRate));
                            applicationRepository.save(application);

                            Interview interview = new Interview();
                            interview.setType(Interview.InterviewType.TECHNICAL);
                            interview.setStatus(Interview.InterviewStatus.SCHEDULED);
                            interview.setScheduledAt(LocalDateTime.now().plusDays(2));
                            interview.setDurationMinutes(60);
                            interview.setNotes(String.format(
                                "Auto-scheduled during recalculation fix: score %.1f%% >= threshold %.0f%%",
                                exam.getFinalScore(), passRate));

                            Interview savedInterview = interviewService.createInterview(application.getId(), interview);
                            log.info("Retroactive interview {} created for application {} stuck in EXAM_ELIGIBLE - scheduled for {}",
                                savedInterview.getId(), application.getId(), savedInterview.getScheduledAt());
                            nowQualified++;
                            updated++;
                        }
                    } catch (Exception e) {
                        log.warn("Could not retroactively fix interview for application {}: {}", application.getId(), e.getMessage());
                    }
                }
            }

            if (wasQualified != shouldQualify) {
                exam.setQualifiedForInterview(shouldQualify);
                examAttemptRepository.save(exam);
                updated++;

                if (shouldQualify) {
                    nowQualified++;
                    // Auto-progress application to INTERVIEWING if it hasn't moved past SCREENING
                    try {
                        if (exam.getApplication() != null) {
                            Application application = exam.getApplication();
                            if (application.getStatus() == Application.ApplicationStatus.SUBMITTED ||
                                application.getStatus() == Application.ApplicationStatus.SCREENING ||
                                application.getStatus() == Application.ApplicationStatus.UNDER_REVIEW ||
                                application.getStatus() == Application.ApplicationStatus.EXAM_ELIGIBLE) {
                                application.setStatus(Application.ApplicationStatus.INTERVIEWING);
                                application.setNotes(String.format(
                                    "Auto-qualified after pass rate adjustment: score %.1f%% >= new threshold %.0f%%",
                                    exam.getFinalScore(), passRate));
                                applicationRepository.save(application);
                                log.info("Application {} promoted to INTERVIEWING after pass rate recalculation", application.getId());

                                // Create interview entity so it appears on the candidate's schedule
                                Interview interview = new Interview();
                                interview.setType(Interview.InterviewType.TECHNICAL);
                                interview.setStatus(Interview.InterviewStatus.SCHEDULED);
                                // Schedule interview for 2 business days from now
                                interview.setScheduledAt(LocalDateTime.now().plusDays(2));
                                interview.setDurationMinutes(60);
                                interview.setNotes(String.format(
                                    "Auto-scheduled after pass rate recalculation: score %.1f%% >= threshold %.0f%%",
                                    exam.getFinalScore(), passRate));

                                Interview savedInterview = interviewService.createInterview(application.getId(), interview);
                                log.info("Interview {} automatically created for application {} during recalculation - scheduled for {}",
                                    savedInterview.getId(), application.getId(), savedInterview.getScheduledAt());
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Could not update application status during recalculation: {}", e.getMessage());
                    }
                } else {
                    nowDisqualified++;
                    log.info("Exam {} no longer qualifies after pass rate change (score {}<{})",
                            exam.getId(), exam.getFinalScore(), passRate);
                }
            }
        }

        log.info("Recalculation for job {}: passRate={}%, totalCompleted={}, updated={}, +qualified={}, -disqualified={}",
                jobId, passRate, total, updated, nowQualified, nowDisqualified);

        Map<String, Object> result = new HashMap<>();
        result.put("jobId", jobId);
        result.put("passRate", passRate);
        result.put("totalCompleted", total);
        result.put("updated", updated);
        result.put("nowQualified", nowQualified);
        result.put("nowDisqualified", nowDisqualified);
        return result;
    }
}
