package com.hubz.application.service;

import com.hubz.application.dto.response.BackgroundJobResponse;
import com.hubz.application.port.in.JobExecutor;
import com.hubz.application.port.out.BackgroundJobRepositoryPort;
import com.hubz.domain.enums.JobStatus;
import com.hubz.domain.enums.JobType;
import com.hubz.domain.exception.BackgroundJobNotFoundException;
import com.hubz.domain.model.BackgroundJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BackgroundJobService Unit Tests")
class BackgroundJobServiceTest {

    @Mock
    private BackgroundJobRepositoryPort jobRepository;

    @Mock
    private JobExecutor emailExecutor;

    private BackgroundJobService backgroundJobService;

    @BeforeEach
    void setUp() {
        lenient().when(emailExecutor.getJobType()).thenReturn(JobType.EMAIL_SEND);
        backgroundJobService = new BackgroundJobService(jobRepository, List.of(emailExecutor));
    }

    @Test
    @DisplayName("Should schedule a new job with PENDING status")
    void shouldScheduleJob() {
        // Arrange
        String payload = "{\"emailType\":\"WELCOME\",\"to\":\"test@test.com\"}";
        when(jobRepository.save(any(BackgroundJob.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        BackgroundJobResponse response = backgroundJobService.scheduleJob(JobType.EMAIL_SEND, payload);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(JobType.EMAIL_SEND);
        assertThat(response.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(response.getPayload()).isEqualTo(payload);
        assertThat(response.getRetryCount()).isEqualTo(0);

        ArgumentCaptor<BackgroundJob> captor = ArgumentCaptor.forClass(BackgroundJob.class);
        verify(jobRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isNotNull();
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should execute a job successfully")
    void shouldExecuteJobSuccessfully() throws Exception {
        // Arrange
        UUID jobId = UUID.randomUUID();
        String payload = "{\"emailType\":\"WELCOME\",\"to\":\"test@test.com\"}";
        BackgroundJob job = BackgroundJob.builder()
                .id(jobId)
                .type(JobType.EMAIL_SEND)
                .status(JobStatus.PENDING)
                .payload(payload)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(BackgroundJob.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emailExecutor).execute(payload);

        // Act
        backgroundJobService.executeJob(jobId);

        // Assert
        verify(emailExecutor).execute(payload);
        // save called 3 times: markRunning, then markCompleted
        // Actually: findById returns the job, then save(running), then execute, then save(completed)
        ArgumentCaptor<BackgroundJob> captor = ArgumentCaptor.forClass(BackgroundJob.class);
        verify(jobRepository, atLeast(2)).save(captor.capture());

        List<BackgroundJob> savedJobs = captor.getAllValues();
        // Last saved job should be COMPLETED
        BackgroundJob lastSaved = savedJobs.get(savedJobs.size() - 1);
        assertThat(lastSaved.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(lastSaved.getExecutedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should mark job as FAILED when executor throws exception")
    void shouldMarkJobAsFailedOnExecutorError() throws Exception {
        // Arrange
        UUID jobId = UUID.randomUUID();
        BackgroundJob job = BackgroundJob.builder()
                .id(jobId)
                .type(JobType.EMAIL_SEND)
                .status(JobStatus.PENDING)
                .payload("{}")
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(BackgroundJob.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("SMTP connection failed")).when(emailExecutor).execute(any());

        // Act
        backgroundJobService.executeJob(jobId);

        // Assert
        ArgumentCaptor<BackgroundJob> captor = ArgumentCaptor.forClass(BackgroundJob.class);
        verify(jobRepository, atLeast(2)).save(captor.capture());

        BackgroundJob lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(lastSaved.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(lastSaved.getError()).isEqualTo("SMTP connection failed");
        assertThat(lastSaved.getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should mark job as FAILED when no executor found for job type")
    void shouldFailWhenNoExecutorFound() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        BackgroundJob job = BackgroundJob.builder()
                .id(jobId)
                .type(JobType.WEBHOOK_CALL) // No executor registered for this type in this test
                .status(JobStatus.PENDING)
                .payload("{}")
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(BackgroundJob.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        backgroundJobService.executeJob(jobId);

        // Assert
        ArgumentCaptor<BackgroundJob> captor = ArgumentCaptor.forClass(BackgroundJob.class);
        verify(jobRepository).save(captor.capture());

        BackgroundJob saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(saved.getError()).contains("No executor found");
    }

    @Test
    @DisplayName("Should throw exception when executing non-existent job")
    void shouldThrowWhenJobNotFound() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> backgroundJobService.executeJob(jobId))
                .isInstanceOf(BackgroundJobNotFoundException.class);
    }

    @Test
    @DisplayName("Should retry failed jobs that haven't exceeded max retries")
    void shouldRetryFailedJobs() {
        // Arrange
        BackgroundJob failedJob1 = BackgroundJob.builder()
                .id(UUID.randomUUID())
                .type(JobType.EMAIL_SEND)
                .status(JobStatus.FAILED)
                .retryCount(1)
                .createdAt(LocalDateTime.now())
                .build();

        BackgroundJob failedJob2 = BackgroundJob.builder()
                .id(UUID.randomUUID())
                .type(JobType.WEBHOOK_CALL)
                .status(JobStatus.FAILED)
                .retryCount(2)
                .createdAt(LocalDateTime.now())
                .build();

        when(jobRepository.findFailedJobsForRetry(BackgroundJob.MAX_RETRIES))
                .thenReturn(List.of(failedJob1, failedJob2));
        when(jobRepository.save(any(BackgroundJob.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        int count = backgroundJobService.retryFailedJobs();

        // Assert
        assertThat(count).isEqualTo(2);
        verify(jobRepository, times(2)).save(any(BackgroundJob.class));
    }

    @Test
    @DisplayName("Should retry a specific failed job")
    void shouldRetrySpecificJob() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        BackgroundJob failedJob = BackgroundJob.builder()
                .id(jobId)
                .type(JobType.EMAIL_SEND)
                .status(JobStatus.FAILED)
                .retryCount(1)
                .createdAt(LocalDateTime.now())
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(failedJob));
        when(jobRepository.save(any(BackgroundJob.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        BackgroundJobResponse response = backgroundJobService.retryJob(jobId);

        // Assert
        assertThat(response.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(response.getError()).isNull();
    }

    @Test
    @DisplayName("Should throw when retrying a job that cannot be retried")
    void shouldThrowWhenRetryingNonRetryableJob() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        BackgroundJob completedJob = BackgroundJob.builder()
                .id(jobId)
                .type(JobType.EMAIL_SEND)
                .status(JobStatus.COMPLETED)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(completedJob));

        // Act & Assert
        assertThatThrownBy(() -> backgroundJobService.retryJob(jobId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be retried");
    }

    @Test
    @DisplayName("Should clean up old jobs")
    void shouldCleanupOldJobs() {
        // Arrange
        when(jobRepository.deleteByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(15);

        // Act
        int deleted = backgroundJobService.cleanupOldJobs();

        // Assert
        assertThat(deleted).isEqualTo(15);
        verify(jobRepository).deleteByCreatedAtBefore(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should return all jobs for admin listing")
    void shouldReturnAllJobs() {
        // Arrange
        BackgroundJob job1 = BackgroundJob.builder()
                .id(UUID.randomUUID())
                .type(JobType.EMAIL_SEND)
                .status(JobStatus.COMPLETED)
                .payload("{}")
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .executedAt(LocalDateTime.now())
                .build();

        BackgroundJob job2 = BackgroundJob.builder()
                .id(UUID.randomUUID())
                .type(JobType.DATA_CLEANUP)
                .status(JobStatus.PENDING)
                .payload("{}")
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        when(jobRepository.findAll()).thenReturn(List.of(job1, job2));

        // Act
        List<BackgroundJobResponse> jobs = backgroundJobService.getAllJobs();

        // Assert
        assertThat(jobs).hasSize(2);
        assertThat(jobs.get(0).getType()).isEqualTo(JobType.EMAIL_SEND);
        assertThat(jobs.get(1).getType()).isEqualTo(JobType.DATA_CLEANUP);
    }
}
