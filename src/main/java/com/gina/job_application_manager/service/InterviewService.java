package com.gina.job_application_manager.service;

import com.gina.job_application_manager.dto.request.InterviewRequest;
import com.gina.job_application_manager.dto.request.InterviewUpdateRequest;
import com.gina.job_application_manager.dto.response.InterviewResponse;
import com.gina.job_application_manager.entity.Interview;
import com.gina.job_application_manager.entity.JobApplication;
import com.gina.job_application_manager.exception.ResourceNotFoundException;
import com.gina.job_application_manager.mapper.InterviewMapper;
import com.gina.job_application_manager.repository.InterviewRepository;
import com.gina.job_application_manager.repository.JobApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InterviewService {
    private final JobApplicationRepository jobApplicationRepository;
    private final InterviewRepository interviewRepository;
    private final InterviewMapper interviewMapper;

    public InterviewService(JobApplicationRepository jobApplicationRepository, InterviewRepository interviewRepository, InterviewMapper interviewMapper) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.interviewRepository = interviewRepository;
        this.interviewMapper = interviewMapper;
    }

    @Transactional
    public InterviewResponse createInterview(Long applicationId, InterviewRequest request) {

        JobApplication jobFound =  jobApplicationRepository
                .findById(applicationId)
                .orElseThrow( () -> new ResourceNotFoundException("Job Application", applicationId));
        Interview interview = Interview.create(
                request.scheduledAt(),
                request.type(),
                jobFound
        );
        Interview createdInterview = interviewRepository.save(interview);
        return interviewMapper.toResponse(createdInterview);
    }

    @Transactional(readOnly = true)
    public InterviewResponse getInterviewById(Long id, Long applicationId) {
        Interview interview = interviewRepository.findByIdAndApplicationId(id, applicationId).orElseThrow( () -> new ResourceNotFoundException("Interview", id));
        return interviewMapper.toResponse(interview);
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse>  getAllInterviews(Long applicationId) {
        return interviewRepository
                .findByApplicationIdOrderByScheduledAtDesc(applicationId)
                .stream()
                .map(interviewMapper::toResponse)
                .toList();

    }

    @Transactional
    public InterviewResponse updateInterview(Long id, Long applicationId, InterviewUpdateRequest request) {
        Interview interview = interviewRepository.findByIdAndApplicationId(id, applicationId).orElseThrow( () -> new ResourceNotFoundException("Interview", id));
        interviewMapper.updateEntityFromRequest(request, interview);
        return interviewMapper.toResponse(interview);
    }

    @Transactional
    public void deleteInterview(Long id, Long applicationId) {
        if (interviewRepository.deleteByIdAndApplicationId(id, applicationId) == 0) {
            throw new ResourceNotFoundException("Interview", id);
        }

    }


}
