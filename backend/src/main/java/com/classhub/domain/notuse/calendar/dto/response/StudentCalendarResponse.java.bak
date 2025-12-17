package com.classhub.domain.calendar.dto.response;

import java.util.List;
import java.util.UUID;

public record StudentCalendarResponse(
        int schemaVersion,
        UUID studentId,
        int year,
        int month,
        List<CalendarSharedLessonDto> sharedLessons,
        List<CalendarPersonalLessonDto> personalLessons,
        List<CalendarClinicRecordDto> clinicRecords
) {

    public static StudentCalendarResponse of(
            UUID studentId,
            int year,
            int month,
            List<CalendarSharedLessonDto> sharedLessons,
            List<CalendarPersonalLessonDto> personalLessons,
            List<CalendarClinicRecordDto> clinicRecords
    ) {
        return new StudentCalendarResponse(
                1,
                studentId,
                year,
                month,
                List.copyOf(sharedLessons),
                List.copyOf(personalLessons),
                List.copyOf(clinicRecords)
        );
    }
}
