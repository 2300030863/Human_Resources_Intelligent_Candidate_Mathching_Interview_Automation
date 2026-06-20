package com.lernathon.recruitment.repository;

import com.lernathon.recruitment.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    
    @Query("SELECT a FROM Application a LEFT JOIN FETCH a.candidate LEFT JOIN FETCH a.job")
    List<Application> findAllWithDetails();
    
    List<Application> findByCandidateId(Long candidateId);
    List<Application> findByJobId(Long jobId);
    List<Application> findByStatus(Application.ApplicationStatus status);
    boolean existsByCandidateIdAndJobId(Long candidateId, Long jobId);
}
