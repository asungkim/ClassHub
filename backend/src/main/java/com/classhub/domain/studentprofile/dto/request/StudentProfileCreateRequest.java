package com.classhub.domain.studentprofile.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record StudentProfileCreateRequest(
        @NotEmpty List<UUID> courseIds,
        @NotBlank @Size(max = 60) String name,
        @NotBlank @Size(max = 40) String phoneNumber,
        UUID assistantId,
        @NotBlank @Pattern(regexp = "^[0-9\\-]{7,20}$") String parentPhone,
        @NotBlank @Size(max = 60) String schoolName,
        @NotBlank @Size(max = 20) String grade,
        @NotNull Integer age,
        UUID defaultClinicSlotId
) {

    public String normalizedPhoneNumber() {
        return phoneNumber.trim();
    }

    public String normalizedName() {
        return name.trim();
    }
}
