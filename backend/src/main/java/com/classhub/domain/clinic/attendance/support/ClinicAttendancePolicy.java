package com.classhub.domain.clinic.attendance.support;

import com.classhub.domain.clinic.session.model.ClinicSession;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

public final class ClinicAttendancePolicy {

    public static final int LOCK_MINUTES = 10;
    public static final int MOVE_LIMIT_MINUTES = 30;

    private ClinicAttendancePolicy() {
    }

    public static boolean isLocked(ClinicSession session, LocalDateTime now) {
        LocalDateTime sessionStart = LocalDateTime.of(session.getDate(), session.getStartTime());
        return !now.isBefore(sessionStart.minusMinutes(LOCK_MINUTES));
    }

    public static boolean isMoveAllowed(ClinicSession session, LocalDateTime now) {
        LocalDateTime sessionStart = LocalDateTime.of(session.getDate(), session.getStartTime());
        return now.isBefore(sessionStart.minusMinutes(MOVE_LIMIT_MINUTES));
    }

    public static WeekRange resolveWeek(LocalDate date) {
        LocalDate startOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return new WeekRange(startOfWeek, endOfWeek);
    }

    public record WeekRange(LocalDate startDate, LocalDate endDate) {
    }
}
