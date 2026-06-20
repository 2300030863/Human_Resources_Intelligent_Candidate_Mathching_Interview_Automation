package com.lernathon.recruitment.repository;

import com.lernathon.recruitment.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByStatus(Job.JobStatus status);
    List<Job> findByDepartment(String department);
    List<Job> findByHiringManagerId(Long hiringManagerId);
}
