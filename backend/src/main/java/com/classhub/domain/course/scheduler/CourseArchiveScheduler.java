package com.classhub.domain.course.scheduler;

import com.classhub.domain.course.application.CourseArchiveService;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseArchiveScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final CourseArchiveService courseArchiveService;

    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
    public void archiveExpiredCourses() {
        LocalDate today = LocalDate.now(KST);
        courseArchiveService.archiveExpiredCourses(today);
    }
}
