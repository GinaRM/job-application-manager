package com.gina.job_application_manager.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceName, Object id) {
        super(String.format("%s not found with id : '%s'", resourceName, id));
    }
}
