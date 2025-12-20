package com.classhub.domain.studentcourse.dto.response;

import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.member.dto.response.StudentSummaryResponse;
import java.util.UUID;

public record StudentCourseDetailResponse(
        UUID recordId,
        StudentSummaryResponse student,
        CourseResponse course,
        UUID assistantMemberId,
        UUID defaultClinicSlotId,
        String teacherNotes,
        boolean active
) {
}
