package com.classhub.domain.worklog.model;

import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
        name = "work_log",
        indexes = {
                @Index(name = "idx_work_log_assistant", columnList = "assistant_member_id"),
                @Index(name = "idx_work_log_date", columnList = "work_date")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkLog extends BaseEntity {

    @Column(name = "assistant_member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID assistantMemberId;

    @Column(name = "work_date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal hours;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Builder
    private WorkLog(UUID assistantMemberId,
                    LocalDate date,
                    LocalTime startTime,
                    LocalTime endTime,
                    BigDecimal hours,
                    String memo) {
        this.assistantMemberId = Objects.requireNonNull(assistantMemberId, "assistantMemberId must not be null");
        this.date = Objects.requireNonNull(date, "date must not be null");
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        this.hours = Objects.requireNonNull(hours, "hours must not be null");
        this.memo = memo;
    }
}
