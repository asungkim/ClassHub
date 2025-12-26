package com.classhub.domain.assignment.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record TeacherStudentCourseResponse(
        UUID courseId,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        UUID assignmentId,
        Boolean assignmentActive,
        UUID recordId
) {
}
