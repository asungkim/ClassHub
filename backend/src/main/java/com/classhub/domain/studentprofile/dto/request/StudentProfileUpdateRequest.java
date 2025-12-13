package com.classhub.domain.studentprofile.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record StudentProfileUpdateRequest(
        @Size(max = 60) String name,
        @Pattern(regexp = "^[0-9\\-]{7,20}$", message = "유효한 연락처를 입력하세요.") String parentPhone,
        @Size(max = 60) String schoolName,
        @Size(max = 20) String grade,
        UUID courseId,
        UUID assistantId,
        @Size(max = 40) String phoneNumber,
        UUID memberId,
        UUID defaultClinicSlotId,
        Integer age
) {
}
