package com.gina.job_application_manager.controller;


import com.gina.job_application_manager.dto.request.JobApplicationRequest;
import com.gina.job_application_manager.dto.request.JobApplicationUpdateRequest;
import com.gina.job_application_manager.dto.response.JobApplicationResponse;
import com.gina.job_application_manager.service.JobApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/job-applications")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    public JobApplicationController(JobApplicationService jobApplicationService) {
        this.jobApplicationService = jobApplicationService;
    }

    @GetMapping
    public ResponseEntity<List<JobApplicationResponse>>  getAllJobApplications() {
        return ResponseEntity.ok(jobApplicationService.getAllJobApplications());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobApplicationResponse> getJobApplicationById(@PathVariable Long id) {
        return ResponseEntity.ok(jobApplicationService.getJobApplicationById(id));
    }

    @PostMapping
    public ResponseEntity<JobApplicationResponse> createJobApplication(@Valid @RequestBody JobApplicationRequest jobApplicationRequest) {
        JobApplicationResponse jobApplicationResponse = jobApplicationService.createJobApplication(jobApplicationRequest);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(jobApplicationResponse.id())
                .toUri();

        return ResponseEntity.created(location).body(jobApplicationResponse);

    }

    @PutMapping("/{id}")
    public ResponseEntity<JobApplicationResponse> updateJobApplication(@PathVariable Long id, @Valid @RequestBody JobApplicationUpdateRequest jobApplicationRequest) {
        return ResponseEntity.ok(jobApplicationService.updateJobApplication(id, jobApplicationRequest));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJobApplication(@PathVariable Long id) {
        jobApplicationService.deleteJobApplicationById(id);
        return ResponseEntity.noContent().build();
    }
}
