package com.classhub.domain.studentcourseenrollment.model;

import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "student_course_enrollment",
        indexes = {
                @Index(name = "idx_enrollment_student", columnList = "student_profile_id"),
                @Index(name = "idx_enrollment_course", columnList = "course_id"),
                @Index(name = "idx_enrollment_teacher", columnList = "teacher_id")
        },
        uniqueConstraints = {
                @jakarta.persistence.UniqueConstraint(
                        name = "uk_enrollment_student_course",
                        columnNames = {"student_profile_id", "course_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class StudentCourseEnrollment extends BaseEntity {

    @Column(name = "student_profile_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID studentProfileId;

    @Column(name = "course_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID courseId;

    @Column(name = "teacher_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID teacherId;
}
