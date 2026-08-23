package com.gina.job_application_manager.repository;

import com.gina.job_application_manager.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;


public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
}
