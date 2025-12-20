package com.classhub.domain.course.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CourseResponse(
        UUID courseId,
        UUID branchId,
        String branchName,
        UUID companyId,
        String companyName,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        List<CourseScheduleResponse> schedules
) {
}
