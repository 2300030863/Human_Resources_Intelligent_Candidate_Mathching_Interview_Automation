package com.lernathon.recruitment.controller;

import com.lernathon.recruitment.dto.InterviewDTO;
import com.lernathon.recruitment.dto.InterviewResultDTO;
import com.lernathon.recruitment.entity.Interview;
import com.lernathon.recruitment.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @GetMapping
    public ResponseEntity<List<InterviewDTO>> getAllInterviews() {
        return ResponseEntity.ok(interviewService.getAllInterviewsAsDTO());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InterviewDTO> getInterviewById(@PathVariable Long id) {
        return ResponseEntity.ok(interviewService.getInterviewByIdAsDTO(id));
    }

    @PostMapping
    public ResponseEntity<Interview> createInterview(
            @RequestParam Long applicationId,
            @RequestBody Interview interview
    ) {
        return ResponseEntity.ok(interviewService.createInterview(applicationId, interview));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Interview> updateInterview(
            @PathVariable Long id,
            @RequestBody Interview interview
    ) {
        return ResponseEntity.ok(interviewService.updateInterview(id, interview));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInterview(@PathVariable Long id) {
        interviewService.deleteInterview(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<List<InterviewDTO>> getInterviewsByApplication(@PathVariable Long applicationId) {
        return ResponseEntity.ok(interviewService.getInterviewsByApplicationAsDTO(applicationId));
    }

    @GetMapping("/candidate/{candidateId}")
    public ResponseEntity<List<InterviewDTO>> getInterviewsByCandidate(@PathVariable Long candidateId) {
        return ResponseEntity.ok(interviewService.getInterviewsByCandidateAsDTO(candidateId));
    }

    @GetMapping("/interviewer/{interviewerId}")
    public ResponseEntity<List<InterviewDTO>> getInterviewsByInterviewer(@PathVariable Long interviewerId) {
        return ResponseEntity.ok(interviewService.getInterviewsByInterviewerAsDTO(interviewerId));
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<InterviewDTO>> getInterviewsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return ResponseEntity.ok(interviewService.getInterviewsByDateRangeAsDTO(start, end));
    }

    @PostMapping("/{id}/result")
    public ResponseEntity<Interview> submitInterviewResult(
            @PathVariable Long id,
            @RequestBody InterviewResultDTO resultDTO
    ) {
        return ResponseEntity.ok(interviewService.submitInterviewResult(id, resultDTO));
    }

    /**
     * AI interview system endpoint - no auth required (called by internal Streamlit app)
     * Automatically marks candidate as passed/failed and updates application status.
     */
    @PostMapping("/{id}/ai-result")
    public ResponseEntity<Interview> submitAIInterviewResult(
            @PathVariable Long id,
            @RequestBody InterviewResultDTO resultDTO
    ) {
        return ResponseEntity.ok(interviewService.submitInterviewResult(id, resultDTO));
    }
}
