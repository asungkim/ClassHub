package com.classhub.domain.member.dto.request;

import com.classhub.domain.member.model.StudentGrade;
import com.classhub.domain.member.support.PhoneNumberNormalizer;
import com.classhub.domain.member.support.SchoolNameFormatter;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record StudentInfoUpdateRequest(
        @Size(max = 60)
        String schoolName,

        StudentGrade grade,

        @Past
        LocalDate birthDate,

        @Pattern(regexp = "^[0-9\\-]+$", message = "전화번호는 숫자와 '-'만 사용할 수 있습니다.")
        @Size(min = 10, max = 13)
        String parentPhone
) {
    public String formattedSchoolName() {
        if (schoolName == null) {
            return null;
        }
        return SchoolNameFormatter.format(schoolName);
    }

    public String normalizedParentPhone() {
        if (parentPhone == null) {
            return null;
        }
        return PhoneNumberNormalizer.normalize(parentPhone);
    }
}
