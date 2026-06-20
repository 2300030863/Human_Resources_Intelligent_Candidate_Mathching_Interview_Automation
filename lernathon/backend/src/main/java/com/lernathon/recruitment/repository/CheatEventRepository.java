package com.lernathon.recruitment.repository;

import com.lernathon.recruitment.entity.CheatEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CheatEventRepository extends JpaRepository<CheatEvent, Long> {

    List<CheatEvent> findByExamAttemptIdOrderByDetectedAtDesc(Long examAttemptId);

    int countByExamAttemptId(Long examAttemptId);
    
    void deleteByExamAttemptId(Long examAttemptId);
}
