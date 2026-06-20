package com.lernathon.recruitment.service;

import com.lernathon.recruitment.entity.Job;
import com.lernathon.recruitment.repository.CandidateRepository;
import com.lernathon.recruitment.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class JobService {

    private final JobRepository jobRepository;
    private final CandidateRepository candidateRepository;
    private final NotificationService notificationService;

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));
    }

    @Transactional
    public Job createJob(Job job) {
        if (job.getStatus() == null) {
            job.setStatus(Job.JobStatus.DRAFT);
        }
        // Auto-fill application start date if not provided
        if (job.getApplicationStartDate() == null && job.getStatus() == Job.JobStatus.OPEN) {
            job.setApplicationStartDate(LocalDateTime.now());
        }
        Job saved = jobRepository.save(job);

        // Notify all candidates when a new OPEN job is published
        if (saved.getStatus() == Job.JobStatus.OPEN) {
            List<String> emails = candidateRepository.findAll()
                    .stream()
                    .map(c -> c.getEmail())
                    .filter(e -> e != null && !e.isBlank())
                    .collect(Collectors.toList());
            if (!emails.isEmpty()) {
                notificationService.notifyJobPosted(saved, emails);
            }
        }

        return saved;
    }

    @Transactional
    public Job updateJob(Long id, Job jobDetails) {
        Job job = getJobById(id);
        Job.JobStatus previousStatus = job.getStatus();

        job.setTitle(jobDetails.getTitle());
        job.setDescription(jobDetails.getDescription());
        job.setRequirements(jobDetails.getRequirements());
        job.setDepartment(jobDetails.getDepartment());
        job.setLocation(jobDetails.getLocation());
        job.setEmploymentType(jobDetails.getEmploymentType());
        job.setSalaryRange(jobDetails.getSalaryRange());
        job.setExperienceRequired(jobDetails.getExperienceRequired());
        job.setSkillsRequired(jobDetails.getSkillsRequired());
        job.setStatus(jobDetails.getStatus());
        job.setOpenings(jobDetails.getOpenings());
        job.setExamPassRate(jobDetails.getExamPassRate());
        job.setInterviewPassRate(jobDetails.getInterviewPassRate());
        job.setInterviewQuestionMode(jobDetails.getInterviewQuestionMode());
        if (jobDetails.getLastDate() != null) {
            job.setLastDate(jobDetails.getLastDate());
        }
        if (jobDetails.getApplicationStartDate() != null) {
            job.setApplicationStartDate(jobDetails.getApplicationStartDate());
        } else if (jobDetails.getStatus() == Job.JobStatus.OPEN && job.getApplicationStartDate() == null) {
            // Auto-fill start date if transitioning to OPEN and no start date is set
            job.setApplicationStartDate(LocalDateTime.now());
        }
        if (jobDetails.getApplicationEndDate() != null) {
            job.setApplicationEndDate(jobDetails.getApplicationEndDate());
        }

        if (jobDetails.getStatus() == Job.JobStatus.CLOSED && job.getClosedAt() == null) {
            job.setClosedAt(LocalDateTime.now());
        }

        Job saved = jobRepository.save(job);

        // Notify candidates when job becomes OPEN (status change from non-OPEN → OPEN)
        if (jobDetails.getStatus() == Job.JobStatus.OPEN && previousStatus != Job.JobStatus.OPEN) {
            List<String> emails = candidateRepository.findAll()
                    .stream()
                    .map(c -> c.getEmail())
                    .filter(e -> e != null && !e.isBlank())
                    .collect(Collectors.toList());
            if (!emails.isEmpty()) {
                notificationService.notifyJobPosted(saved, emails);
            }
        }

        return saved;
    }

    @Transactional
    public void deleteJob(Long id) {
        Job job = getJobById(id);
        jobRepository.delete(job);
    }

    public List<Job> getJobsByStatus(Job.JobStatus status) {
        return jobRepository.findByStatus(status);
    }

    public List<Job> getJobsByDepartment(String department) {
        return jobRepository.findByDepartment(department);
    }

    /**
     * Check if applications are allowed for a job
     * @param jobId the job ID
     * @return true if applications are allowed, false otherwise
     */
    public boolean isApplicationAllowed(Long jobId) {
        Job job = getJobById(jobId);
        LocalDateTime now = LocalDateTime.now();

        // Check if job is OPEN
        if (job.getStatus() != Job.JobStatus.OPEN) {
            return false;
        }

        // Check application start date
        if (job.getApplicationStartDate() != null && now.isBefore(job.getApplicationStartDate())) {
            return false;
        }

        // Check application end date
        if (job.getApplicationEndDate() != null && now.isAfter(job.getApplicationEndDate())) {
            return false;
        }

        return true;
    }

    /**
     * Get the application status message for a job
     * @param jobId the job ID
     * @return status message
     */
    public String getApplicationStatusMessage(Long jobId) {
        Job job = getJobById(jobId);
        LocalDateTime now = LocalDateTime.now();

        if (job.getStatus() != Job.JobStatus.OPEN) {
            return "Job is currently closed";
        }

        if (job.getApplicationStartDate() != null && now.isBefore(job.getApplicationStartDate())) {
            return "Applications will start on " + job.getApplicationStartDate();
        }

        if (job.getApplicationEndDate() != null && now.isAfter(job.getApplicationEndDate())) {
            return "Applications have ended";
        }

        return "Applications are open";
    }

    /**
     * Automatically close jobs whose application end date has passed
     * This method runs every minute to check and close expired jobs
     */
    @Scheduled(fixedRate = 60000) // Run every 60 seconds (1 minute)
    @Transactional
    public void autoCloseExpiredJobs() {
        LocalDateTime now = LocalDateTime.now();
        
        // Get all OPEN jobs
        List<Job> openJobs = jobRepository.findByStatus(Job.JobStatus.OPEN);
        
        // Check each job and close if application end date has passed
        for (Job job : openJobs) {
            if (job.getApplicationEndDate() != null && now.isAfter(job.getApplicationEndDate())) {
                job.setStatus(Job.JobStatus.CLOSED);
                job.setClosedAt(LocalDateTime.now());
                jobRepository.save(job);
                
                // Log the auto-closure
                org.slf4j.LoggerFactory.getLogger(JobService.class)
                    .info("Auto-closed job: {} ({}), application end date: {}", 
                        job.getId(), job.getTitle(), job.getApplicationEndDate());
            }
        }
    }
}
