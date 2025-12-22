package com.classhub.domain.clinic.attendance.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public interface ClinicAttendanceEventProjection {
    UUID getClinicSessionId();

    UUID getClinicAttendanceId();

    UUID getCourseId();

    UUID getSlotId();

    LocalDate getDate();

    LocalTime getStartTime();

    LocalTime getEndTime();

    boolean isCanceled();

    UUID getRecordId();

    String getRecordTitle();

    UUID getRecordWriterId();

    String getRecordContent();

    String getRecordHomeworkProgress();
}
