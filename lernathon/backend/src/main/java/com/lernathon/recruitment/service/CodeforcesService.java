package com.lernathon.recruitment.service;

import com.lernathon.recruitment.dto.CodeforcesResponse;
import com.lernathon.recruitment.dto.QuestionDTO;
import com.lernathon.recruitment.entity.ExamAttempt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CodeforcesService {
    
    private static final String CODEFORCES_API_URL = "https://codeforces.com/api/problemset.problems";
    private final RestTemplate restTemplate = new RestTemplate();
    private final Random random = new Random();
    
    /**
     * Fetch coding problems from Codeforces API
     * @param skill Programming language (Java, Python, JavaScript)
     * @param difficulty EASY, MEDIUM, HARD
     * @param count Number of problems to fetch
     * @return List of QuestionDTOs
     */
    public List<QuestionDTO> fetchCodeforcesProblems(String skill, ExamAttempt.DifficultyLevel difficulty, int count) {
        try {
            log.info("Fetching {} {} problems from Codeforces for {}", count, difficulty, skill);
            
            // Fetch problems from Codeforces API
            String url = CODEFORCES_API_URL + "?tags=implementation";
            CodeforcesResponse response = restTemplate.getForObject(url, CodeforcesResponse.class);
            
            if (response == null || !response.getStatus().equals("OK")) {
                log.error("Failed to fetch problems from Codeforces");
                return new ArrayList<>();
            }
            
            List<CodeforcesResponse.Problem> problems = response.getResult().getProblems();
            
            // Filter problems by difficulty rating
            int minRating = getDifficultyRatingMin(difficulty);
            int maxRating = getDifficultyRatingMax(difficulty);
            
            List<CodeforcesResponse.Problem> filteredProblems = problems.stream()
                    .filter(p -> p.getRating() != null)
                    .filter(p -> p.getRating() >= minRating && p.getRating() <= maxRating)
                    .filter(p -> p.getType().equals("PROGRAMMING"))
                    .collect(Collectors.toList());
            
            // Randomly select problems
            List<QuestionDTO> questions = new ArrayList<>();
            int availableCount = Math.min(count, filteredProblems.size());
            
            for (int i = 0; i < availableCount; i++) {
                int randomIndex = random.nextInt(filteredProblems.size());
                CodeforcesResponse.Problem problem = filteredProblems.remove(randomIndex);
                
                QuestionDTO questionDTO = convertToQuestionDTO(problem, skill, difficulty);
                questions.add(questionDTO);
            }
            
            log.info("Successfully fetched {} Codeforces problems", questions.size());
            return questions;
            
        } catch (Exception e) {
            log.error("Error fetching Codeforces problems: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Convert Codeforces problem to QuestionDTO
     */
    private QuestionDTO convertToQuestionDTO(CodeforcesResponse.Problem problem, String skill, ExamAttempt.DifficultyLevel difficulty) {
        // Build the question description
        StringBuilder questionText = new StringBuilder();
        questionText.append("**").append(problem.getName()).append("**\n\n");
        questionText.append("**Problem ID:** ").append(problem.getContestId()).append(problem.getIndex()).append("\n");
        questionText.append("**Difficulty Rating:** ").append(problem.getRating()).append("\n");
        
        if (problem.getTags() != null && !problem.getTags().isEmpty()) {
            questionText.append("**Tags:** ").append(String.join(", ", problem.getTags())).append("\n");
        }
        
        questionText.append("\n**Task:**\n");
        questionText.append("Solve this problem from Codeforces.\n\n");
        questionText.append("Visit the full problem at: https://codeforces.com/problemset/problem/");
        questionText.append(problem.getContestId()).append("/").append(problem.getIndex()).append("\n\n");
        
        questionText.append("**Instructions:**\n");
        questionText.append("1. Read the problem statement on Codeforces\n");
        questionText.append("2. Implement your solution in ").append(skill).append("\n");
        questionText.append("3. Test your code with the provided examples\n");
        questionText.append("4. Submit your solution for evaluation\n\n");
        
        questionText.append("**Note:** You can visit the Codeforces link above to see the full problem description, ");
        questionText.append("input/output format, examples, and constraints.");
        
        // Generate code snippet template
        String codeSnippet = generateCodeTemplate(skill, problem.getName());
        
        // Create sample test cases (placeholders - user should refer to Codeforces)
        List<QuestionDTO.TestCaseDTO> testCases = new ArrayList<>();
        testCases.add(QuestionDTO.TestCaseDTO.builder()
                .input("See problem on Codeforces")
                .expectedOutput("See problem on Codeforces")
                .build());
        
        return QuestionDTO.builder()
                .skill(skill)
                .level(difficulty.name())
                .type("CODING")
                .question(questionText.toString())
                .codeSnippet(codeSnippet)
                .testCases(testCases)
                .points(getPointsByDifficulty(difficulty))
                .timeLimit(900) // 15 minutes
                .build();
    }
    
    /**
     * Generate code template based on language
     */
    private String generateCodeTemplate(String skill, String problemName) {
        String lowerSkill = skill.toLowerCase();
        
        if (lowerSkill.contains("java")) {
            return "import java.util.*;\n\n" +
                   "public class Solution {\n" +
                   "    public static void main(String[] args) {\n" +
                   "        Scanner sc = new Scanner(System.in);\n" +
                   "        // Read input\n" +
                   "        \n" +
                   "        // Solve problem: " + problemName + "\n" +
                   "        \n" +
                   "        // Print output\n" +
                   "        \n" +
                   "        sc.close();\n" +
                   "    }\n" +
                   "}";
        } else if (lowerSkill.contains("python")) {
            return "# Problem: " + problemName + "\n\n" +
                   "def solve():\n" +
                   "    # Read input\n" +
                   "    \n" +
                   "    # Solve problem\n" +
                   "    \n" +
                   "    # Print output\n" +
                   "    pass\n\n" +
                   "if __name__ == '__main__':\n" +
                   "    solve()";
        } else {
            return "// Problem: " + problemName + "\n\n" +
                   "function solve() {\n" +
                   "    // Read input\n" +
                   "    \n" +
                   "    // Solve problem\n" +
                   "    \n" +
                   "    // Print output\n" +
                   "}\n\n" +
                   "solve();";
        }
    }
    
    /**
     * Map difficulty level to Codeforces rating range
     */
    private int getDifficultyRatingMin(ExamAttempt.DifficultyLevel difficulty) {
        return switch (difficulty) {
            case EASY -> 800;
            case MEDIUM -> 1200;
            case HARD -> 1600;
            default -> 1200; // Default to MEDIUM
        };
    }
    
    private int getDifficultyRatingMax(ExamAttempt.DifficultyLevel difficulty) {
        return switch (difficulty) {
            case EASY -> 1199;
            case MEDIUM -> 1599;
            case HARD -> 2400;
            default -> 1599; // Default to MEDIUM
        };
    }
    
    private int getPointsByDifficulty(ExamAttempt.DifficultyLevel difficulty) {
        return switch (difficulty) {
            case EASY -> 15;
            case MEDIUM -> 20;
            case HARD -> 25;
            default -> 20; // Default to MEDIUM
        };
    }
}
