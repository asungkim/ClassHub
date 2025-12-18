package com.classhub.domain.member.dto.request;

import com.classhub.domain.member.model.StudentGrade;
import com.classhub.domain.member.support.PhoneNumberNormalizer;
import com.classhub.domain.member.support.SchoolNameFormatter;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegisterStudentRequest(
        @Valid
        @JsonUnwrapped
        RegisterMemberRequest member,

        @NotBlank
        @Size(max = 60)
        String schoolName,

        @NotNull
        StudentGrade grade,

        @NotNull
        @Past
        LocalDate birthDate,

        @NotBlank
        @Pattern(regexp = "^[0-9\\-]+$", message = "전화번호는 숫자와 '-'만 사용할 수 있습니다.")
        @Size(min = 10, max = 13)
        String parentPhone
) {

    public RegisterMemberRequest memberRequest() {
        return member;
    }

    public String normalizedEmail() {
        return member.normalizedEmail();
    }

    public String normalizedPhoneNumber() {
        return member.normalizedPhoneNumber();
    }

    public String formattedSchoolName() {
        return SchoolNameFormatter.format(schoolName);
    }

    public String normalizedParentPhone() {
        return PhoneNumberNormalizer.normalize(parentPhone);
    }
}
