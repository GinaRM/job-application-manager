package com.gina.job_application_manager.mapper;


import com.gina.job_application_manager.dto.request.JobApplicationUpdateRequest;
import com.gina.job_application_manager.dto.response.JobApplicationResponse;
import com.gina.job_application_manager.entity.JobApplication;
import org.mapstruct.Mapper;

import org.mapstruct.MappingTarget;

@Mapper (componentModel = "spring")
public interface JobApplicationMapper {

    JobApplicationResponse toResponse(JobApplication jobApplication);

    void updateEntityFromRequest(JobApplicationUpdateRequest request, @MappingTarget JobApplication jobApplication);
}
