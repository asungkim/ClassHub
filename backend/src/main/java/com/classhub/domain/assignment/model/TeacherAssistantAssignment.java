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
        name = "teacher_assistant_assignment",
        indexes = {
                @Index(name = "idx_taa_teacher", columnList = "teacher_member_id"),
                @Index(name = "idx_taa_assistant", columnList = "assistant_member_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeacherAssistantAssignment extends BaseEntity {

    @Column(name = "teacher_member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID teacherMemberId;

    @Column(name = "assistant_member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID assistantMemberId;

    @Builder
    private TeacherAssistantAssignment(UUID teacherMemberId, UUID assistantMemberId) {
        this.teacherMemberId = Objects.requireNonNull(teacherMemberId, "teacherMemberId must not be null");
        this.assistantMemberId = Objects.requireNonNull(assistantMemberId, "assistantMemberId must not be null");
    }

    public static TeacherAssistantAssignment create(UUID teacherMemberId, UUID assistantMemberId) {
        return TeacherAssistantAssignment.builder()
                .teacherMemberId(teacherMemberId)
                .assistantMemberId(assistantMemberId)
                .build();
    }
}
