package com.classhub.domain.enrollment.dto.response;

import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.enrollment.model.EnrollmentStatus;
import com.classhub.domain.member.dto.response.StudentSummaryResponse;
import java.time.LocalDateTime;
import java.util.UUID;

public record TeacherEnrollmentRequestResponse(
        UUID requestId,
        CourseResponse course,
        StudentSummaryResponse student,
        EnrollmentStatus status,
        String studentMessage,
        LocalDateTime processedAt,
        UUID processedByMemberId,
        LocalDateTime createdAt
) {
}
