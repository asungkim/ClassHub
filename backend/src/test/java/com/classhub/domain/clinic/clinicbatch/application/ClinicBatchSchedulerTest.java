package com.classhub.domain.clinic.clinicbatch.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClinicBatchSchedulerTest {

    @Mock
    private ClinicBatchService clinicBatchService;

    @InjectMocks
    private ClinicBatchScheduler clinicBatchScheduler;

    @Test
    void runWeeklyBatch_shouldTriggerBatchService() {
        clinicBatchScheduler.runWeeklyBatch();

        verify(clinicBatchService).generateWeeklySessions(any(LocalDate.class));
        verify(clinicBatchService).generateWeeklyAttendances(any(LocalDate.class));
    }
}
