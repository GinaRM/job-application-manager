package com.gina.job_application_manager.util;

import com.gina.job_application_manager.dto.request.JobApplicationRequest;
import com.gina.job_application_manager.dto.request.JobApplicationUpdateRequest;
import com.gina.job_application_manager.entity.JobApplication;
import com.gina.job_application_manager.enums.ApplicationStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

public class JobApplicationTestData {

    public static final LocalDate DEFAULT_APPLIED_ON = LocalDate.of(2026, 8, 15);

    private JobApplicationTestData() {}

    public static JobApplication newJobApplication() {

        return newJobApplication("Company1");
    }


    public static JobApplication newJobApplication(String companyName) {
        return JobApplication.create(
                companyName,
                "Backend Developer",
                "E-mail",
                DEFAULT_APPLIED_ON,
                "notes-1"
        );
    }

    public static JobApplication persistedJobApplication(Long id) {

        return persistedJobApplication(id, "Company1");
    }

    public static JobApplication persistedJobApplication(Long id, String companyName) {
        JobApplication jobApplication = newJobApplication(companyName);
        ReflectionTestUtils.setField(jobApplication, "id", id);
        return jobApplication;
    }

    public static JobApplicationRequest request() {
        return new JobApplicationRequest(
                "Company1",
                "Backend Developer",
                "E-mail",
                DEFAULT_APPLIED_ON,
                "notes-1"
        );
    }

    public static JobApplicationUpdateRequest updateRequest() {
        return new JobApplicationUpdateRequest(
                "Company2",
                "Senior Backend Developer",
                "LinkedIn",
                ApplicationStatus.INTERVIEWING,
                DEFAULT_APPLIED_ON,
                "updated notes"
        );
    }
}
