package com.gina.job_application_manager.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record JobApplicationRequest(

        @NotBlank (message = "Company name is required")
        String companyName,

        @NotBlank(message = "Role title is required")
        String roleTitle,

        @NotBlank (message = "Source is required")
        String source,

        @NotNull
        LocalDate appliedOn,
        String notes) {
}
