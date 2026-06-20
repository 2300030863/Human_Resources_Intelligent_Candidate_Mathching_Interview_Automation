package com.lernathon.recruitment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheatEventResponse {
    private Integer currentCheatingScore;
    private Integer warningsRemaining;
    private Boolean examTerminated;
    private String message;
}
