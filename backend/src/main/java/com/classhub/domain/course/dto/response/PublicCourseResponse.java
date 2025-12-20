package com.classhub.domain.course.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PublicCourseResponse(
        UUID courseId,
        UUID branchId,
        String branchName,
        UUID companyId,
        String companyName,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        List<CourseScheduleResponse> schedules,
        UUID teacherId,
        String teacherName,
        String scheduleSummary
) {

    public static PublicCourseResponse from(
            CourseResponse course,
            UUID teacherId,
            String teacherName,
            String scheduleSummary
    ) {
        return new PublicCourseResponse(
                course.courseId(),
                course.branchId(),
                course.branchName(),
                course.companyId(),
                course.companyName(),
                course.name(),
                course.description(),
                course.startDate(),
                course.endDate(),
                course.active(),
                course.schedules(),
                teacherId,
                teacherName,
                scheduleSummary
        );
    }
}
