package com.classhub.domain.studentcourse.dto.response;

import com.classhub.domain.course.dto.response.CourseResponse;
import java.time.LocalDateTime;
import java.util.UUID;

public record StudentCourseResponse(
        UUID enrollmentId,
        LocalDateTime enrolledAt,
        CourseResponse course
) {
}
