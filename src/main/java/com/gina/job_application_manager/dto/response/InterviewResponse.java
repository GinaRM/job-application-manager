package com.gina.job_application_manager.dto.response;

import com.gina.job_application_manager.enums.InterviewResult;
import com.gina.job_application_manager.enums.InterviewType;

import java.time.LocalDateTime;

public record InterviewResponse(
        Long id,
        LocalDateTime scheduledAt,
        InterviewType type,
        InterviewResult result,
        Long applicationId
) {
}
