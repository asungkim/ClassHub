package com.classhub.global.validator;

import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.LocalTime;

public final class ScheduleTimeRangeValidator {

    private static final LocalTime EARLIEST_START = LocalTime.of(6, 0);
    private static final LocalTime LATEST_END = LocalTime.of(22, 0);

    private ScheduleTimeRangeValidator() {
    }

    public static void validate(LocalTime startTime, LocalTime endTime, RsCode invalidCode) {
        if (startTime == null || endTime == null || invalidCode == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException(invalidCode);
        }
        if (startTime.isBefore(EARLIEST_START) || endTime.isAfter(LATEST_END)) {
            throw new BusinessException(invalidCode);
        }
    }
}
