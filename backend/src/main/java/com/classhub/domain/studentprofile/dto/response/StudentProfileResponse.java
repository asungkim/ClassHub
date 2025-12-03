package com.classhub.domain.studentprofile.dto.response;

import com.classhub.domain.studentprofile.model.StudentProfile;
import java.time.LocalDateTime;
import java.util.UUID;

public record StudentProfileResponse(
        UUID id,
        UUID courseId,
        UUID teacherId,
        UUID assistantId,
        UUID memberId,
        String name,
        String phoneNumber,
        String parentPhone,
        String schoolName,
        String grade,
        UUID defaultClinicSlotId,
        Integer age,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static StudentProfileResponse from(StudentProfile profile) {
        return new StudentProfileResponse(
                profile.getId(),
                profile.getCourseId(),
                profile.getTeacherId(),
                profile.getAssistantId(),
                profile.getMemberId(),
                profile.getName(),
                profile.getPhoneNumber(),
                profile.getParentPhone(),
                profile.getSchoolName(),
                profile.getGrade(),
                profile.getDefaultClinicSlotId(),
                profile.getAge(),
                profile.isActive(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
