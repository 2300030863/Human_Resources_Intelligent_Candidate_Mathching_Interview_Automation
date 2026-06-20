package com.lernathon.recruitment.dto;

import com.lernathon.recruitment.entity.Interview;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for submitting interview results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewResultDTO {
    
    private Interview.InterviewStatus status; // COMPLETED, NO_SHOW, etc.
    
    private String feedback; // Detailed interview feedback
    
    private Integer rating; // Interview rating (1-5 or 1-10)
    
    private String notes; // Additional notes or observations
    
    private Boolean passed; // Did the candidate pass the interview?
    
    private String strengths; // Candidate strengths observed
    
    private String weaknesses; // Areas for improvement
    
    private String recommendation; // HIRE, REJECT, NEXT_ROUND, etc.
}
