package com.lernathon.recruitment.repository;

import com.lernathon.recruitment.entity.ExamAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamAnswerRepository extends JpaRepository<ExamAnswer, Long> {

    List<ExamAnswer> findByExamAttemptIdOrderById(Long examAttemptId);

    Optional<ExamAnswer> findByExamAttemptIdAndQuestionId(Long examAttemptId, Long questionId);

    int countByExamAttemptIdAndIsCorrectTrue(Long examAttemptId);

    int countByExamAttemptId(Long examAttemptId);
    
    void deleteByExamAttemptId(Long examAttemptId);
}
