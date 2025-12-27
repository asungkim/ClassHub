package com.classhub.domain.calendar.dto;

import com.classhub.domain.member.model.MemberRole;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record StudentCalendarResponse(
        UUID studentId,
        int year,
        int month,
        List<CourseProgressEvent> courseProgress,
        List<PersonalProgressEvent> personalProgress,
        List<ClinicEvent> clinicEvents
) {
    public record CourseProgressEvent(
            UUID id,
            UUID courseId,
            String courseName,
            LocalDate date,
            String title,
            String content,
            UUID writerId,
            String writerName,
            MemberRole writerRole,
            LocalDateTime createdAt
    ) {
    }

    public record PersonalProgressEvent(
            UUID id,
            UUID studentCourseRecordId,
            UUID courseId,
            String courseName,
            LocalDate date,
            String title,
            String content,
            String writerName,
            MemberRole writerRole,
            LocalDateTime createdAt
    ) {
    }

    public record ClinicEvent(
            UUID clinicSessionId,
            UUID clinicAttendanceId,
            UUID courseId,
            UUID slotId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            boolean canceled,
            ClinicRecordSummary recordSummary
    ) {
    }

    public record ClinicRecordSummary(
            UUID id,
            String title,
            String content,
            String homeworkProgress,
            String writerName,
            MemberRole writerRole,
            LocalDateTime createdAt
    ) {
    }
}
