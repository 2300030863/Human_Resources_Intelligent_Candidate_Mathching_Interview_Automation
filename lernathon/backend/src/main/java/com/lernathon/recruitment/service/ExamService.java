package com.lernathon.recruitment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lernathon.recruitment.dto.*;
import com.lernathon.recruitment.entity.*;
import com.lernathon.recruitment.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class ExamService {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final QuestionRepository questionRepository;
    private final CandidateService candidateService;
    private final JobService jobService;
    private final ApplicationRepository applicationRepository;
    private final CodeforcesService codeforcesService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${groq.api.key:your-groq-api-key}")
    private String groqApiKey;

    private static final int MCQ_COUNT = 10;
    private static final int CODING_COUNT = 2;
    private static final int SCENARIO_COUNT = 1;

    @Transactional
    public ExamGenerationResponse generateExam(ExamGenerationRequest request) {
        // Validate request
        if (request.getCandidateId() == null) {
            throw new IllegalArgumentException("Candidate ID is required");
        }
        if (request.getJobId() == null) {
            throw new IllegalArgumentException("Job ID is required");
        }
        if (request.getResumeMatchScore() == null) {
            throw new IllegalArgumentException("Resume match score is required");
        }
        
        // Validate resume match score
        if (request.getResumeMatchScore() < 80.0) {
            throw new IllegalArgumentException("Resume match score must be at least 80% to generate exam");
        }

        // Check if exam already exists
        Optional<ExamAttempt> existing = examAttemptRepository.findByCandidate_IdAndJob_Id(
                request.getCandidateId(), request.getJobId()
        );
        if (existing.isPresent() && existing.get().getStatus() != ExamAttempt.ExamStatus.DISQUALIFIED) {
            throw new RuntimeException("Exam already exists for this candidate and job");
        }

        // Fetch job and candidate details
        Job job = jobService.getJobById(request.getJobId());
        Candidate candidate = candidateService.getCandidateById(request.getCandidateId());

        // Determine difficulty based on experience
        ExamAttempt.DifficultyLevel difficulty = determineDifficulty(job.getExperienceRequired());

        // Generate session token
        String sessionToken = UUID.randomUUID().toString();

        // Create exam attempt
        ExamAttempt examAttempt = ExamAttempt.builder()
                .candidate(candidate)
                .job(job)
                .application(request.getApplicationId() != null ? 
                        applicationRepository.findById(request.getApplicationId()).orElse(null) : null)
                .status(ExamAttempt.ExamStatus.NOT_STARTED)
                .currentDifficulty(difficulty)
                .cheatingScore(0)
                .consecutiveCorrect(0)
                .consecutiveWrong(0)
                .sessionToken(sessionToken)
                .totalQuestions(MCQ_COUNT + CODING_COUNT + SCENARIO_COUNT)
                .build();

        examAttempt = examAttemptRepository.save(examAttempt);

        // Generate questions based on source
        List<QuestionDTO> questions;
        String source = request.getQuestionSource() != null ? request.getQuestionSource().toUpperCase() : "AI";
        
        if ("CODEFORCES".equals(source)) {
            log.info("Generating exam with Codeforces problems");
            questions = generateCodeforcesQuestions(job, difficulty);
        } else {
            log.info("Generating exam with AI-generated questions");
            questions = generateAIQuestions(job, difficulty);
        }

        return ExamGenerationResponse.builder()
                .examAttemptId(examAttempt.getId())
                .sessionToken(sessionToken)
                .totalQuestions(examAttempt.getTotalQuestions())
                .timeLimit(90) // 90 minutes
                .difficulty(difficulty.name())
                .message("Exam generated successfully. Good luck!")
                .questions(questions)
                .build();
    }

    private ExamAttempt.DifficultyLevel determineDifficulty(Integer experienceRequired) {
        if (experienceRequired == null || experienceRequired <= 1) {
            return ExamAttempt.DifficultyLevel.EASY;
        } else if (experienceRequired <= 4) {
            return ExamAttempt.DifficultyLevel.MEDIUM;
        } else {
            return ExamAttempt.DifficultyLevel.HARD;
        }
    }

    private List<QuestionDTO> generateAIQuestions(Job job, ExamAttempt.DifficultyLevel difficulty) {
        List<QuestionDTO> questions = new ArrayList<>();

        try {
            String prompt = buildPrompt(job, difficulty);
            String aiResponse = callGroqAPI(prompt);
            questions = parseAIResponse(aiResponse);
        } catch (Exception e) {
            log.error("AI question generation failed, using fallback", e);
            questions = generateFallbackQuestions(job, difficulty);
        }

        return questions;
    }

    private String buildPrompt(Job job, ExamAttempt.DifficultyLevel difficulty) {
        // Add timestamp to ensure uniqueness
        String timestamp = java.time.LocalDateTime.now().toString();
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        
        return String.format("""
                Generate a UNIQUE and FRESH technical exam for the following job role.
                DO NOT repeat questions from previous exams. Each exam must be completely different.
                
                Exam ID: %s
                Generated at: %s
                
                Job Title: %s
                Required Skills: %s
                Experience Level: %s years
                Difficulty: %s
                
                Generate exactly:
                - %d Multiple Choice Questions (MCQ) covering the required skills
                - %d Coding/Practical Questions (LEETCODE-STYLE with detailed test cases)
                - %d Real-world Scenario Question
                
                IMPORTANT: Questions must be:
                1. UNIQUE - Do not repeat common questions
                2. SKILL-SPECIFIC - Directly related to: %s
                3. DIFFICULTY-APPROPRIATE - Match %s level
                4. PRACTICAL - Test real-world application, not just theory
                
                For each MCQ, provide:
                - Question text (unique, not commonly asked)
                - 4 options (A, B, C, D)
                - Correct answer (single letter: A, B, C, or D)
                - Brief explanation
                - Points (5-10 based on complexity)
                
                For Coding questions (LEETCODE-STYLE FORMAT):
                - Clear problem title
                - Detailed problem statement explaining the task
                - Input/Output format specification
                - At least 2 example test cases shown in the problem description (use actual values)
                - Constraints (time/space complexity, input ranges)
                - Array of 5-8 comprehensive test cases including:
                  * 2 simple edge cases (empty, single element, etc.)
                  * 3-4 normal cases with varying sizes
                  * 1-2 complex edge cases (max values, special conditions)
                - Points (20-30)
                
                CRITICAL - Test Cases Format for Coding Questions:
                Each test case must have EXACT input/output that can be used programmatically.
                
                Example for "Find Maximum in Array":
                {
                  "question": "**Maximum Element in Array**\\n\\nGiven an integer array nums, find the maximum element.\\n\\n**Example 1:**\\nInput: [2, 7, 3, 1]\\nOutput: 7\\n\\n**Example 2:**\\nInput: [-5, -2, -8]\\nOutput: -2\\n\\n**Constraints:**\\n- 1 <= nums.length <= 10^4\\n- -10^9 <= nums[i] <= 10^9",
                  "codeSnippet": "# Example starter code\\ndef find_max(nums):\\n    # Your code here\\n    pass",
                  "skill": "Python",
                  "points": 25,
                  "testCases": [
                    {"input": "[5]", "expectedOutput": "5"},
                    {"input": "[2, 7, 3, 1]", "expectedOutput": "7"},
                    {"input": "[-5, -2, -8]", "expectedOutput": "-2"},
                    {"input": "[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]", "expectedOutput": "10"},
                    {"input": "[100, 50, 25, 75, 90]", "expectedOutput": "100"}
                  ]
                }
                
                Example for "Two Sum":
                {
                  "question": "**Two Sum**\\n\\nGiven an array of integers nums and an integer target, return the indices of two numbers that add up to target.\\n\\n**Example 1:**\\nInput: nums = [2,7,11,15], target = 9\\nOutput: [0,1]\\nExplanation: nums[0] + nums[1] = 2 + 7 = 9\\n\\n**Example 2:**\\nInput: nums = [3,2,4], target = 6\\nOutput: [1,2]",
                  "testCases": [
                    {"input": "[2,7,11,15]\\n9", "expectedOutput": "[0,1]"},
                    {"input": "[3,2,4]\\n6", "expectedOutput": "[1,2]"},
                    {"input": "[3,3]\\n6", "expectedOutput": "[0,1]"},
                    {"input": "[1,5,3,7,2]\\n8", "expectedOutput": "[1,3]"},
                    {"input": "[-1,-2,-3,-4,-5]\\n-8", "expectedOutput": "[2,4]"}
                  ]
                }
                
                For Scenario question, provide:
                - Real-world problem description
                - What to design/explain
                - Points (30-40)
                
                Return ONLY valid JSON in this exact format (no markdown, no extra text):
                {
                  "mcq": [
                    {
                      "question": "...",
                      "options": ["A) ...", "B) ...", "C) ...", "D) ..."],
                      "correctAnswer": "A",
                      "explanation": "...",
                      "skill": "...",
                      "points": 10
                    }
                  ],
                  "coding": [
                    {
                      "question": "**Problem Title**\\n\\nProblem description...\\n\\n**Example 1:**\\nInput: ...\\nOutput: ...\\n\\n**Constraints:**\\n- ...",
                      "codeSnippet": "# Starter code if applicable",
                      "skill": "...",
                      "points": 25,
                      "testCases": [
                        {"input": "exact input", "expectedOutput": "exact output"},
                        {"input": "edge case", "expectedOutput": "expected result"}
                      ]
                    }
                  ],
                  "scenario": [
                    {
                      "question": "...",
                      "skill": "...",
                      "points": 35
                    }
                  ]
                }
                """,
                uniqueId,
                timestamp,
                job.getTitle(),
                job.getSkillsRequired(),
                job.getExperienceRequired(),
                difficulty.name(),
                MCQ_COUNT,
                CODING_COUNT,
                SCENARIO_COUNT,
                job.getSkillsRequired(),
                difficulty.name()
        );
    }

    private String callGroqAPI(String prompt) {
        try {
            String url = "https://api.groq.com/openai/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama-3.3-70b-versatile");
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", "You are an expert technical interviewer and exam creator. Generate unique, varied questions for each exam. Never repeat the same questions."),
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", 0.9); // Higher temperature for more variation
            requestBody.put("max_tokens", 4000);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            log.error("Groq API call failed", e);
            throw new RuntimeException("Failed to generate questions using AI", e);
        }
    }

    private List<QuestionDTO> parseAIResponse(String aiResponse) {
        List<QuestionDTO> questions = new ArrayList<>();
        
        try {
            // Extract JSON from markdown code blocks if present
            String jsonContent = aiResponse;
            if (aiResponse.contains("```json")) {
                jsonContent = aiResponse.substring(
                    aiResponse.indexOf("```json") + 7,
                    aiResponse.lastIndexOf("```")
                ).trim();
            } else if (aiResponse.contains("```")) {
                jsonContent = aiResponse.substring(
                    aiResponse.indexOf("```") + 3,
                    aiResponse.lastIndexOf("```")
                ).trim();
            }

            JsonNode root = objectMapper.readTree(jsonContent);

            // Parse MCQ questions
            JsonNode mcqNode = root.path("mcq");
            if (mcqNode.isArray()) {
                for (JsonNode q : mcqNode) {
                    List<String> options = new ArrayList<>();
                    JsonNode optionsNode = q.path("options");
                    if (optionsNode.isArray()) {
                        optionsNode.forEach(opt -> options.add(opt.asText()));
                    }

                    QuestionDTO dto = QuestionDTO.builder()
                            .skill(q.path("skill").asText())
                            .level(ExamAttempt.DifficultyLevel.MEDIUM.name())
                            .type(Question.QuestionType.MCQ.name())
                            .question(q.path("question").asText())
                            .options(options)
                            .points(q.path("points").asInt(10))
                            .timeLimit(120) // 2 minutes per MCQ
                            .build();
                    
                    // Save to database and get real ID
                    Long questionId = saveQuestionToDatabase(dto, q.path("correctAnswer").asText(), q.path("explanation").asText(), null);
                    dto.setId(questionId);
                    questions.add(dto);
                }
            }

            // Parse Coding questions
            JsonNode codingNode = root.path("coding");
            if (codingNode.isArray()) {
                for (JsonNode q : codingNode) {
                    // Extract test cases if present
                    String testCasesJson = null;
                    List<QuestionDTO.TestCaseDTO> testCasesList = new ArrayList<>();
                    JsonNode testCasesNode = q.path("testCases");
                    if (testCasesNode.isArray() && !testCasesNode.isEmpty()) {
                        testCasesJson = objectMapper.writeValueAsString(testCasesNode);
                        // Parse test cases for DTO
                        for (JsonNode tc : testCasesNode) {
                            testCasesList.add(QuestionDTO.TestCaseDTO.builder()
                                    .input(tc.path("input").asText())
                                    .expectedOutput(tc.path("expectedOutput").asText())
                                    .build());
                        }
                    }
                    
                    QuestionDTO dto = QuestionDTO.builder()
                            .skill(q.path("skill").asText())
                            .level(ExamAttempt.DifficultyLevel.HARD.name())
                            .type(Question.QuestionType.CODING.name())
                            .question(q.path("question").asText())
                            .codeSnippet(q.path("codeSnippet").asText(""))
                            .testCases(testCasesList)
                            .points(q.path("points").asInt(25))
                            .timeLimit(900) // 15 minutes per coding
                            .build();
                    
                    // Save to database and get real ID, passing test cases
                    Long questionId = saveQuestionToDatabase(dto, null, null, testCasesJson);
                    dto.setId(questionId);
                    questions.add(dto);
                }
            }

            // Parse Scenario questions
            JsonNode scenarioNode = root.path("scenario");
            if (scenarioNode.isArray()) {
                for (JsonNode q : scenarioNode) {
                    QuestionDTO dto = QuestionDTO.builder()
                            .skill(q.path("skill").asText())
                            .level(ExamAttempt.DifficultyLevel.HARD.name())
                            .type(Question.QuestionType.SCENARIO.name())
                            .question(q.path("question").asText())
                            .points(q.path("points").asInt(35))
                            .timeLimit(1200) // 20 minutes for scenario
                            .build();
                    
                    // Save to database and get real ID
                    Long questionId = saveQuestionToDatabase(dto, null, null, null);
                    dto.setId(questionId);
                    questions.add(dto);
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse AI response", e);
            throw new RuntimeException("Failed to parse AI generated questions", e);
        }

        return questions;
    }

    private Long saveQuestionToDatabase(QuestionDTO dto, String correctAnswer, String explanation, String testCases) {
        try {
            Question question = Question.builder()
                    .skill(dto.getSkill())
                    .level(Question.DifficultyLevel.valueOf(dto.getLevel()))
                    .type(Question.QuestionType.valueOf(dto.getType()))
                    .question(dto.getQuestion())
                    .codeSnippet(dto.getCodeSnippet())
                    .options(dto.getOptions() != null ? objectMapper.writeValueAsString(dto.getOptions()) : null)
                    .correctAnswer(correctAnswer)
                    .explanation(explanation)
                    .testCases(testCases)
                    .points(dto.getPoints())
                    .timeLimit(dto.getTimeLimit())
                    .isActive(true)
                    .build();
            
            Question saved = questionRepository.save(question);
            log.info("Saved AI-generated question with ID: {}", saved.getId());
            return saved.getId();
        } catch (Exception e) {
            log.error("Failed to save question to database", e);
            throw new RuntimeException("Failed to save question", e);
        }
    }

    private List<QuestionDTO> generateFallbackQuestions(Job job, ExamAttempt.DifficultyLevel difficulty) {
        log.warn("Using fallback questions from database for job: {}", job.getTitle());
        List<QuestionDTO> questions = new ArrayList<>();

        try {
            // Extract skills from job
            String[] skills = job.getSkillsRequired() != null ? 
                job.getSkillsRequired().split(",") : new String[]{"General"};
            String primarySkill = skills[0].trim();

            // Convert difficulty level
            Question.DifficultyLevel dbDifficulty;
            switch (difficulty) {
                case EASY -> dbDifficulty = Question.DifficultyLevel.EASY;
                case HARD -> dbDifficulty = Question.DifficultyLevel.HARD;
                case ADVANCED -> dbDifficulty = Question.DifficultyLevel.ADVANCED;
                default -> dbDifficulty = Question.DifficultyLevel.MEDIUM;
            }

            // Fetch MCQ questions from database
            List<Question> mcqQuestions = questionRepository
                .findRandomQuestionsBySkillAndLevelAndType(primarySkill, dbDifficulty.name(), Question.QuestionType.MCQ.name(), PageRequest.of(0, MCQ_COUNT));
            
            for (Question q : mcqQuestions) {
                questions.add(convertToDTO(q));
            }

            // Fetch Coding questions
            List<Question> codingQuestions = questionRepository
                .findRandomQuestionsBySkillAndLevelAndType(primarySkill, dbDifficulty.name(), Question.QuestionType.CODING.name(), PageRequest.of(0, CODING_COUNT));
            
            for (Question q : codingQuestions) {
                questions.add(convertToDTO(q));
            }

            // Fetch Scenario questions
            List<Question> scenarioQuestions = questionRepository
                .findRandomQuestionsBySkillAndLevelAndType(primarySkill, dbDifficulty.name(), Question.QuestionType.SCENARIO.name(), PageRequest.of(0, SCENARIO_COUNT));
            
            for (Question q : scenarioQuestions) {
                questions.add(convertToDTO(q));
            }

            // If not enough questions, fetch any available
            if (questions.size() < (MCQ_COUNT + CODING_COUNT + SCENARIO_COUNT)) {
                log.warn("Not enough skill-specific questions, fetching general questions");
                List<Question> generalQuestions = questionRepository
                    .findRandomQuestionsByLevelAndType(dbDifficulty.name(), Question.QuestionType.MCQ.name(), PageRequest.of(0, MCQ_COUNT));
                
                for (Question q : generalQuestions) {
                    if (questions.size() >= (MCQ_COUNT + CODING_COUNT + SCENARIO_COUNT)) break;
                    questions.add(convertToDTO(q));
                }
            }

        } catch (Exception e) {
            log.error("Fallback question generation failed", e);
            // Last resort: generate basic placeholder questions
            for (int i = 0; i < MCQ_COUNT; i++) {
                questions.add(QuestionDTO.builder()
                        .id((long) i + 1)
                        .skill("General")
                        .level(difficulty.name())
                        .type(Question.QuestionType.MCQ.name())
                        .question("Sample question #" + (i + 1) + " about " + job.getTitle())
                        .options(List.of("Option A", "Option B", "Option C", "Option D"))
                        .points(10)
                        .timeLimit(120)
                        .build());
            }
        }

        return questions;
    }

    private QuestionDTO convertToDTO(Question question) {
        return QuestionDTO.builder()
                .id(question.getId())
                .skill(question.getSkill())
                .level(question.getLevel().name())
                .type(question.getType().name())
                .question(question.getQuestion())
                .codeSnippet(question.getCodeSnippet())
                .options(parseOptions(question.getOptions()))
                .testCases(parseTestCases(question.getTestCases()))
                .points(question.getPoints())
                .timeLimit(question.getTimeLimit())
                .build();
    }

    private List<String> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isEmpty()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(optionsJson);
            List<String> options = new ArrayList<>();
            if (node.isArray()) {
                node.forEach(n -> options.add(n.asText()));
            }
            return options;
        } catch (Exception e) {
            log.error("Failed to parse options JSON", e);
            return List.of();
        }
    }
    
    private List<QuestionDTO.TestCaseDTO> parseTestCases(String testCasesJson) {
        if (testCasesJson == null || testCasesJson.isEmpty()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(testCasesJson);
            List<QuestionDTO.TestCaseDTO> testCases = new ArrayList<>();
            if (node.isArray()) {
                for (JsonNode tc : node) {
                    testCases.add(QuestionDTO.TestCaseDTO.builder()
                            .input(tc.path("input").asText())
                            .expectedOutput(tc.path("expectedOutput").asText())
                            .build());
                }
            }
            return testCases;
        } catch (Exception e) {
            log.error("Failed to parse test cases JSON", e);
            return List.of();
        }
    }

    /**
     * Generate exam questions using Codeforces problems for coding section
     */
    private List<QuestionDTO> generateCodeforcesQuestions(Job job, ExamAttempt.DifficultyLevel difficulty) {
        List<QuestionDTO> questions = new ArrayList<>();
        
        try {
            // Get required skills for MCQ/Scenario questions
            String[] skills = job.getSkillsRequired().split(",");
            String primarySkill = skills.length > 0 ? skills[0].trim() : "General";
            
            // Generate MCQ questions from database or AI
            List<QuestionDTO> mcqQuestions = getQuestionsFromDatabase(
                primarySkill, 
                difficulty, 
                Question.QuestionType.MCQ, 
                MCQ_COUNT
            );
            
            if (mcqQuestions.isEmpty()) {
                log.info("No MCQ questions in database, generating with AI");
                mcqQuestions = generateAIQuestions(job, difficulty).stream()
                    .filter(q -> "MCQ".equals(q.getType()))
                    .limit(MCQ_COUNT)
                    .collect(Collectors.toList());
            }
            questions.addAll(mcqQuestions);
            
            // Fetch coding questions from Codeforces
            log.info("Fetching coding questions from Codeforces");
            List<QuestionDTO> codingQuestions = codeforcesService.fetchCodeforcesProblems(
                primarySkill, 
                difficulty, 
                CODING_COUNT
            );
            
            // Save Codeforces questions to database and assign IDs
            for (QuestionDTO q : codingQuestions) {
                Long questionId = saveQuestionToDatabase(q, null, null, 
                    q.getTestCases() != null ? objectMapper.writeValueAsString(q.getTestCases()) : null);
                q.setId(questionId);
            }
            questions.addAll(codingQuestions);
            
            // Generate scenario question
            List<QuestionDTO> scenarioQuestions = getQuestionsFromDatabase(
                primarySkill, 
                difficulty, 
                Question.QuestionType.SCENARIO, 
                SCENARIO_COUNT
            );
            
            if (scenarioQuestions.isEmpty()) {
                log.info("No scenario questions in database, generating with AI");
                scenarioQuestions = generateAIQuestions(job, difficulty).stream()
                    .filter(q -> "SCENARIO".equals(q.getType()))
                    .limit(SCENARIO_COUNT)
                    .collect(Collectors.toList());
            }
            questions.addAll(scenarioQuestions);
            
            log.info("Successfully generated exam with {} Codeforces coding problems", codingQuestions.size());
            
        } catch (Exception e) {
            log.error("Codeforces question generation failed, using fallback AI questions", e);
            questions = generateAIQuestions(job, difficulty);
        }
        
        return questions;
    }
    
    /**
     * Get questions from database by criteria
     */
    private List<QuestionDTO> getQuestionsFromDatabase(String skill, ExamAttempt.DifficultyLevel difficulty, 
                                                       Question.QuestionType type, int limit) {
        try {
            // Convert ExamAttempt.DifficultyLevel to Question.DifficultyLevel
            Question.DifficultyLevel questionDifficulty = Question.DifficultyLevel.valueOf(difficulty.name());
            
            List<Question> dbQuestions = questionRepository
                .findBySkillAndLevelAndTypeAndIsActiveTrue(skill, questionDifficulty, type);
            
            // Limit the results
            return dbQuestions.stream()
                .limit(limit)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to fetch questions from database", e);
            return new ArrayList<>();
        }
    }

    @Transactional
    public void startExam(Long examAttemptId, String ipAddress) {
        ExamAttempt exam = examAttemptRepository.findById(examAttemptId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        exam.setStatus(ExamAttempt.ExamStatus.IN_PROGRESS);
        exam.setStartedAt(LocalDateTime.now());
        exam.setIpAddress(ipAddress);
        examAttemptRepository.save(exam);
    }

    public ExamAttempt getExamBySessionToken(String sessionToken) {
        return examAttemptRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new RuntimeException("Invalid session token"));
    }

    @Transactional(readOnly = true)
    public List<ExamAttempt> getExamAttemptsByCandidate(Long candidateId) {
        List<ExamAttempt> attempts = examAttemptRepository.findByCandidate_IdOrderByCreatedAtDesc(candidateId);
        // Initialize lazy relationships
        attempts.forEach(attempt -> {
            if (attempt.getCandidate() != null) {
                attempt.getCandidate().getFirstName();
            }
            if (attempt.getJob() != null) {
                attempt.getJob().getTitle();
            }
            if (attempt.getApplication() != null) {
                attempt.getApplication().getId();
            }
        });
        return attempts;
    }

    @Transactional
    public ExamGenerationResponse getExamDetails(Long examAttemptId) {
        ExamAttempt exam = examAttemptRepository.findById(examAttemptId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));
        
        // Initialize lazy relationships
        if (exam.getCandidate() != null) {
            exam.getCandidate().getFirstName();
        }
        if (exam.getJob() != null) {
            exam.getJob().getTitle();
        }
        
        // If exam is NOT_STARTED, mark it as IN_PROGRESS
        if (exam.getStatus() == ExamAttempt.ExamStatus.NOT_STARTED) {
            exam.setStatus(ExamAttempt.ExamStatus.IN_PROGRESS);
            exam.setStartedAt(LocalDateTime.now());
            examAttemptRepository.save(exam);
            log.info("Exam {} status changed from NOT_STARTED to IN_PROGRESS", examAttemptId);
        }
        
        List<QuestionDTO> questionDTOs = new ArrayList<>();
        
        // Try to get questions from exam answers (if candidate has started answering)
        List<ExamAnswer> examAnswers = examAnswerRepository.findByExamAttemptIdOrderById(examAttemptId);
        
        if (!examAnswers.isEmpty()) {
            // If there are answers, get questions from them
            log.info("Loading {} questions from existing exam answers for exam {}", examAnswers.size(), examAttemptId);
            
            questionDTOs = examAnswers.stream()
                    .map(answer -> {
                        Question q = answer.getQuestion();
                        // Initialize lazy loading
                        q.getId();
                        return QuestionDTO.builder()
                                .id(q.getId())
                                .skill(q.getSkill())
                                .level(q.getLevel().name())
                                .type(q.getType().name())
                                .question(q.getQuestion())
                                .codeSnippet(q.getCodeSnippet())
                                .options(parseOptions(q.getOptions()))
                                .points(q.getPoints())
                                .timeLimit(q.getTimeLimit())
                                .build();
                    })
                    .collect(Collectors.toList());
        } else {
            // If no answers yet, regenerate questions for this exam
            // This happens when resuming a NOT_STARTED exam or IN_PROGRESS with no answers
            log.info("No existing answers found, regenerating questions for exam {}", examAttemptId);
            questionDTOs = generateAIQuestions(exam.getJob(), exam.getCurrentDifficulty());
        }
        
        return ExamGenerationResponse.builder()
                .examAttemptId(exam.getId())
                .sessionToken(exam.getSessionToken())
                .totalQuestions(exam.getTotalQuestions())
                .timeLimit(90)
                .difficulty(exam.getCurrentDifficulty().name())
                .message("Exam loaded successfully")
                .questions(questionDTOs)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ExamAttempt> getAllExamAttempts() {
        List<ExamAttempt> attempts = examAttemptRepository.findAll();
        // Initialize lazy relationships
        attempts.forEach(attempt -> {
            if (attempt.getCandidate() != null) {
                attempt.getCandidate().getFirstName();
            }
            if (attempt.getJob() != null) {
                attempt.getJob().getTitle();
            }
            if (attempt.getApplication() != null) {
                attempt.getApplication().getId();
            }
        });
        return attempts;
    }

    @Transactional(readOnly = true)
    public GenerateSamplesResponse generateSamples(Long questionId, String language, String code) {
        log.info("Generating samples for question ID: {}, language: {}", questionId, language);
        
        try {
            // Fetch the question
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new RuntimeException("Question not found with ID: " + questionId));
            
            // Parse existing test cases from the question
            List<GenerateSamplesResponse.TestCaseSample> samples = new ArrayList<>();
            
            if (question.getTestCases() != null && !question.getTestCases().trim().isEmpty()) {
                try {
                    JsonNode testCasesNode = objectMapper.readTree(question.getTestCases());
                    
                    if (testCasesNode.isArray()) {
                        for (JsonNode testCaseNode : testCasesNode) {
                            String input = testCaseNode.has("input") ? testCaseNode.get("input").asText() : "";
                            String expectedOutput = testCaseNode.has("expectedOutput") ? 
                                testCaseNode.get("expectedOutput").asText() : "";
                            
                            samples.add(GenerateSamplesResponse.TestCaseSample.builder()
                                    .input(input)
                                    .expectedOutput(expectedOutput)
                                    .build());
                        }
                    }
                } catch (Exception e) {
                    log.error("Error parsing test cases: " + e.getMessage(), e);
                }
            }
            
            // If no test cases found, generate some basic ones based on the problem
            if (samples.isEmpty()) {
                samples = generateDefaultSamples(question, language);
            }
            
            return GenerateSamplesResponse.builder()
                    .samples(samples)
                    .message("Generated " + samples.size() + " sample test case(s)")
                    .build();
                    
        } catch (Exception e) {
            log.error("Error generating samples: " + e.getMessage(), e);
            throw new RuntimeException("Failed to generate samples: " + e.getMessage());
        }
    }
    
    private List<GenerateSamplesResponse.TestCaseSample> generateDefaultSamples(Question question, String language) {
        // Generate 3 basic sample test cases as a fallback
        List<GenerateSamplesResponse.TestCaseSample> samples = new ArrayList<>();
        
        // Basic examples - these would ideally be AI-generated based on the question
        samples.add(GenerateSamplesResponse.TestCaseSample.builder()
                .input("5")
                .expectedOutput("5")
                .build());
                
        samples.add(GenerateSamplesResponse.TestCaseSample.builder()
                .input("10")
                .expectedOutput("10")
                .build());
                
        samples.add(GenerateSamplesResponse.TestCaseSample.builder()
                .input("1")
                .expectedOutput("1")
                .build());
        
        return samples;
    }
}
