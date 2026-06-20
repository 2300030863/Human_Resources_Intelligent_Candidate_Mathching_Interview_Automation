package com.lernathon.recruitment.repository;

import com.lernathon.recruitment.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
    List<Interview> findByApplication_Id(Long applicationId);
    List<Interview> findByApplication_CandidateId(Long candidateId);
    List<Interview> findByInterviewerId(Long interviewerId);
    List<Interview> findByStatus(Interview.InterviewStatus status);
    List<Interview> findByScheduledAtBetween(LocalDateTime start, LocalDateTime end);
}
