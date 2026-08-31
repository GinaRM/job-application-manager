package com.gina.job_application_manager.entity;

import com.gina.job_application_manager.enums.InterviewResult;
import com.gina.job_application_manager.enums.InterviewType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "interview")
public class Interview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewResult result;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication application;

    public static Interview create(LocalDateTime scheduleAt, InterviewType type, JobApplication application) {
        Interview interview = new Interview();
        interview.scheduledAt = scheduleAt;
        interview.type = type;
        interview.result = InterviewResult.PENDING;
        interview.application = application;
        return interview;
    }
}
