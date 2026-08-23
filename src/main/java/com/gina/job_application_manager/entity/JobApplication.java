package com.gina.job_application_manager.entity;


import com.gina.job_application_manager.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "job_application")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String companyName;

    @Column(nullable = false, length = 150)
    private String roleTitle;

    @Column(length = 100)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status;

    @Column(nullable = false)
    private LocalDate appliedOn;

    @Column(columnDefinition = "TEXT")
    private String notes;


    public static JobApplication create(String companyName, String roleTitle, String source, LocalDate appliedOn, String notes) {
        JobApplication jobApplication = new JobApplication();
        jobApplication.companyName = companyName;
        jobApplication.roleTitle = roleTitle;
        jobApplication.source = source;
        jobApplication.status = ApplicationStatus.APPLIED;
        jobApplication.appliedOn = appliedOn;
        jobApplication.notes = notes;
        return jobApplication;
    }

}
