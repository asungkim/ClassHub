package com.classhub.domain.clinic.session.dto.response;

import com.classhub.domain.clinic.session.model.ClinicSession;
import com.classhub.domain.clinic.session.model.ClinicSessionType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ClinicSessionResponse(
        UUID sessionId,
        UUID slotId,
        UUID teacherMemberId,
        UUID branchId,
        ClinicSessionType sessionType,
        UUID creatorMemberId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        Integer capacity,
        boolean isCanceled
) {

    public static ClinicSessionResponse from(ClinicSession session) {
        return new ClinicSessionResponse(
                session.getId(),
                session.getSlotId(),
                session.getTeacherMemberId(),
                session.getBranchId(),
                session.getSessionType(),
                session.getCreatorMemberId(),
                session.getDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getCapacity(),
                session.isCanceled()
        );
    }
}
