package com.classhub.domain.studentcourse.model;

import com.classhub.global.entity.BaseEntity;
import com.classhub.global.util.KstTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "student_course_enrollment",
        indexes = {
                @Index(name = "idx_sce_student", columnList = "student_member_id"),
                @Index(name = "idx_sce_course", columnList = "course_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentCourseEnrollment extends BaseEntity {

    @Column(name = "student_member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID studentMemberId;

    @Column(name = "course_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID courseId;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt;

    @Builder
    private StudentCourseEnrollment(UUID studentMemberId,
                                    UUID courseId,
                                    LocalDateTime enrolledAt) {
        this.studentMemberId = Objects.requireNonNull(studentMemberId, "studentMemberId must not be null");
        this.courseId = Objects.requireNonNull(courseId, "courseId must not be null");
        this.enrolledAt = enrolledAt == null ? LocalDateTime.now(KstTime.clock()) : enrolledAt;
    }

    public static StudentCourseEnrollment create(UUID studentMemberId,
                                                 UUID courseId,
                                                 LocalDateTime enrolledAt) {
        return StudentCourseEnrollment.builder()
                .studentMemberId(studentMemberId)
                .courseId(courseId)
                .enrolledAt(enrolledAt)
                .build();
    }
}
