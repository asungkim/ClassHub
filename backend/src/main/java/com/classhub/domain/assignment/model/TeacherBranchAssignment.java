package com.classhub.domain.assignment.model;

import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "teacher_branch_assignment",
        indexes = {
                @Index(name = "idx_tba_teacher", columnList = "teacher_member_id"),
                @Index(name = "idx_tba_branch", columnList = "branch_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeacherBranchAssignment extends BaseEntity {

    @Column(name = "teacher_member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID teacherMemberId;

    @Column(name = "branch_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID branchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BranchRole role;

    @Builder
    private TeacherBranchAssignment(UUID teacherMemberId,
                                    UUID branchId,
                                    BranchRole role) {
        this.teacherMemberId = Objects.requireNonNull(teacherMemberId, "teacherMemberId must not be null");
        this.branchId = Objects.requireNonNull(branchId, "branchId must not be null");
        this.role = Objects.requireNonNull(role, "role must not be null");
    }

    public static TeacherBranchAssignment create(UUID teacherMemberId,
                                                 UUID branchId,
                                                 BranchRole role) {
        return TeacherBranchAssignment.builder()
                .teacherMemberId(teacherMemberId)
                .branchId(branchId)
                .role(role)
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
