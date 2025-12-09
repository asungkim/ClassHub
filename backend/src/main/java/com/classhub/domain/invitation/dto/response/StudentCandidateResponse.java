package com.classhub.domain.invitation.dto.response;

import com.classhub.domain.studentprofile.model.StudentProfile;
import java.util.UUID;

public record StudentCandidateResponse(
        UUID id,
        String name,
        String phoneNumber,
        String parentPhone,
        String schoolName,
        String grade,
        Integer age
) {
    public static StudentCandidateResponse from(StudentProfile profile) {
        return new StudentCandidateResponse(
                profile.getId(),
                profile.getName(),
                profile.getPhoneNumber(),
                profile.getParentPhone(),
                profile.getSchoolName(),
                profile.getGrade(),
                profile.getAge()
        );
    }
}
