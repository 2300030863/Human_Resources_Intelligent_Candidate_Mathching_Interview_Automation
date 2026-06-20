package com.lernathon.recruitment.controller;

import com.lernathon.recruitment.dto.*;
import com.lernathon.recruitment.entity.ExamAttempt;
import com.lernathon.recruitment.service.CheatDetectionService;
import com.lernathon.recruitment.service.CodeExecutionService;
import com.lernathon.recruitment.service.ExamService;
import com.lernathon.recruitment.service.ExamSubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/exams")
@RequiredArgsConstructor
@Slf4j
public class ExamController {

    private final ExamService examService;
    private final ExamSubmissionService examSubmissionService;
    private final CheatDetectionService cheatDetectionService;
    private final CodeExecutionService codeExecutionService;

    @PostMapping("/generate")
    public ResponseEntity<ExamGenerationResponse> generateExam(
            @Valid @RequestBody ExamGenerationRequest request,
            Authentication authentication
    ) {
        ExamGenerationResponse response = examService.generateExam(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{examAttemptId}/start")
    public ResponseEntity<String> startExam(
            @PathVariable Long examAttemptId,
            @RequestParam String sessionToken,
            HttpServletRequest request
    ) {
        // Verify session token
        examService.getExamBySessionToken(sessionToken);
        
        String ipAddress = getClientIpAddress(request);
        examService.startExam(examAttemptId, ipAddress);
        
        return ResponseEntity.ok("Exam started successfully");
    }

    @PostMapping("/submit-answer")
    public ResponseEntity<SubmitAnswerResponse> submitAnswer(
            @Valid @RequestBody SubmitAnswerRequest request
    ) {
        try {
            SubmitAnswerResponse response = examSubmissionService.submitAnswer(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log the error for debugging
            System.err.println("Error submitting answer: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @PostMapping("/cheat-event")
    public ResponseEntity<CheatEventResponse> recordCheatEvent(
            @RequestBody CheatEventRequest request
    ) {
        CheatEventResponse response = cheatDetectionService.recordCheatEvent(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{examAttemptId}/complete")
    public ResponseEntity<ExamResultResponse> completeExam(
            @PathVariable Long examAttemptId,
            @RequestParam String sessionToken
    ) {
        ExamResultResponse response = examSubmissionService.completeExam(examAttemptId, sessionToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{examAttemptId}/auto-submit")
    public ResponseEntity<ExamResultResponse> autoSubmitExam(
            @PathVariable Long examAttemptId,
            @RequestParam String sessionToken
    ) {
        // Verify session token
        examService.getExamBySessionToken(sessionToken);
        
        ExamResultResponse response = examSubmissionService.autoSubmitExam(examAttemptId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{examAttemptId}/result")
    public ResponseEntity<ExamResultResponse> getExamResult(
            @PathVariable Long examAttemptId
    ) {
        ExamResultResponse response = examSubmissionService.getExamResult(examAttemptId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{examAttemptId}/cheating-score")
    public ResponseEntity<Integer> getCheatingScore(
            @PathVariable Long examAttemptId
    ) {
        int score = cheatDetectionService.getCurrentCheatingScore(examAttemptId);
        return ResponseEntity.ok(score);
    }

    @GetMapping("/{examAttemptId}/status")
    public ResponseEntity<Boolean> isExamDisqualified(
            @PathVariable Long examAttemptId
    ) {
        boolean disqualified = cheatDetectionService.isExamDisqualified(examAttemptId);
        return ResponseEntity.ok(disqualified);
    }

    @GetMapping("/{examAttemptId}/details")
    public ResponseEntity<ExamGenerationResponse> getExamDetails(
            @PathVariable Long examAttemptId
    ) {
        ExamGenerationResponse response = examService.getExamDetails(examAttemptId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/candidate/{candidateId}")
    public ResponseEntity<List<ExamAttempt>> getCandidateExamAttempts(
            @PathVariable Long candidateId
    ) {
        List<ExamAttempt> attempts = examService.getExamAttemptsByCandidate(candidateId);
        return ResponseEntity.ok(attempts);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ExamAttempt>> getAllExamAttempts() {
        List<ExamAttempt> attempts = examService.getAllExamAttempts();
        return ResponseEntity.ok(attempts);
    }

    @PostMapping("/job/{jobId}/recalculate")
    public ResponseEntity<Map<String, Object>> recalculateJobQualifications(
            @PathVariable Long jobId
    ) {
        log.info("Admin triggered recalculation of exam qualifications for jobId: {}", jobId);
        Map<String, Object> result = examSubmissionService.recalculateJobQualifications(jobId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/test-code")
    public ResponseEntity<CodeExecutionService.TestResult> testCode(
            @RequestBody TestCodeRequest request
    ) {
        try {
            CodeExecutionService.TestResult result = codeExecutionService.executeTestCases(
                request.getCode(),
                request.getTestCases(),
                request.getLanguage()
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error executing test cases: " + e.getMessage(), e);
            throw new RuntimeException("Failed to execute test cases: " + e.getMessage());
        }
    }

    @PostMapping("/compile")
    public ResponseEntity<CompileResponse> compileCode(
            @RequestBody CompileRequest request
    ) {
        try {
            CompileResponse response = codeExecutionService.compileCode(
                request.getCode(),
                request.getLanguage()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error compiling code: " + e.getMessage(), e);
            return ResponseEntity.ok(CompileResponse.builder()
                .success(false)
                .message("Compilation failed: " + e.getMessage())
                .build());
        }
    }

    @PostMapping("/run")
    public ResponseEntity<RunCodeResponse> runCode(
            @RequestBody RunCodeRequest request
    ) {
        try {
            RunCodeResponse response = codeExecutionService.runCode(
                request.getCode(),
                request.getInput(),
                request.getLanguage()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error running code: " + e.getMessage(), e);
            return ResponseEntity.ok(RunCodeResponse.builder()
                .status("FAILED")
                .output("")
                .error("Execution failed: " + e.getMessage())
                .executionTime("0ms")
                .build());
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<CodeSubmitResponse> submitCodeForEvaluation(
            @RequestBody CodeSubmitRequest request
    ) {
        try {
            log.info("Submitting code for evaluation - QuestionId: {}, ExamAttemptId: {}, TestCases: {}", 
                request.getQuestionId(), request.getExamAttemptId(), 
                request.getTestCases() != null ? request.getTestCases().size() : 0);

            // Execute test cases
            final CodeExecutionService.TestResult testResult = codeExecutionService.executeTestCases(
                request.getCode(),
                request.getTestCases(),
                request.getLanguage()
            );

            // Calculate marks
            double maxMarks = request.getMaxMarks() != null ? request.getMaxMarks() : 25.0;
            double marksAwarded = testResult.totalTests > 0 
                ? (testResult.passedTests * maxMarks / testResult.totalTests) 
                : 0;

            // Determine status
            String status;
            if (testResult.passedTests == testResult.totalTests && testResult.totalTests > 0) {
                status = "PASS";
            } else if (testResult.passedTests > 0) {
                status = "PARTIAL_PASS";
            } else {
                status = "FAIL";
            }

            // Build detailed results
            List<CodeSubmitResponse.TestCaseDetail> details = new java.util.ArrayList<>();
            for (int i = 0; i < testResult.totalTests; i++) {
                final int testIndex = i; // Make effectively final for lambda
                final int testNumber = i + 1;
                
                boolean passed = testIndex < testResult.passedTests || 
                    (testResult.failedTests != null && testResult.failedTests.stream()
                        .noneMatch(f -> f.contains("Test " + testNumber)));
                
                String expected = testIndex < request.getTestCases().size() 
                    ? request.getTestCases().get(testIndex).expectedOutput : "";
                    
                String received = "";
                if (testResult.failedTests != null && !passed) {
                    received = testResult.failedTests.stream()
                        .filter(f -> f.contains("Test " + testNumber))
                        .findFirst().orElse("Unknown error");
                }
                
                details.add(CodeSubmitResponse.TestCaseDetail.builder()
                    .testCase(testNumber)
                    .status(passed ? "PASSED" : "FAILED")
                    .expected(expected)
                    .received(received)
                    .build());
            }

            CodeSubmitResponse response = CodeSubmitResponse.builder()
                .status(status)
                .totalTestCases(testResult.totalTests)
                .passed(testResult.passedTests)
                .marksAwarded(marksAwarded)
                .maxMarks(maxMarks)
                .details(details)
                .message(String.format("%d/%d test cases passed", 
                    testResult.passedTests, testResult.totalTests))
                .build();

            log.info("Code evaluation complete: {}/{} tests passed, {} marks awarded", 
                testResult.passedTests, testResult.totalTests, marksAwarded);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error evaluating code: " + e.getMessage(), e);
            
            double maxMarks = request.getMaxMarks() != null ? request.getMaxMarks() : 25.0;
            
            return ResponseEntity.ok(CodeSubmitResponse.builder()
                .status("ERROR")
                .totalTestCases(request.getTestCases() != null ? request.getTestCases().size() : 0)
                .passed(0)
                .marksAwarded(0)
                .maxMarks(maxMarks)
                .details(java.util.Collections.emptyList())
                .message("Evaluation error: " + e.getMessage())
                .build());
        }
    }

    @PostMapping("/generate-samples")
    public ResponseEntity<?> generateSamples(@RequestBody GenerateSamplesRequest request) {
        try {
            log.info("Generating samples for questionId: {}, language: {}", 
                request.getQuestionId(), request.getLanguage());
            
            GenerateSamplesResponse response = examService.generateSamples(
                request.getQuestionId(), 
                request.getLanguage(), 
                request.getCode()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating samples: " + e.getMessage(), e);
            return ResponseEntity.status(500).body(
                java.util.Map.of("message", "Failed to generate samples: " + e.getMessage())
            );
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
