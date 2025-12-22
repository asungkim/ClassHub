package com.classhub.domain.clinic.clinicattendance.dto.response;

import com.classhub.domain.clinic.clinicattendance.model.ClinicAttendance;
import java.util.UUID;

public record ClinicAttendanceResponse(
        UUID attendanceId,
        UUID clinicSessionId,
        UUID studentCourseRecordId
) {

    public static ClinicAttendanceResponse from(ClinicAttendance attendance) {
        return new ClinicAttendanceResponse(
                attendance.getId(),
                attendance.getClinicSessionId(),
                attendance.getStudentCourseRecordId()
        );
    }
}
