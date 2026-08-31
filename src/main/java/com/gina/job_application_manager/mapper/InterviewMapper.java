package com.gina.job_application_manager.mapper;

import com.gina.job_application_manager.dto.request.InterviewUpdateRequest;
import com.gina.job_application_manager.dto.response.InterviewResponse;
import com.gina.job_application_manager.entity.Interview;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;



@Mapper(componentModel = "spring")
public interface InterviewMapper {
    @Mapping(target = "applicationId", source = "application.id")
    InterviewResponse toResponse(Interview interview);

    @Mapping(target = "application", ignore = true)
    void updateEntityFromRequest(InterviewUpdateRequest request, @MappingTarget Interview interview);
}
