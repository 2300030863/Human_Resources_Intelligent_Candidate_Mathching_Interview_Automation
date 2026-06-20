package com.lernathon.recruitment.repository;

import com.lernathon.recruitment.entity.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    Optional<ExamAttempt> findByCandidate_IdAndJob_Id(Long candidateId, Long jobId);

    List<ExamAttempt> findByCandidate_IdOrderByCreatedAtDesc(Long candidateId);
    
    List<ExamAttempt> findByCandidate_Id(Long candidateId);
    
    List<ExamAttempt> findByApplication_Id(Long applicationId);

    List<ExamAttempt> findByJob_IdOrderByFinalScoreDesc(Long jobId);

    Optional<ExamAttempt> findBySessionToken(String sessionToken);

    boolean existsByCandidate_IdAndJob_IdAndStatus(Long candidateId, Long jobId, ExamAttempt.ExamStatus status);

    List<ExamAttempt> findByStatus(ExamAttempt.ExamStatus status);
    
    void deleteByCandidate_Id(Long candidateId);
    
    void deleteByApplication_Id(Long applicationId);
}
