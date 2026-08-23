package com.gina.job_application_manager.service;

import com.gina.job_application_manager.dto.request.JobApplicationRequest;
import com.gina.job_application_manager.dto.request.JobApplicationUpdateRequest;
import com.gina.job_application_manager.dto.response.JobApplicationResponse;
import com.gina.job_application_manager.entity.JobApplication;
import com.gina.job_application_manager.exception.JobApplicationNotFoundException;
import com.gina.job_application_manager.mapper.JobApplicationMapper;
import com.gina.job_application_manager.repository.JobApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;



@Service
public class JobApplicationService {
    private final JobApplicationRepository jobApplicationRepository;
    private final JobApplicationMapper jobApplicationMapper;

    public JobApplicationService(JobApplicationRepository jobApplicationRepository, JobApplicationMapper jobApplicationMapper) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.jobApplicationMapper = jobApplicationMapper;
    }

    @Transactional
    public JobApplicationResponse createJobApplication(JobApplicationRequest request) {
        JobApplication jobApplication = JobApplication.create(
                request.companyName(),
                request.roleTitle(),
                request.source(),
                request.appliedOn(),
                request.notes()
        );

        JobApplication createdJobApplication = jobApplicationRepository.save(jobApplication);

        return jobApplicationMapper.toResponse(createdJobApplication);
    }

    @Transactional(readOnly = true)
     public JobApplicationResponse getJobApplicationById(Long id) {
        JobApplication jobApplication = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new JobApplicationNotFoundException(id));
        return jobApplicationMapper.toResponse(jobApplication);
     }

    @Transactional(readOnly = true)
    public List<JobApplicationResponse> getAllJobApplications() {
        return jobApplicationRepository
                .findAll().stream().map(jobApplicationMapper::toResponse).toList();

    }

    @Transactional
    public JobApplicationResponse updateJobApplication(Long id, JobApplicationUpdateRequest request) {
        JobApplication jobApplication = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new JobApplicationNotFoundException(id));
        jobApplicationMapper.updateEntityFromRequest(request, jobApplication);
        return jobApplicationMapper.toResponse(jobApplication);
    }

    @Transactional
    public void deleteJobApplicationById(Long id) {
        if (!jobApplicationRepository.existsById(id)) {
            throw new JobApplicationNotFoundException(id);
        }
        jobApplicationRepository.deleteById(id);
    }
}
