package com.classhub.domain.clinic.clinicslot.dto.response;

import com.classhub.domain.clinic.clinicslot.model.ClinicSlot;
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
        Integer defaultCapacity
) {

    public static ClinicSlotResponse from(ClinicSlot slot) {
        return new ClinicSlotResponse(
                slot.getId(),
                slot.getTeacherMemberId(),
                slot.getCreatorMemberId(),
                slot.getBranchId(),
                slot.getDayOfWeek(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getDefaultCapacity()
        );
    }
}
