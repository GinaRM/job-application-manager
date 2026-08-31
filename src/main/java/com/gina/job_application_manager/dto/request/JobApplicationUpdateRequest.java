package com.gina.job_application_manager.dto.request;


import com.gina.job_application_manager.enums.ApplicationStatus;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record JobApplicationUpdateRequest(
        @NotBlank (message = "Company name is required")
        @Size(max = 70, message = "Company name cannot exceed 70 characters")
        String companyName,

        @NotBlank(message = "Role title is required")
        @Size(max = 70, message = "Role title cannot exceed 70 characters")
        String roleTitle,

        @NotBlank (message = "Source is required")
        String source,
        @NotNull(message = "Status is required")
        ApplicationStatus status,
        @NotNull(message = "Date applied is required")
        @PastOrPresent(message = "Date applied cannot be in the future")
        LocalDate appliedOn,
        String notes) {
}
