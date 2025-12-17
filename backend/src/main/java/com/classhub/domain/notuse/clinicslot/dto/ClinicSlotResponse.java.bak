package com.classhub.domain.clinic.clinicslot.dto;

import com.classhub.domain.clinic.clinicslot.model.ClinicSlot;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record ClinicSlotResponse(
    UUID id,
    UUID teacherId,
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    int capacity,
    boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ClinicSlotResponse from(ClinicSlot slot) {
        return new ClinicSlotResponse(
            slot.getId(),
            slot.getTeacherId(),
            slot.getDayOfWeek(),
            slot.getStartTime(),
            slot.getEndTime(),
            slot.getCapacity(),
            slot.isActive(),
            slot.getCreatedAt(),
            slot.getUpdatedAt()
        );
    }
}
