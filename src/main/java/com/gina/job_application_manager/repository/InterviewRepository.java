package com.gina.job_application_manager.repository;

import com.gina.job_application_manager.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    Optional<Interview> findByIdAndApplicationId(Long id, Long applicationId);
    List<Interview> findByApplicationIdOrderByScheduledAtDesc(Long applicationId);

    long deleteByIdAndApplicationId(Long id, Long applicationId);
}
