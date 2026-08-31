package com.gina.job_application_manager.dto.request;

import com.gina.job_application_manager.enums.InterviewType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record InterviewRequest(

        @NotNull(message = "Interview type is required")
        InterviewType type,

        @NotNull(message = "Scheduled date is required")
        LocalDateTime scheduledAt

) {
}
