package com.lernathon.recruitment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeParserService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${ai.matching.service.url:http://localhost:5000}")
    private String aiServiceUrl;
    
    @Value("${ai.matching.service.enabled:true}")
    private boolean aiServiceEnabled;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}");
    private static final Pattern EXPERIENCE_PATTERN = Pattern.compile("(\\d+)\\+?\\s*(years?|yrs?)");
    private static final Pattern YEAR_RANGE_PATTERN = Pattern.compile(
        "\\b((?:19|20)\\d{2})\\s*[-\u2013\u2014]\\s*((?:19|20)\\d{2}|present|current|now)",
        Pattern.CASE_INSENSITIVE);

    public ParsedResume parseResume(MultipartFile file) throws IOException {
        String content = extractText(file);
        return parseContent(content);
    }

    public ParsedResume parseContent(String content) {
        ParsedResume resume = new ParsedResume();
        
        // Extract email
        Matcher emailMatcher = EMAIL_PATTERN.matcher(content);
        if (emailMatcher.find()) {
            resume.setEmail(emailMatcher.group());
        }

        // Extract phone
        Matcher phoneMatcher = PHONE_PATTERN.matcher(content);
        if (phoneMatcher.find()) {
            resume.setPhone(phoneMatcher.group());
        }

        // Extract experience years - explicit "X years" pattern first
        Matcher expMatcher = EXPERIENCE_PATTERN.matcher(content.toLowerCase());
        if (expMatcher.find()) {
            try {
                resume.setExperienceYears(Integer.parseInt(expMatcher.group(1)));
            } catch (NumberFormatException e) {
                log.warn("Failed to parse experience years: {}", expMatcher.group(1));
            }
        }
        // Fallback: sum up date ranges like "2019 - Present" or "2020 - 2023"
        if (resume.getExperienceYears() == null || resume.getExperienceYears() == 0) {
            int calcYears = calculateExperienceFromDateRanges(content);
            if (calcYears > 0) {
                resume.setExperienceYears(calcYears);
            }
        }

        // Extract current/most recent company
        resume.setCurrentCompany(extractCurrentCompany(content));

        // Extract education
        resume.setEducation(extractEducation(content));

        // Extract skills
        resume.setSkills(extractSkills(content));

        // Extract name (first two lines or first capitalized words)
        resume.setName(extractName(content));

        resume.setRawContent(content);

        return resume;
    }

    private String extractText(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType != null && contentType.contains("pdf")) {
            return extractPdfText(file);
        } else {
            // For text files or fallback
            return new String(file.getBytes());
        }
    }

    private String extractPdfText(MultipartFile file) throws IOException {
        try {
            // Use Apache PDFBox to extract text from PDF
            org.apache.pdfbox.pdmodel.PDDocument document = 
                org.apache.pdfbox.Loader.loadPDF(file.getBytes());
            
            org.apache.pdfbox.text.PDFTextStripper stripper = 
                new org.apache.pdfbox.text.PDFTextStripper();
            
            String text = stripper.getText(document);
            document.close();
            
            log.info("Successfully extracted {} characters from PDF", text.length());
            return text;
        } catch (Exception e) {
            log.error("Failed to extract text from PDF: {}", e.getMessage());
            // Fallback to treating as text file
            return new String(file.getBytes());
        }
    }

    private int calculateExperienceFromDateRanges(String content) {
        Matcher m = YEAR_RANGE_PATTERN.matcher(content);
        java.util.Set<String> seen = new java.util.HashSet<>();
        int totalMonths = 0;
        while (m.find()) {
            String range = m.group().trim();
            if (seen.add(range)) {
                try {
                    int startYear = Integer.parseInt(m.group(1));
                    String endPart = m.group(2).toLowerCase().trim();
                    int endYear;
                    if (endPart.equals("present") || endPart.equals("current") || endPart.equals("now")) {
                        endYear = java.time.LocalDate.now().getYear();
                    } else {
                        endYear = Integer.parseInt(endPart);
                    }
                    if (endYear >= startYear && endYear <= java.time.LocalDate.now().getYear() + 1) {
                        totalMonths += (endYear - startYear) * 12;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return totalMonths / 12;
    }

    private String extractCurrentCompany(String content) {
        String[] lines = content.split("\\r?\\n");
        java.util.regex.Pattern presentPattern = java.util.regex.Pattern.compile(
            "(?i)\\b(present|current|now|2024|2025|2026)\\b");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (presentPattern.matcher(line).find()) {
                // Search upward for a plausible company name line
                for (int j = i - 1; j >= Math.max(0, i - 5); j--) {
                    String candidate = lines[j].trim();
                    if (candidate.length() < 3 || candidate.length() > 70) continue;
                    if (candidate.contains("@")) continue;
                    // Skip date-only lines
                    if (candidate.matches(".*\\b(19|20)\\d{2}\\b.*")) continue;
                    // Skip lines with special chars that indicate non-company content
                    if (candidate.matches(".*[|#$%^&*+={}\\[\\]<>?/\\\\].*")) continue;
                    // Skip skill/tool lists: lines containing a colon (e.g. "Tools: Git, ...")
                    if (candidate.contains(":")) continue;
                    // Skip lines that look like comma-separated lists (3+ commas)
                    if (candidate.chars().filter(c -> c == ',').count() >= 3) continue;
                    // Skip lines that are resume section headers
                    if (candidate.toLowerCase().matches(
                            ".*(experience|education|skills|summary|profile|"
                            + "objective|work history|employment|projects|"
                            + "certification|languages|references|internship|"
                            + "tools|frameworks|technologies|responsibilities).*")) continue;
                    // Must start with an uppercase letter (company name)
                    if (!Character.isUpperCase(candidate.charAt(0))) continue;
                    return candidate;
                }
            }
        }
        return null;
    }

    private String extractName(String content) {
        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && line.length() < 50 && !line.contains("@")) {
                return line;
            }
        }
        return "";
    }

    private String extractEducation(String content) {
        List<String> educationKeywords = Arrays.asList(
            "Bachelor", "Master", "PhD", "B.Tech", "M.Tech", "MBA", 
            "B.E", "M.E", "B.Sc", "M.Sc", "Diploma", "University", "College"
        );

        StringBuilder education = new StringBuilder();
        String[] lines = content.split("\\r?\\n");
        
        for (String line : lines) {
            for (String keyword : educationKeywords) {
                if (line.contains(keyword)) {
                    education.append(line.trim()).append("; ");
                    break;
                }
            }
        }

        return education.toString();
    }

    private List<String> extractSkills(String content) {
        // Try AI service first if enabled
        if (aiServiceEnabled) {
            try {
                List<String> aiSkills = extractSkillsUsingAI(content);
                if (aiSkills != null && !aiSkills.isEmpty()) {
                    log.info("Extracted {} skills using AI service", aiSkills.size());
                    return aiSkills;
                }
            } catch (Exception e) {
                log.warn("AI skill extraction failed, falling back to keyword matching: {}", e.getMessage());
            }
        }
        
        // Fallback to keyword matching
        return extractSkillsFallback(content);
    }
    
    private List<String> extractSkillsUsingAI(String content) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("text", content);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            String url = aiServiceUrl + "/extract-skills";
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            JsonNode skillsNode = jsonNode.get("skills");
            
            List<String> skills = new ArrayList<>();
            if (skillsNode != null && skillsNode.isArray()) {
                skillsNode.forEach(skill -> skills.add(skill.asText()));
            }
            
            log.info("AI extracted skills: {}", skills);
            return skills;
        } catch (Exception e) {
            log.error("Error calling AI skill extraction service: {}", e.getMessage());
            return null;
        }
    }
    
    private List<String> extractSkillsFallback(String content) {
        List<String> commonSkills = Arrays.asList(
            // Programming Languages
            "Java", "Python", "JavaScript", "TypeScript", "C++", "C#", "PHP", "Ruby", "Go", "Rust", "Swift", "Kotlin",
            // Frontend
            "React", "Angular", "Vue", "HTML", "CSS", "Redux", "Next.js", "Tailwind",
            // Backend
            "Spring Boot", "Node.js", "Express", "Django", "Flask", "FastAPI", "REST API", "GraphQL",
            // Databases
            "MySQL", "PostgreSQL", "MongoDB", "Redis", "Oracle", "SQL Server", "Cassandra",
            // Cloud
            "AWS", "Azure", "GCP", "Docker", "Kubernetes", "CI/CD", "Jenkins", "GitLab",
            // Tools
            "Git", "Jira", "Postman", "Maven", "Gradle", "npm", "Webpack",
            // Other
            "Machine Learning", "AI", "Data Science", "TensorFlow", "PyTorch", "Pandas", "NumPy",
            "Microservices", "Agile", "Scrum", "DevOps", "Testing", "JUnit", "Jest"
        );

        List<String> foundSkills = new ArrayList<>();
        String contentLower = content.toLowerCase();

        for (String skill : commonSkills) {
            if (contentLower.contains(skill.toLowerCase())) {
                foundSkills.add(skill);
            }
        }

        return foundSkills;
    }

    public static class ParsedResume {
        private String name;
        private String email;
        private String phone;
        private Integer experienceYears;
        private String education;
        private List<String> skills = new ArrayList<>();
        private String rawContent;
        private String currentCompany;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public Integer getExperienceYears() { return experienceYears; }
        public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }
        public String getEducation() { return education; }
        public void setEducation(String education) { this.education = education; }
        public List<String> getSkills() { return skills; }
        public void setSkills(List<String> skills) { this.skills = skills; }
        public String getRawContent() { return rawContent; }
        public void setRawContent(String rawContent) { this.rawContent = rawContent; }
        public String getCurrentCompany() { return currentCompany; }
        public void setCurrentCompany(String currentCompany) { this.currentCompany = currentCompany; }
    }
}
