package com.classhub.domain.studentcourse.dto.response;

import java.util.UUID;

public record StudentCourseListItemResponse(
        UUID recordId,
        UUID studentMemberId,
        String studentName,
        String phoneNumber,
        String parentPhoneNumber,
        String schoolName,
        String grade,
        Integer age,
        UUID courseId,
        String courseName,
        boolean active,
        UUID assistantMemberId,
        UUID defaultClinicSlotId
) {
}
