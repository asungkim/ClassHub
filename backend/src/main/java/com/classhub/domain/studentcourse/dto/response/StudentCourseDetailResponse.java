package com.classhub.domain.studentcourse.dto.response;

import com.classhub.domain.course.dto.response.CourseResponse;
import java.time.LocalDate;
import java.util.UUID;

public record StudentCourseDetailResponse(
        UUID recordId,
        StudentSummary student,
        CourseResponse course,
        UUID assistantMemberId,
        UUID defaultClinicSlotId,
        String teacherNotes,
        boolean active
) {

    public record StudentSummary(
            UUID memberId,
            String name,
            String email,
            String phoneNumber,
            String schoolName,
            String grade,
            LocalDate birthDate,
            Integer age,
            String parentPhone
    ) {
    }
}
