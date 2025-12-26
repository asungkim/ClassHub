package com.classhub.domain.assignment.dto.response;

import com.classhub.domain.member.dto.response.StudentSummaryResponse;
import java.util.List;

public record TeacherStudentDetailResponse(
        StudentSummaryResponse student,
        List<TeacherStudentCourseResponse> courses
) {
}
