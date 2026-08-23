package com.gina.job_application_manager.dto.request;


import com.gina.job_application_manager.enums.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record JobApplicationUpdateRequest(
        @NotBlank(message = "Company name is required")
        String companyName,
        @NotBlank(message = "Role title is required")
        String roleTitle,
        String source,
        @NotNull
        ApplicationStatus status,
        @NotNull
        LocalDate appliedOn,
        String notes) {
}
