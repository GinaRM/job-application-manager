package com.gina.job_application_manager.unit;


import com.gina.job_application_manager.dto.request.InterviewRequest;
import com.gina.job_application_manager.dto.request.InterviewUpdateRequest;
import com.gina.job_application_manager.dto.response.InterviewResponse;
import com.gina.job_application_manager.entity.Interview;
import com.gina.job_application_manager.entity.JobApplication;
import com.gina.job_application_manager.enums.InterviewResult;
import com.gina.job_application_manager.enums.InterviewType;
import com.gina.job_application_manager.exception.ResourceNotFoundException;
import com.gina.job_application_manager.mapper.InterviewMapper;
import com.gina.job_application_manager.repository.InterviewRepository;
import com.gina.job_application_manager.repository.JobApplicationRepository;
import com.gina.job_application_manager.service.InterviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.List;
import java.util.Optional;

import static com.gina.job_application_manager.util.InterviewTestData.*;
import static com.gina.job_application_manager.util.JobApplicationTestData.persistedJobApplication;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private InterviewRepository interviewRepository;

    @Spy
    private InterviewMapper interviewMapper = Mappers.getMapper(InterviewMapper.class);

    @InjectMocks
    private InterviewService interviewService;

    @Captor
    private ArgumentCaptor<Interview> interviewCaptor;

    @Test
    @DisplayName("Should successfully save interview and return response dto")
    void shouldSaveInterviewAndReturnResponseDTO() {
        //given
        Long applicationId = 1L;
        InterviewRequest interviewRequest = interviewRequest();

        given(jobApplicationRepository.findById(applicationId))
                .willReturn(Optional.of(persistedJobApplication(applicationId)));

        given(interviewRepository.save(any(Interview.class)))
                .willReturn(persistedInterview(1L,
                        DEFAULT_SCHEDULED_AT,
                        InterviewType.TECHNICAL,
                        persistedJobApplication(applicationId)));

        //when
        InterviewResponse response = interviewService.createInterview(applicationId, interviewRequest);

        //then
        then(interviewRepository).should().save(interviewCaptor.capture());
        Interview captured = interviewCaptor.getValue();

        assertThat(captured.getId()).isNull();
        assertThat(captured.getApplication().getId()).isEqualTo(applicationId);
        assertThat(captured.getScheduledAt()).isEqualTo(DEFAULT_SCHEDULED_AT);
        assertThat(captured.getType()).isEqualTo(InterviewType.TECHNICAL);
        assertThat(captured.getResult()).isEqualTo(InterviewResult.PENDING);

        //then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.scheduledAt()).isEqualTo(DEFAULT_SCHEDULED_AT);
        assertThat(response.type()).isEqualTo(InterviewType.TECHNICAL);
        assertThat(response.result()).isEqualTo(InterviewResult.PENDING);

    }

    @Test
    @DisplayName("Should return Resource not found when creating an interview and job application doesn't exist")
    void shouldReturnNotFoundWhenCreatingInterviewAndJobApplicationDoesNotExist() {
        //given
        Long applicationId = 99L;
        InterviewRequest interviewRequest = interviewRequest();

        given(jobApplicationRepository.findById(applicationId)).willReturn(Optional.empty());

        //when/then
        assertThatThrownBy(() -> interviewService.createInterview(applicationId, interviewRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        then(interviewRepository).shouldHaveNoInteractions();
        then(interviewMapper).shouldHaveNoInteractions();

    }

    @Test
    @DisplayName("Should return interview linked to job application when id exists and matches")
    void shouldReturnInterviewWhenIdExist() {
        //given
        Long applicationId = 1L;
        Long id = 1L;

        given(interviewRepository.findByIdAndApplicationId(id, applicationId))
                .willReturn(Optional.of(persistedInterview(1L,
                        DEFAULT_SCHEDULED_AT, InterviewType.TECHNICAL,
                        persistedJobApplication(applicationId))));

        //when
        InterviewResponse response = interviewService.getInterviewById(id, applicationId);

        //then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.scheduledAt()).isEqualTo(DEFAULT_SCHEDULED_AT);
        assertThat(response.type()).isEqualTo(InterviewType.TECHNICAL);
        assertThat(response.result()).isEqualTo(InterviewResult.PENDING);
        assertThat(response.applicationId()).isEqualTo(applicationId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when interview is not found under the given job application")
    void shouldThrowResourceNotFoundWhenGettingNonExistentId() {
        //given
        Long applicationId = 50L;
        Long id = 99L;
        given(interviewRepository.findByIdAndApplicationId(id, applicationId))
                .willReturn(Optional.empty());

        //when/then
        assertThatThrownBy(() -> interviewService.getInterviewById(id, applicationId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
        then(interviewMapper).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should retrieve all interviews")
    void shouldRetrieveAllInterviews() {
        //given
        Long applicationId = 1L;
        JobApplication jobApplication = persistedJobApplication(applicationId);
        given(interviewRepository.findByApplicationIdOrderByScheduledAtDesc(applicationId))
                .willReturn(List.of(
                        persistedInterview(2L, DEFAULT_SCHEDULED_AT_2, InterviewType.BEHAVIORAL , jobApplication),
                        persistedInterview(1L, DEFAULT_SCHEDULED_AT, InterviewType.TECHNICAL, jobApplication)
                ));

        //when
        List<InterviewResponse> responses = interviewService.getAllInterviews(applicationId);

        //then
        assertThat(responses).hasSize(2)
                .extracting(
                        InterviewResponse::id,
                        InterviewResponse::scheduledAt,
                        InterviewResponse::type,
                        InterviewResponse::result,
                        InterviewResponse::applicationId)
                .containsExactly(
                        tuple(2L, DEFAULT_SCHEDULED_AT_2, InterviewType.BEHAVIORAL, InterviewResult.PENDING, applicationId),
                        tuple(1L, DEFAULT_SCHEDULED_AT, InterviewType.TECHNICAL, InterviewResult.PENDING, applicationId)
                );

    }

    @Test
    @DisplayName("Should return empty list when job application has no interviews")
    void shouldReturnEmptyListWhenNoInterviews() {
        //given
        Long applicationId = 1L;
        given(interviewRepository.findByApplicationIdOrderByScheduledAtDesc(applicationId))
                .willReturn(List.of());

        //then
        assertThat(interviewService.getAllInterviews(applicationId)).isEmpty();
    }

    @Test
    @DisplayName("Should successfuly update an Interview")
    void shouldUpdateInterviewSuccessfully() {
        //given
        Long applicationId = 1L;
        Long id = 1L;
        InterviewUpdateRequest updateRequest = interviewUpdateRequest();

        given(interviewRepository.findByIdAndApplicationId(id, applicationId))
                .willReturn(Optional.of(persistedInterview(
                        id,
                        DEFAULT_SCHEDULED_AT,
                        InterviewType.TECHNICAL,
                        persistedJobApplication(applicationId)
                )));

        //when
        InterviewResponse response = interviewService.updateInterview(id, applicationId, updateRequest);

        //then
        then(interviewRepository).should(never()).save(any());

        //then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.scheduledAt()).isEqualTo(DEFAULT_SCHEDULED_AT_2);
        assertThat(response.type()).isEqualTo(InterviewType.BEHAVIORAL);
        assertThat(response.result()).isEqualTo(InterviewResult.FAILED);
        assertThat(response.applicationId()).isEqualTo(applicationId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating a non-existent id")
    void shouldThrowResourceNotFoundWhenUpdatingNonExistentId() {
        //given
        Long id = 99L;
        Long applicationId = 1L;

        given(interviewRepository.findByIdAndApplicationId(id, applicationId))
                .willReturn(Optional.empty());

        InterviewUpdateRequest updateRequest = interviewUpdateRequest();

        //when/then
        assertThatThrownBy(() -> interviewService.updateInterview(id, applicationId, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

    }

    @Test
    @DisplayName("Should successfully delete an interview")
    void  shouldDeleteInterviewSuccessfully() {
        //given
        Long applicationId = 1L;
        Long id = 2L;

        given(interviewRepository.deleteByIdAndApplicationId(id, applicationId))
                .willReturn(1L);

        //when
        interviewService.deleteInterview(id, applicationId);

        //then
        then(interviewRepository).should().deleteByIdAndApplicationId(id, applicationId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting an interview that is not found")
    void shouldThrowResourceNotFoundWhenDeletingNonExistentInterview() {
        // given
        Long applicationId = 50L;
        Long id = 99L;
        given(interviewRepository.deleteByIdAndApplicationId(id, applicationId)).willReturn(0L);

        // when / then
        assertThatThrownBy(() -> interviewService.deleteInterview(id, applicationId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    }
