package com.gina.job_application_manager.unit;


import com.gina.job_application_manager.dto.request.JobApplicationRequest;
import com.gina.job_application_manager.dto.request.JobApplicationUpdateRequest;
import com.gina.job_application_manager.dto.response.JobApplicationResponse;
import com.gina.job_application_manager.entity.JobApplication;
import com.gina.job_application_manager.enums.ApplicationStatus;
import com.gina.job_application_manager.exception.ResourceNotFoundException;
import com.gina.job_application_manager.mapper.JobApplicationMapper;
import com.gina.job_application_manager.repository.JobApplicationRepository;
import com.gina.job_application_manager.service.JobApplicationService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;


import static com.gina.job_application_manager.util.JobApplicationTestData.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import static org.mockito.Mockito.never;



import java.util.List;
import java.util.Optional;


@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Spy
    private JobApplicationMapper jobApplicationMapper = Mappers.getMapper(JobApplicationMapper.class);

    @InjectMocks
    private JobApplicationService jobApplicationService;

    @Captor
    private ArgumentCaptor<JobApplication> jobApplicationCaptor;


    @Test
    @DisplayName("Should successfully save job application and return response dto")
    void shouldSaveJobApplicationSuccessfully() {
        //given
        JobApplicationRequest request = request();


        given(jobApplicationRepository.save(any(JobApplication.class)))
                .willReturn(persistedJobApplication(1L));

        //when
        JobApplicationResponse response = jobApplicationService.createJobApplication(request);

        //then
        then(jobApplicationRepository).should().save(jobApplicationCaptor.capture());
        JobApplication captured = jobApplicationCaptor.getValue();

        assertThat(captured.getId()).isNull();
        assertThat(captured.getCompanyName()).isEqualTo("Company1");
        assertThat(captured.getStatus()).isEqualTo(ApplicationStatus.APPLIED);

        //then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.companyName()).isEqualTo("Company1");
        assertThat(response.status()).isEqualTo(ApplicationStatus.APPLIED);


    }

    @Test
    @DisplayName("Should return job application when id exists")
    void shouldReturnJobApplicationWhenIdExist() {

        //given
        Long id = 1L;

        given(jobApplicationRepository.findById(id))
                .willReturn(Optional.of(persistedJobApplication(id)));

        //when
        JobApplicationResponse response = jobApplicationService.getJobApplicationById(id);

        //then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.companyName()).isEqualTo("Company1");
        assertThat(response.status()).isEqualTo(ApplicationStatus.APPLIED);


    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when id does not exist")
    void shouldThrowResourceNotFoundWhenGettingNonExistentId() {

        //given
        Long id = 99L;

        given(jobApplicationRepository.findById(id))
                .willReturn(Optional.empty());

        //when/then
        assertThatThrownBy(() -> jobApplicationService.getJobApplicationById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        then(jobApplicationMapper).shouldHaveNoInteractions();

    }

    @Test
    @DisplayName("Should retrieve all job applications")
    void shouldRetrieveAllJobApplications() {

        //given
        given(jobApplicationRepository.findAll())
                .willReturn(List.of(
                        persistedJobApplication(1L, "Company1"),
                        persistedJobApplication(2L,"Company2")
                ));

        //when
        List<JobApplicationResponse> responses = jobApplicationService.getAllJobApplications();

        //then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).companyName()).isEqualTo("Company1");
        assertThat(responses.get(1).companyName()).isEqualTo("Company2");
    }


    @Test
    @DisplayName("Should return empty list when there are no job applications" )
    void shouldReturnEmptyList() {
        //given

        given(jobApplicationRepository.findAll())
                .willReturn(List.of());

        //then
        assertThat(jobApplicationService.getAllJobApplications()).isEmpty();
    }


    @Test
    @DisplayName("Should successfuly update a job application")
    void shouldUpdateJobApplicationSuccessfully() {
        //given
        Long id = 2L;
        JobApplicationUpdateRequest updateRequest= updateRequest();

        given(jobApplicationRepository.findById(id))
                .willReturn(Optional.of(persistedJobApplication(id)));

        //when
        JobApplicationResponse response = jobApplicationService.updateJobApplication(id, updateRequest);

        // then
        then(jobApplicationRepository).should(never()).save(any());

        //then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.companyName()).isEqualTo("Company2");
        assertThat(response.roleTitle()).isEqualTo("Senior Backend Developer");
        assertThat(response.status()).isEqualTo(ApplicationStatus.INTERVIEWING);

    }
    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating a non-existent id")
    void shouldThrowResourceNotFoundWhenUpdatingNonExistentId() {
        //given
        Long id = 99L;

        given(jobApplicationRepository.findById(id))
                .willReturn(Optional.empty());
        JobApplicationUpdateRequest updateRequest= updateRequest();

        //when/then
        assertThatThrownBy(() -> jobApplicationService.updateJobApplication(id, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

    }

    @Test
    @DisplayName("Should successfully delete a job application")
    void  shouldDeleteJobApplicationSuccessfully() {
        //given
        Long id = 2L;

        given(jobApplicationRepository.existsById(id))
                .willReturn(Boolean.TRUE);

        //when
        jobApplicationService.deleteJobApplicationById(id);

        //then
        then(jobApplicationRepository).should().deleteById(id);
    }

    @Test
    @DisplayName("Should abort delete execution and throw exception when target doesn't exist")
    void shouldAbortDeleteExecutionAndThrowException() {
        //given
        Long id = 2L;
        given(jobApplicationRepository.existsById(id))
                .willReturn(Boolean.FALSE);

        //when/then
        assertThatThrownBy(() -> jobApplicationService.deleteJobApplicationById(id))
                .isInstanceOf(ResourceNotFoundException.class);

        then(jobApplicationRepository).should(never()).deleteById(id);
    }

}
