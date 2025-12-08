package com.classhub.domain.studentprofile.dto.response;

import com.classhub.domain.studentprofile.model.StudentProfile;
import java.util.UUID;

public record StudentProfileSummary(
        UUID id,
        UUID courseId,
        String courseName,
        String name,
        String grade,
        String phoneNumber,
        UUID assistantId,
        String assistantName,
        UUID memberId,
        String parentPhone,
        Integer age,
        boolean active
) {

    public static StudentProfileSummary from(
            StudentProfile profile,
            String assistantName,
            String courseName
    ) {
        return new StudentProfileSummary(
                profile.getId(),
                profile.getCourseId(),
                courseName,
                profile.getName(),
                profile.getGrade(),
                profile.getPhoneNumber(),
                profile.getAssistantId(),
                assistantName,
                profile.getMemberId(),
                profile.getParentPhone(),
                profile.getAge(),
                profile.isActive()
        );
    }
}
