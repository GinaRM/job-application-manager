package com.gina.job_application_manager.exception;

public class JobApplicationNotFoundException extends RuntimeException {
    public JobApplicationNotFoundException(Long id) {
        super("Job Application Not Found with id: " + id);
    }
}
