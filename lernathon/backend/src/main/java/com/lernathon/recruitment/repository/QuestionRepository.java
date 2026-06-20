package com.lernathon.recruitment.repository;

import com.lernathon.recruitment.entity.Question;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findBySkillAndLevelAndTypeAndIsActiveTrue(
            String skill, 
            Question.DifficultyLevel level, 
            Question.QuestionType type
    );

    List<Question> findBySkillAndLevelAndIsActiveTrue(
            String skill, 
            Question.DifficultyLevel level
    );

    List<Question> findBySkillAndTypeAndIsActiveTrue(
            String skill, 
            Question.QuestionType type
    );

    @Query("SELECT DISTINCT q.skill FROM Question q WHERE q.isActive = true")
    List<String> findAllActiveSkills();

    @Query(value = "SELECT * FROM questions WHERE skill = :skill AND level = :level AND type = :type AND is_active = true ORDER BY RAND()", nativeQuery = true)
    List<Question> findRandomQuestionsBySkillAndLevelAndType(
            @Param("skill") String skill,
            @Param("level") String level,
            @Param("type") String type,
            Pageable pageable
    );

    @Query(value = "SELECT * FROM questions WHERE level = :level AND type = :type AND is_active = true ORDER BY RAND()", nativeQuery = true)
    List<Question> findRandomQuestionsByLevelAndType(
            @Param("level") String level,
            @Param("type") String type,
            Pageable pageable
    );
}
