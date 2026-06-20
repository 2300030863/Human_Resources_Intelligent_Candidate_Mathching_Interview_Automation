package com.lernathon.recruitment.dto;

import com.lernathon.recruitment.entity.Application;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateStatusRequest {

    @NotNull
    private Application.ApplicationStatus status;

    /** Exam score (0-100), required when status is EXAM_PASSED or EXAM_FAILED */
    private Integer examScore;

    /** Interview date/time, required when status is INTERVIEW_SCHEDULED */
    private LocalDateTime interviewDate;

    /** Optional notes / result text */
    private String interviewResult;
}
