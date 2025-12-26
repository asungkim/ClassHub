package com.classhub.domain.course.dto.response;

import com.classhub.domain.member.dto.response.StudentSummaryResponse;
import java.util.UUID;

public record CourseStudentResponse(
        UUID recordId,
        boolean assignmentActive,
        StudentSummaryResponse student
) {
}
