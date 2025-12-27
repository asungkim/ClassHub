package com.classhub.domain.course.scheduler;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.classhub.domain.course.application.CourseArchiveService;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseArchiveSchedulerTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Mock
    private CourseArchiveService courseArchiveService;

    @InjectMocks
    private CourseArchiveScheduler scheduler;

    @Test
    void archiveExpiredCourses_shouldInvokeServiceWithKstDate() {
        LocalDate today = LocalDate.now(KST);

        scheduler.archiveExpiredCourses();

        verify(courseArchiveService).archiveExpiredCourses(eq(today));
    }
}
