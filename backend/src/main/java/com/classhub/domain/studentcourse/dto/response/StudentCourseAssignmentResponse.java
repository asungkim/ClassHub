package com.classhub.domain.studentcourse.dto.response;

import com.classhub.domain.studentcourse.model.StudentCourseAssignment;
import java.time.LocalDateTime;
import java.util.UUID;

public record StudentCourseAssignmentResponse(
        UUID assignmentId,
        UUID studentId,
        UUID courseId,
        UUID assignedByMemberId,
        LocalDateTime assignedAt,
        boolean active
) {
    public static StudentCourseAssignmentResponse from(StudentCourseAssignment assignment) {
        return new StudentCourseAssignmentResponse(
                assignment.getId(),
                assignment.getStudentMemberId(),
                assignment.getCourseId(),
                assignment.getAssignedByMemberId(),
                assignment.getAssignedAt(),
                assignment.isActive()
        );
    }
}
