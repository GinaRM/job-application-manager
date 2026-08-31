package com.gina.job_application_manager.exception;


import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.databind.exc.InvalidFormatException;

import java.net.URI;
import java.util.*;


@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        //To have a list of messages per error field name
        Map<String, List<String> > errors = new HashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String fieldName = fieldError.getField();
            String message = fieldError.getDefaultMessage();

            errors.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(message);
        }

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        pd.setTitle("Validation Failed");
        pd.setProperty("invalidFields", errors);
        applyInstance(pd, request);

        return handleExceptionInternal(ex, pd, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        ProblemDetail body = createProblemDetail(
                ex,
                status, "Failed to read request", null, null, request);

        if (ex.getCause() instanceof InvalidFormatException invalidFormatException) {
            Map<String, Object> invalidValue = new HashMap<>();

            if (!invalidFormatException.getPath().isEmpty()){
            invalidValue.put("field", invalidFormatException.getPath().get(0).getPropertyName());
            }
            invalidValue.put("value", String.valueOf(invalidFormatException.getValue()));

            Object[] constants = invalidFormatException.getTargetType().getEnumConstants();
            if(constants != null){
                List<String> allowedValues = new ArrayList<>();
                for (Object constant : constants) {
                    allowedValues.add(constant.toString());
                }
                invalidValue.put("allowedValues", allowedValues);
            }
            body.setProperty("invalidValue", invalidValue);
        }
        log.warn("Unreadable request body: {}", ex.getMessage());
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Resource Not Found");
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGlobalException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception occurred: ", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later or contact support.");
        pd.setTitle("Internal Server Error");
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }

    private static void applyInstance(ProblemDetail pd, WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            pd.setInstance(URI.create(servletWebRequest.getRequest().getRequestURI()));
        }
    }
}
