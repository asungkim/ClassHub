package com.classhub.domain.studentcourse.dto.response;

import com.classhub.domain.member.dto.response.StudentSummaryResponse;
import java.util.List;
import java.util.UUID;

public record StudentStudentListItemResponse(
        StudentSummaryResponse student,
        boolean active,
        List<UUID> activeCourseIds,
        List<String> activeCourseNames
) {
}
