package com.classhub.domain.course.dto.request;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

public record CourseUpdateRequest(
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        @Valid List<CourseScheduleRequest> schedules
) {
}
