package com.gina.job_application_manager.util;

import com.gina.job_application_manager.dto.request.InterviewRequest;
import com.gina.job_application_manager.dto.request.InterviewUpdateRequest;
import com.gina.job_application_manager.entity.Interview;
import com.gina.job_application_manager.entity.JobApplication;
import com.gina.job_application_manager.enums.InterviewResult;
import com.gina.job_application_manager.enums.InterviewType;
import org.springframework.test.util.ReflectionTestUtils;


import java.time.LocalDateTime;

import static com.gina.job_application_manager.util.JobApplicationTestData.persistedJobApplication;

public class InterviewTestData {
    public static final LocalDateTime DEFAULT_SCHEDULED_AT = LocalDateTime.of(2026, 9, 15, 10, 30);
    public static final LocalDateTime DEFAULT_SCHEDULED_AT_2 = LocalDateTime.of(2026, 9, 20, 10, 30);

    private InterviewTestData() {}

    public static Interview newInterview(JobApplication jobApplication) {
        return Interview.create(DEFAULT_SCHEDULED_AT, InterviewType.TECHNICAL, jobApplication);
    }

    public static Interview persistedInterview(Long id,
                                               LocalDateTime scheduledAt,
                                               InterviewType type,
                                               JobApplication jobApplication) {
        Interview interview = Interview.create(scheduledAt, type, jobApplication);
        ReflectionTestUtils.setField(interview, "id", id);
        return interview;
    }


    public static Interview persistedInterview(Long id) {
        return persistedInterview(id,
        DEFAULT_SCHEDULED_AT, InterviewType.TECHNICAL , persistedJobApplication(1L));
    }



    public static InterviewRequest interviewRequest() {
        return new InterviewRequest(
                InterviewType.TECHNICAL,
                DEFAULT_SCHEDULED_AT
        );
    }

    public static InterviewUpdateRequest interviewUpdateRequest() {
        return new InterviewUpdateRequest(
                InterviewType.BEHAVIORAL,
                DEFAULT_SCHEDULED_AT_2,
                InterviewResult.FAILED
        );
    }

}
