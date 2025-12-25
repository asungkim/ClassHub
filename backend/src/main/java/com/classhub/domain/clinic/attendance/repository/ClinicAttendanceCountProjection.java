package com.classhub.domain.clinic.attendance.repository;

import java.util.UUID;

public interface ClinicAttendanceCountProjection {

    UUID getClinicSessionId();

    Long getAttendanceCount();
}
