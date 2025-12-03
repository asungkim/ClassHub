package com.classhub.domain.studentprofile.dto.response;

import com.classhub.domain.studentprofile.model.StudentProfile;
import java.util.UUID;

public record StudentProfileSummary(
        UUID id,
        UUID courseId,
        String name,
        String phoneNumber,
        UUID assistantId,
        UUID memberId,
        String parentPhone,
        Integer age,
        boolean active
) {

    public static StudentProfileSummary from(StudentProfile profile) {
        return new StudentProfileSummary(
                profile.getId(),
                profile.getCourseId(),
                profile.getName(),
                profile.getPhoneNumber(),
                profile.getAssistantId(),
                profile.getMemberId(),
                profile.getParentPhone(),
                profile.getAge(),
                profile.isActive()
        );
    }
}
