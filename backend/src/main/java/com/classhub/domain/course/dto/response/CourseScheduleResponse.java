package com.classhub.domain.course.dto.response;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record CourseScheduleResponse(
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
}
