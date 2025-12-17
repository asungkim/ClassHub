package com.classhub.domain.studentprofile.dto.response;

import com.classhub.domain.studentprofile.model.StudentProfile;
import java.util.List;
import java.util.UUID;

public record StudentProfileSummary(
        UUID id,
        List<String> courseNames,
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

    public static StudentProfileSummary of(
            StudentProfile profile,
            String assistantName,
            List<String> courseNames
    ) {
        return new StudentProfileSummary(
                profile.getId(),
                courseNames,
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
