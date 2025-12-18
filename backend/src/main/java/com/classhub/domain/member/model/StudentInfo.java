package com.classhub.domain.member.model;

import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "student_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class StudentInfo extends BaseEntity {

    @Column(name = "member_id", nullable = false, unique = true, columnDefinition = "BINARY(16)")
    private UUID memberId;

    @Column(nullable = false, length = 60)
    private String schoolName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudentGrade grade;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 20)
    private String parentPhone;

    public static StudentInfo create(
            Member member,
            String schoolName,
            StudentGrade grade,
            LocalDate birthDate,
            String parentPhone
    ) {
        return StudentInfo.builder()
                .memberId(member.getId())
                .schoolName(schoolName)
                .grade(grade)
                .birthDate(birthDate)
                .parentPhone(parentPhone)
                .build();
    }
}
