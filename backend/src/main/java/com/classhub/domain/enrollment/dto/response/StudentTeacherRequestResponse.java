package com.classhub.domain.enrollment.dto.response;

import com.classhub.domain.enrollment.model.TeacherStudentRequestStatus;
import com.classhub.domain.member.dto.response.StudentSummaryResponse;
import com.classhub.domain.member.dto.response.TeacherSearchResponse;
import java.time.LocalDateTime;
import java.util.UUID;

public record StudentTeacherRequestResponse(
        UUID requestId,
        TeacherSearchResponse teacher,
        StudentSummaryResponse student,
        TeacherStudentRequestStatus status,
        String message,
        LocalDateTime processedAt,
        UUID processedByMemberId,
        LocalDateTime createdAt
) {
}
