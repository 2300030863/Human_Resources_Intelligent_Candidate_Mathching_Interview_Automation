package com.lernathon.recruitment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchingResponse {
    private Long candidateId;
    private String candidateName;
    private String email;
    private Double matchScore;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private Integer experienceYears;
    /** Experience years counting only VERIFIED / LIKELY_VALID companies */
    private Integer verifiedExperienceYears;
    private String status;
    /** Raw JSON from Flask /upload-and-verify; parsed on the frontend to show per-company VERIFIED/FAKE badges */
    private String employmentVerificationJson;
    /** True when at least one company in the resume is SUSPICIOUS (FAKE) */
    private Boolean experienceSuspect;
}
