package com.classhub.domain.studentcourse.model;

import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
        name = "student_course_assignment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_sca_student_course", columnNames = {"student_member_id", "course_id"})
        },
        indexes = {
                @Index(name = "idx_sca_student", columnList = "student_member_id"),
                @Index(name = "idx_sca_course", columnList = "course_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentCourseAssignment extends BaseEntity {

    @Column(name = "student_member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID studentMemberId;

    @Column(name = "course_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID courseId;

    @Column(name = "assigned_by_member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID assignedByMemberId;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Builder
    private StudentCourseAssignment(UUID studentMemberId,
                                    UUID courseId,
                                    UUID assignedByMemberId,
                                    LocalDateTime assignedAt) {
        this.studentMemberId = Objects.requireNonNull(studentMemberId, "studentMemberId must not be null");
        this.courseId = Objects.requireNonNull(courseId, "courseId must not be null");
        this.assignedByMemberId = Objects.requireNonNull(assignedByMemberId, "assignedByMemberId must not be null");
        this.assignedAt = assignedAt == null ? LocalDateTime.now() : assignedAt;
    }

    public static StudentCourseAssignment create(UUID studentMemberId,
                                                 UUID courseId,
                                                 UUID assignedByMemberId,
                                                 LocalDateTime assignedAt) {
        return StudentCourseAssignment.builder()
                .studentMemberId(studentMemberId)
                .courseId(courseId)
                .assignedByMemberId(assignedByMemberId)
                .assignedAt(assignedAt)
                .build();
    }

    public void deactivate() {
        delete();
    }

    public void activate() {
        restore();
    }

    public boolean isActive() {
        return !isDeleted();
    }
}
