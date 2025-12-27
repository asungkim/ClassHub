package com.classhub.domain.member.dto.response;

import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.model.StudentGrade;
import com.classhub.domain.member.model.StudentInfo;
import java.time.LocalDate;
import java.util.UUID;

public record MemberProfileResponse(
        MemberProfileInfo member,
        StudentInfoResponse studentInfo
) {

    public static MemberProfileResponse from(Member member, StudentInfo info) {
        return new MemberProfileResponse(
                new MemberProfileInfo(
                        member.getId(),
                        member.getEmail(),
                        member.getName(),
                        member.getPhoneNumber(),
                        member.getRole()
                ),
                info != null
                        ? new StudentInfoResponse(
                                info.getSchoolName(),
                                info.getGrade(),
                                info.getBirthDate(),
                                info.getParentPhone()
                        )
                        : null
        );
    }

    public record MemberProfileInfo(
            UUID memberId,
            String email,
            String name,
            String phoneNumber,
            MemberRole role
    ) {
    }

    public record StudentInfoResponse(
            String schoolName,
            StudentGrade grade,
            LocalDate birthDate,
            String parentPhone
    ) {
    }
}
