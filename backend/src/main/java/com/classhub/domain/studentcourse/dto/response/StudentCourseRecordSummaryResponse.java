package com.classhub.domain.studentcourse.dto.response;

import java.util.UUID;

public record StudentCourseRecordSummaryResponse(
        UUID recordId,
        UUID courseId,
        UUID assistantMemberId,
        UUID defaultClinicSlotId,
        String teacherNotes,
        boolean active
) {
}
