package com.classhub.domain.assignment.dto.response;

import com.classhub.domain.assignment.model.TeacherAssistantAssignment;
import com.classhub.domain.member.model.Member;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.LocalDateTime;
import java.util.UUID;

public record AssistantAssignmentResponse(
        UUID assignmentId,
        AssistantProfile assistant,
        boolean isActive,
        LocalDateTime assignedAt,
        LocalDateTime disabledAt
) {

    public static AssistantAssignmentResponse from(
            TeacherAssistantAssignment assignment,
            Member assistantMember
    ) {
        if (assistantMember == null) {
            throw new BusinessException(RsCode.MEMBER_NOT_FOUND);
        }
        AssistantProfile profile = new AssistantProfile(
                assistantMember.getId(),
                assistantMember.getName(),
                assistantMember.getEmail(),
                assistantMember.getPhoneNumber()
        );
        return new AssistantAssignmentResponse(
                assignment.getId(),
                profile,
                assignment.isActive(),
                assignment.getCreatedAt(),
                assignment.getDeletedAt()
        );
    }

    public record AssistantProfile(
            UUID memberId,
            String name,
            String email,
            String phoneNumber
    ) {
    }
}
