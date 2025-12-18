package com.classhub.domain.member.dto.request;

import com.classhub.domain.member.support.PhoneNumberNormalizer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterTeacherRequest(
        @NotBlank @Email String email,

        @Size(min = 8, max = 64) @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-={}:;\"'`~<>,.?/\\\\|\\[\\]]).{8,64}$", message = "비밀번호는 8자 이상이며 영문/숫자/특수문자를 모두 포함해야 합니다.") String password,

        @NotBlank @Size(max = 60) String name,

        @NotBlank @Pattern(regexp = "^[0-9\\-]+$", message = "전화번호는 숫자와 '-'만 사용할 수 있습니다.") @Size(min = 10, max = 13) String phoneNumber) {

    public String normalizedEmail() {
        return email.trim().toLowerCase();
    }

    public String normalizedPhoneNumber() {
        return PhoneNumberNormalizer.normalize(phoneNumber);
    }
}
