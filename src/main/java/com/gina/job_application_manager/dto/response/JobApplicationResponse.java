package com.gina.job_application_manager.dto.response;

import com.gina.job_application_manager.enums.ApplicationStatus;

import java.time.LocalDate;

public record JobApplicationResponse(
        Long id,
        String companyName,
        String roleTitle,
        String source,
        ApplicationStatus status,
        LocalDate appliedOn,
        String notes) {

}
