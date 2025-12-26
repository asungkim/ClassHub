package com.classhub.domain.studentcourse.dto.response;

import com.classhub.domain.course.dto.response.CourseResponse;
import java.time.LocalDateTime;
import java.util.UUID;

public record StudentMyCourseResponse(
        UUID assignmentId,
        LocalDateTime assignedAt,
        boolean assignmentActive,
        UUID recordId,
        CourseResponse course
) {
}
