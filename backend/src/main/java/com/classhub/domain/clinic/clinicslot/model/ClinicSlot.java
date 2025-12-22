package com.classhub.domain.clinic.clinicslot.model;

import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "clinic_slot",
        indexes = {
                @Index(name = "idx_clinic_slot_teacher", columnList = "teacher_member_id"),
                @Index(name = "idx_clinic_slot_creator", columnList = "creator_member_id"),
                @Index(name = "idx_clinic_slot_branch", columnList = "branch_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClinicSlot extends BaseEntity {

    @Column(name = "teacher_member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID teacherMemberId;

    @Column(name = "creator_member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID creatorMemberId;

    @Column(name = "branch_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID branchId;

    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "default_capacity", nullable = false)
    private Integer defaultCapacity;

    @Builder
    private ClinicSlot(UUID teacherMemberId,
                       UUID creatorMemberId,
                       UUID branchId,
                       DayOfWeek dayOfWeek,
                       LocalTime startTime,
                       LocalTime endTime,
                       Integer defaultCapacity) {
        this.teacherMemberId = Objects.requireNonNull(teacherMemberId, "teacherMemberId must not be null");
        this.creatorMemberId = Objects.requireNonNull(creatorMemberId, "creatorMemberId must not be null");
        this.branchId = Objects.requireNonNull(branchId, "branchId must not be null");
        this.dayOfWeek = Objects.requireNonNull(dayOfWeek, "dayOfWeek must not be null");
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        this.defaultCapacity = Objects.requireNonNull(defaultCapacity, "defaultCapacity must not be null");
    }

    public void updateSchedule(DayOfWeek dayOfWeek,
                               LocalTime startTime,
                               LocalTime endTime,
                               Integer defaultCapacity) {
        this.dayOfWeek = Objects.requireNonNull(dayOfWeek, "dayOfWeek must not be null");
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        this.defaultCapacity = Objects.requireNonNull(defaultCapacity, "defaultCapacity must not be null");
    }
}
