package com.classhub.domain.studentcourse.dto.request;

import jakarta.annotation.Nullable;
import java.util.UUID;

public record StudentCourseRecordUpdateRequest(
        @Nullable UUID assistantMemberId,
        @Nullable UUID defaultClinicSlotId,
        @Nullable String teacherNotes
) {
}
