package com.classhub.domain.clinic.session.model;

import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
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
        name = "clinic_session",
        indexes = {
                @Index(name = "idx_clinic_session_slot", columnList = "slot_id"),
                @Index(name = "idx_clinic_session_date", columnList = "session_date"),
                @Index(name = "idx_clinic_session_type", columnList = "session_type"),
                @Index(name = "idx_clinic_session_creator", columnList = "creator_member_id"),
                @Index(name = "idx_clinic_session_teacher", columnList = "teacher_member_id"),
                @Index(name = "idx_clinic_session_branch", columnList = "branch_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClinicSession extends BaseEntity {

    @Column(name = "slot_id", columnDefinition = "BINARY(16)")
    private UUID slotId;

    @Column(name = "teacher_member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID teacherMemberId;

    @Column(name = "branch_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID branchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 20)
    private ClinicSessionType sessionType;

    @Column(name = "creator_member_id", columnDefinition = "BINARY(16)")
    private UUID creatorMemberId;

    @Column(name = "session_date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "is_canceled", nullable = false)
    private boolean canceled;

    @Version
    @Column(nullable = false)
    private Long version;

    @Builder
    private ClinicSession(UUID slotId,
                          UUID teacherMemberId,
                          UUID branchId,
                          ClinicSessionType sessionType,
                          UUID creatorMemberId,
                          LocalDate date,
                          LocalTime startTime,
                          LocalTime endTime,
                          Integer capacity,
                          boolean canceled) {
        this.slotId = slotId;
        this.teacherMemberId = Objects.requireNonNull(teacherMemberId, "teacherMemberId must not be null");
        this.branchId = Objects.requireNonNull(branchId, "branchId must not be null");
        this.sessionType = Objects.requireNonNull(sessionType, "sessionType must not be null");
        this.creatorMemberId = creatorMemberId;
        this.date = Objects.requireNonNull(date, "date must not be null");
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        this.capacity = Objects.requireNonNull(capacity, "capacity must not be null");
        this.canceled = canceled;
    }

    public void cancel() {
        this.canceled = true;
    }
}
