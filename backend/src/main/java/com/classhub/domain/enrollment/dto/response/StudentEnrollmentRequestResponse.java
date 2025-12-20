package com.classhub.domain.enrollment.dto.response;

import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.enrollment.model.EnrollmentStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record StudentEnrollmentRequestResponse(
        UUID requestId,
        CourseResponse course,
        EnrollmentStatus status,
        String message,
        LocalDateTime processedAt,
        UUID processedByMemberId,
        LocalDateTime createdAt
) {
}
