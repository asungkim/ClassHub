package com.classhub.domain.clinic.clinicbatch.application;

import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClinicBatchScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ClinicBatchService clinicBatchService;

    @Scheduled(cron = "0 0 0 ? * SUN", zone = "Asia/Seoul")
    public void runWeeklyBatch() {
        LocalDate today = LocalDate.now(KST);
        clinicBatchService.generateWeeklySessions(today);
        clinicBatchService.generateWeeklyAttendances(today);
    }
}
