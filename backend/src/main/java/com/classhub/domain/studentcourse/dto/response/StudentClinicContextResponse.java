package com.classhub.domain.studentcourse.dto.response;

import java.util.UUID;

public record StudentClinicContextResponse(
        UUID courseId,
        String courseName,
        UUID recordId,
        UUID defaultClinicSlotId,
        UUID teacherId,
        String teacherName,
        UUID branchId,
        String branchName,
        UUID companyId,
        String companyName
) {
}
