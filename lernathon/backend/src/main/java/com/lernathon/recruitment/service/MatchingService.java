package com.lernathon.recruitment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lernathon.recruitment.entity.Application;
import com.lernathon.recruitment.entity.Candidate;
import com.lernathon.recruitment.entity.Job;
import com.lernathon.recruitment.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingService {

    private final ApplicationRepository applicationRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${ai.matching.service.url:http://localhost:5000}")
    private String aiServiceUrl;
    
    @Value("${ai.matching.service.enabled:false}")
    private boolean aiServiceEnabled;

    public double calculateMatchScore(Candidate candidate, Job job) {
        double skillScore = calculateSkillMatch(candidate.getSkills(), job.getSkillsRequired());
        // Use only verified experience (deduct fake company years)
        int verifiedExp = calculateVerifiedExperienceYears(candidate);
        double experienceScore = calculateExperienceMatch(verifiedExp, job.getExperienceRequired());
        double locationScore = calculateLocationMatch(candidate.getLocation(), job.getLocation());

        // Weighted average
        double totalScore = (skillScore * 0.6) + (experienceScore * 0.3) + (locationScore * 0.1);

        return Math.round(totalScore * 100.0) / 100.0;
    }

    /**
     * Returns experience years counting only VERIFIED / LIKELY_VALID companies.
     * SUSPICIOUS (FAKE) company years are deducted from the total.
     */
    public int calculateVerifiedExperienceYears(Candidate candidate) {
        int total = candidate.getExperienceYears() != null ? candidate.getExperienceYears() : 0;
        String json = candidate.getEmploymentVerificationJson();
        if (json == null || json.isBlank()) return total;

        try {
            com.fasterxml.jackson.databind.JsonNode arr = objectMapper.readTree(json);
            if (!arr.isArray()) return total;
            double fakeYears = 0;
            for (com.fasterxml.jackson.databind.JsonNode node : arr) {
                String status = node.path("verification_status").asText("");
                if ("SUSPICIOUS".equals(status)) {
                    fakeYears += node.path("duration_years").asDouble(0);
                }
            }
            int verified = (int) Math.max(0, total - fakeYears);
            log.info("Candidate {} – total exp: {} yrs, fake: {} yrs, verified: {} yrs",
                    candidate.getId(), total, fakeYears, verified);
            return verified;
        } catch (Exception e) {
            log.warn("Could not parse employmentVerificationJson for candidate {}: {}",
                    candidate.getId(), e.getMessage());
            return total;
        }
    }

    private double calculateSkillMatch(String candidateSkills, String requiredSkills) {
        if (candidateSkills == null || requiredSkills == null) {
            return 0.0;
        }

        // Try AI matching service first if enabled
        if (aiServiceEnabled) {
            try {
                Double aiScore = callAIMatchingService(candidateSkills, requiredSkills);
                if (aiScore != null) {
                    log.info("AI Matching Score: {}", aiScore);
                    return aiScore;
                }
            } catch (Exception e) {
                log.warn("AI Matching Service failed, falling back to keyword matching: {}", e.getMessage());
            }
        }

        // Fallback to keyword matching
        Set<String> candidateSkillSet = extractSkills(candidateSkills);
        Set<String> requiredSkillSet = extractSkills(requiredSkills);

        if (requiredSkillSet.isEmpty()) {
            return 50.0; // Neutral score if no requirements specified
        }

        long matchedSkills = requiredSkillSet.stream()
            .filter(candidateSkillSet::contains)
            .count();

        return (matchedSkills * 100.0) / requiredSkillSet.size();
    }
    
    private Double callAIMatchingService(String candidateSkills, String requiredSkills) {
        try {
            // Prepare request
            Map<String, Object> request = new HashMap<>();
            request.put("job_skills", Arrays.asList(requiredSkills.split("[,;\\n]")));
            request.put("candidate_skills", Arrays.asList(candidateSkills.split("[,;\\n]")));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            // Call AI service
            String url = aiServiceUrl + "/match-skills";
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            // Parse response
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            double matchPercentage = jsonNode.get("match_percentage").asDouble();
            
            log.info("AI Match Percentage: {}%", matchPercentage);
            log.info("Matched Skills: {}", jsonNode.get("matched_skills"));
            log.info("Missing Skills: {}", jsonNode.get("missing_skills"));
            
            return matchPercentage;
        } catch (Exception e) {
            log.error("Error calling AI Matching Service: {}", e.getMessage());
            return null;
        }
    }

    private double calculateExperienceMatch(Integer candidateExp, Integer requiredExp) {
        if (candidateExp == null || requiredExp == null) {
            return 50.0;
        }

        if (candidateExp >= requiredExp) {
            // Exact or more experience
            int diff = candidateExp - requiredExp;
            if (diff <= 2) {
                return 100.0; // Perfect match
            } else if (diff <= 5) {
                return 90.0; // Slightly overqualified
            } else {
                return 75.0; // Very overqualified
            }
        } else {
            // Less experience
            int diff = requiredExp - candidateExp;
            if (diff == 1) {
                return 80.0;
            } else if (diff == 2) {
                return 60.0;
            } else if (diff <= 3) {
                return 40.0;
            } else {
                return 20.0;
            }
        }
    }

    private double calculateLocationMatch(String candidateLocation, String jobLocation) {
        if (candidateLocation == null || jobLocation == null) {
            return 50.0;
        }

        String candLoc = candidateLocation.toLowerCase().trim();
        String jobLoc = jobLocation.toLowerCase().trim();

        if (candLoc.equals(jobLoc)) {
            return 100.0;
        } else if (candLoc.contains(jobLoc) || jobLoc.contains(candLoc)) {
            return 75.0;
        } else if (jobLoc.contains("remote") || candLoc.contains("remote")) {
            return 100.0;
        } else {
            return 30.0;
        }
    }

    private Set<String> extractSkills(String skillsText) {
        if (skillsText == null || skillsText.trim().isEmpty()) {
            return new HashSet<>();
        }

        return Arrays.stream(skillsText.split("[,;\\n]"))
            .map(String::trim)
            .map(String::toLowerCase)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
    }

    @Transactional
    public List<MatchResult> findMatchingCandidates(Job job, List<Candidate> candidates) {
        return candidates.stream()
            .map(candidate -> {
                double score = calculateMatchScore(candidate, job);
                return new MatchResult(candidate, score);
            })
            .sorted(Comparator.comparing(MatchResult::getScore).reversed())
            .collect(Collectors.toList());
    }

    @Transactional
    public void updateApplicationMatchScore(Application application) {
        double score = calculateMatchScore(application.getCandidate(), application.getJob());
        application.setMatchScore(score);
        applicationRepository.save(application);
    }

    public static class MatchResult {
        private Candidate candidate;
        private double score;

        public MatchResult(Candidate candidate, double score) {
            this.candidate = candidate;
            this.score = score;
        }

        public Candidate getCandidate() { return candidate; }
        public void setCandidate(Candidate candidate) { this.candidate = candidate; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
    }
}
