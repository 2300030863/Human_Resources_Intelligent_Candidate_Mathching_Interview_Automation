package com.lernathon.recruitment.controller;

import com.lernathon.recruitment.entity.Job;
import com.lernathon.recruitment.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs() {
        return ResponseEntity.ok(jobService.getAllJobs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    @PostMapping
    public ResponseEntity<Job> createJob(@RequestBody Job job) {
        return ResponseEntity.ok(jobService.createJob(job));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Job> updateJob(@PathVariable Long id, @RequestBody Job job) {
        return ResponseEntity.ok(jobService.updateJob(id, job));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Job>> getJobsByStatus(@PathVariable Job.JobStatus status) {
        return ResponseEntity.ok(jobService.getJobsByStatus(status));
    }

    @GetMapping("/department/{department}")
    public ResponseEntity<List<Job>> getJobsByDepartment(@PathVariable String department) {
        return ResponseEntity.ok(jobService.getJobsByDepartment(department));
    }

    @GetMapping("/{id}/application-allowed")
    public ResponseEntity<Map<String, Object>> checkApplicationAllowed(@PathVariable Long id) {
        boolean allowed = jobService.isApplicationAllowed(id);
        String message = jobService.getApplicationStatusMessage(id);
        Map<String, Object> response = new HashMap<>();
        response.put("allowed", allowed);
        response.put("message", message);
        return ResponseEntity.ok(response);
    }
}
