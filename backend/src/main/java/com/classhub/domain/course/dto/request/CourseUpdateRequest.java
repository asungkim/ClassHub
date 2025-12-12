package com.classhub.domain.course.dto.request;

import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record CourseUpdateRequest(
        @Size(max = 100)
        String name,

        @Size(max = 100)
        String company,

        @Size(min = 1)
        Set<DayOfWeek> daysOfWeek,

        LocalTime startTime,

        LocalTime endTime
) {
}
