package com.classhub.domain.studentcourse.dto.response;

import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.member.dto.response.StudentSummaryResponse;
import java.util.List;

public record StudentStudentDetailResponse(
        StudentSummaryResponse student,
        List<CourseResponse> courses,
        List<StudentCourseRecordSummaryResponse> records
) {
}
