package com.gina.job_application_manager.controller;

import com.gina.job_application_manager.dto.request.InterviewRequest;
import com.gina.job_application_manager.dto.request.InterviewUpdateRequest;
import com.gina.job_application_manager.dto.response.InterviewResponse;
import com.gina.job_application_manager.service.InterviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/job-applications/{applicationId}/interviews")
public class InterviewController {
    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @GetMapping
    public ResponseEntity<List<InterviewResponse>> getAllInterviews(@PathVariable Long applicationId) {
        return ResponseEntity.ok(interviewService.getAllInterviews(applicationId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InterviewResponse> getInterviewById(@PathVariable Long id, @PathVariable Long applicationId) {
        return ResponseEntity.ok(interviewService.getInterviewById(id, applicationId));
    }

    @PostMapping
    public ResponseEntity<InterviewResponse> createInterview(@PathVariable Long applicationId, @Valid @RequestBody InterviewRequest interviewRequest) {
        InterviewResponse interviewResponse = interviewService.createInterview(applicationId, interviewRequest);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(interviewResponse.id())
                .toUri();
        return ResponseEntity.created(location).body(interviewResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InterviewResponse> updateInterview(@PathVariable Long id, @PathVariable Long applicationId, @Valid @RequestBody InterviewUpdateRequest interviewUpdateRequest) {
        return ResponseEntity.ok(interviewService.updateInterview(id, applicationId, interviewUpdateRequest));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInterview(@PathVariable Long id, @PathVariable Long applicationId) {
        interviewService.deleteInterview(id, applicationId);
        return ResponseEntity.noContent().build();
    }
}
