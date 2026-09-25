package com.gina.job_application_manager.integration;



import com.gina.job_application_manager.util.InterviewTestData;
import com.gina.job_application_manager.entity.Interview;
import com.gina.job_application_manager.entity.JobApplication;
import com.gina.job_application_manager.repository.InterviewRepository;
import com.gina.job_application_manager.repository.JobApplicationRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;


import java.util.List;

import static com.gina.job_application_manager.util.JobApplicationTestData.newJobApplication;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.hamcrest.Matchers.*;


class JobApplicationIntegrationTest extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/api/v1/job-applications";
    private static final String COMPANY = "Company1";
    private static final String UPDATED_COMPANY = "Company2";


    @LocalServerPort
    private Integer port;


    @Autowired
    JobApplicationRepository jobApplicationRepository;

    @Autowired
    InterviewRepository interviewRepository;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost:" + port;
        jobApplicationRepository.deleteAll();
    }

    private JobApplication persistApplication() {
        return jobApplicationRepository.save(newJobApplication(COMPANY));
    }

    private static String path(Long id) {
        return BASE_PATH + "/" + id;
    }


    @Test
    void shouldCreateJobApplicationSuccessfully() {
        String validBody = """
        {
          "companyName": "%s",
          "roleTitle": "Backend Developer",
          "source": "LinkedIn",
          "appliedOn": "2026-09-01",
          "notes": "some notes"
        }
        """.formatted(COMPANY);

        given()
                .contentType(ContentType.JSON)
                .body(validBody)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(200)
                .header("Location", matchesPattern(".*" + BASE_PATH + "/\\d+$"))
                .body("id", notNullValue())
                .body("companyName", equalTo(COMPANY))
                .body("status", equalTo("FAILED"));
    }

    @Test
    void shouldUpdateJobApplicationAndReturn200() {
        Long id = persistApplication().getId();

        String updateBody = """
        {
          "companyName": "%s",
          "roleTitle": "Senior Backend Developer",
          "source": "LinkedIn",
          "status": "INTERVIEWING",
          "appliedOn": "2026-09-05",
          "notes": "some notes updated"
        }
        """.formatted(UPDATED_COMPANY);

        given()
                .contentType(ContentType.JSON)
                .body(updateBody)
                .when()
                .put(path(id))
                .then()
                .statusCode(200)
                .body("companyName", equalTo(UPDATED_COMPANY))
                .body("status", equalTo("INTERVIEWING"));
    }

    @Test
    void shouldReturnJobApplicationWhenIdExists() {
        Long id = persistApplication().getId();

        given()
                .when()
                .get(path(id))
                .then()
                .statusCode(200)
                .body("id", equalTo(id.intValue()))
                .body("companyName", equalTo(COMPANY));

    }

    @Test
    void shouldReturn400WithValidationErrorsWhenCompanyNameIsBlank() {
        String invalidBody = """
        {
          "companyName": "",
          "roleTitle": "Backend Developer",
          "source": "LinkedIn",
          "appliedOn": "2026-09-01"
        }
        """;

        given()
                .contentType(ContentType.JSON)
                .body(invalidBody)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(400)
                .body("title", equalTo("Validation Failed"))
                .body("invalidFields.companyName", not(empty()));
    }

    @Test
    void shouldReturn400WithInvalidValueWhenStatusEnumIsInvalid() {
        Long id = persistApplication().getId();

        String bodyWithBadEnum = """
        {
          "companyName": "%s",
          "roleTitle": "Backend Developer",
          "source": "LinkedIn",
          "status": "GHOSTED",
          "appliedOn": "2026-09-01",
          "notes": "some notes"
        }
        """.formatted(COMPANY);

        given()
                .contentType(ContentType.JSON)
                .body(bodyWithBadEnum)
                .when()
                .put(path(id))
                .then()
                .statusCode(400)
                .body("invalidValue.field", equalTo("status"))
                .body("invalidValue.allowedValues", hasItem("APPLIED"));
    }

    @Test
    void shouldGetAllJobApplications() {
        jobApplicationRepository.saveAll(List.of(
                newJobApplication(COMPANY),
                newJobApplication(UPDATED_COMPANY)
        ));

        given()
                .contentType(ContentType.JSON)
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .body(".", hasSize(2));
    }

    @Test
    void shouldReturn404WhenJobApplicationDoesNotExist() {
        given()
                .when()
                .get(BASE_PATH + "/99999")
                .then()
                .statusCode(404)
                .body("title", equalTo("Resource Not Found"))
                .body("detail", containsString("99999"));
    }

    @Test
    void shouldDeleteJobApplicationViaHttp() {
        Long id = persistApplication().getId();

        given()
                .when()
                .delete(path(id))
                .then()
                .statusCode(204);
    }


    @Test
    void deletingApplicationCascadesToInterviews() {
        JobApplication app = persistApplication();

        Interview interview = interviewRepository.save(InterviewTestData.newInterview(app));

        jobApplicationRepository.deleteById(app.getId());

        assertThat(interviewRepository.findById(interview.getId())).isEmpty();
    }
}
