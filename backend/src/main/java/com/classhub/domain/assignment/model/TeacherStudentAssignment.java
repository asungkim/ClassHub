package com.classhub.domain.assignment.model;

import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "teacher_student_assignment",
        indexes = {
                @Index(name = "idx_tsa_teacher", columnList = "teacher_member_id"),
                @Index(name = "idx_tsa_student", columnList = "student_member_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeacherStudentAssignment extends BaseEntity {

    @Column(name = "teacher_member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID teacherMemberId;

    @Column(name = "student_member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID studentMemberId;

    @Builder
    private TeacherStudentAssignment(UUID teacherMemberId, UUID studentMemberId) {
        this.teacherMemberId = Objects.requireNonNull(teacherMemberId, "teacherMemberId must not be null");
        this.studentMemberId = Objects.requireNonNull(studentMemberId, "studentMemberId must not be null");
    }

    public static TeacherStudentAssignment create(UUID teacherMemberId, UUID studentMemberId) {
        return TeacherStudentAssignment.builder()
                .teacherMemberId(teacherMemberId)
                .studentMemberId(studentMemberId)
                .build();
    }

    public void disable() {
        delete();
    }

    public void enable() {
        restore();
    }

    public boolean isActive() {
        return !isDeleted();
    }
}
