package com.classhub.domain.clinic.clinicslot.model;

import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "clinic_slot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class ClinicSlot extends BaseEntity {

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 20)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    // Business Methods
    public void updateSlot(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, Integer capacity) {
        if (dayOfWeek != null) this.dayOfWeek = dayOfWeek;
        if (startTime != null) this.startTime = startTime;
        if (endTime != null) this.endTime = endTime;
        if (capacity != null) this.capacity = capacity;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }
}
