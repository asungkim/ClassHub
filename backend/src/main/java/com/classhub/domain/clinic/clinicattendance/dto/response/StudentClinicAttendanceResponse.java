package com.classhub.domain.clinic.clinicattendance.dto.response;

import com.classhub.domain.clinic.clinicsession.model.ClinicSession;
import com.classhub.domain.clinic.clinicsession.model.ClinicSessionType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record StudentClinicAttendanceResponse(
        UUID attendanceId,
        UUID clinicSessionId,
        UUID teacherId,
        UUID branchId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        ClinicSessionType sessionType,
        boolean isCanceled
) {

    public static StudentClinicAttendanceResponse from(ClinicAttendanceResponse attendance,
                                                       ClinicSession session) {
        return new StudentClinicAttendanceResponse(
                attendance.attendanceId(),
                attendance.clinicSessionId(),
                session.getTeacherMemberId(),
                session.getBranchId(),
                session.getDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getSessionType(),
                session.isCanceled()
        );
    }
}
