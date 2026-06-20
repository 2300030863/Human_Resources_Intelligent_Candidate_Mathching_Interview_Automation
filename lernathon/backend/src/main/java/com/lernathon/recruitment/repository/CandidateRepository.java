package com.lernathon.recruitment.repository;

import com.lernathon.recruitment.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    Optional<Candidate> findByEmail(String email);
    
    List<Candidate> findByStatus(Candidate.CandidateStatus status);
    
    @Query("SELECT c FROM Candidate c WHERE " +
           "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.skills) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Candidate> searchCandidates(String keyword);
}
