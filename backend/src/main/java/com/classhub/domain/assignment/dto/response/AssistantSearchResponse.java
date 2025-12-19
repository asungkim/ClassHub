package com.classhub.domain.assignment.dto.response;

import com.classhub.domain.assignment.model.TeacherAssistantAssignment;
import com.classhub.domain.member.model.Member;
import java.time.LocalDateTime;
import java.util.UUID;

public record AssistantSearchResponse(
        UUID assistantMemberId,
        String name,
        String email,
        String phoneNumber,
        AssignmentStatus assignmentStatus,
        UUID assignmentId,
        LocalDateTime connectedAt,
        LocalDateTime disabledAt
) {

    public static AssistantSearchResponse from(
            Member assistant,
            TeacherAssistantAssignment assignment
    ) {
        if (assignment == null) {
            return new AssistantSearchResponse(
                    assistant.getId(),
                    assistant.getName(),
                    assistant.getEmail(),
                    assistant.getPhoneNumber(),
                    AssignmentStatus.NOT_ASSIGNED,
                    null,
                    null,
                    null
            );
        }

        AssignmentStatus status = assignment.isActive()
                ? AssignmentStatus.ACTIVE
                : AssignmentStatus.INACTIVE;

        return new AssistantSearchResponse(
                assistant.getId(),
                assistant.getName(),
                assistant.getEmail(),
                assistant.getPhoneNumber(),
                status,
                assignment.getId(),
                assignment.getCreatedAt(),
                assignment.getDeletedAt()
        );
    }

    public enum AssignmentStatus {
        NOT_ASSIGNED,
        ACTIVE,
        INACTIVE
    }
}
