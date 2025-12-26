package com.classhub.domain.clinic.slot.dto.response;

import com.classhub.domain.clinic.slot.model.ClinicSlot;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record ClinicSlotResponse(
        UUID slotId,
        UUID teacherMemberId,
        UUID creatorMemberId,
        UUID branchId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer defaultCapacity,
        Long defaultAssignedCount
) {

    public static ClinicSlotResponse from(ClinicSlot slot) {
        return from(slot, 0L);
    }

    public static ClinicSlotResponse from(ClinicSlot slot, long defaultAssignedCount) {
        return new ClinicSlotResponse(
                slot.getId(),
                slot.getTeacherMemberId(),
                slot.getCreatorMemberId(),
                slot.getBranchId(),
                slot.getDayOfWeek(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getDefaultCapacity(),
                defaultAssignedCount
        );
    }
}
