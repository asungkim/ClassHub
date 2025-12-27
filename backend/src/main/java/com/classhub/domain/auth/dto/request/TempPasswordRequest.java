package com.classhub.domain.auth.dto.request;

import com.classhub.domain.member.support.PhoneNumberNormalizer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TempPasswordRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        @Pattern(regexp = "^[0-9\\-]+$", message = "전화번호는 숫자와 '-'만 사용할 수 있습니다.")
        @Size(min = 10, max = 13)
        String phoneNumber
) {
    public String normalizedEmail() {
        return email.trim().toLowerCase();
    }

    public String normalizedPhoneNumber() {
        return PhoneNumberNormalizer.normalize(phoneNumber);
    }
}
