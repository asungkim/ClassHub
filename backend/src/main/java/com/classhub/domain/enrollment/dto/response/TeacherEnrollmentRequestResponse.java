package com.classhub.domain.enrollment.dto.response;

import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.enrollment.model.EnrollmentStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record TeacherEnrollmentRequestResponse(
        UUID requestId,
        CourseResponse course,
        StudentSummary student,
        EnrollmentStatus status,
        String studentMessage,
        LocalDateTime processedAt,
        UUID processedByMemberId,
        LocalDateTime createdAt
) {

    public record StudentSummary(
            UUID memberId,
            String name,
            String email,
            String phoneNumber,
            String schoolName,
            String grade,
            Integer age
    ) {
    }
}
