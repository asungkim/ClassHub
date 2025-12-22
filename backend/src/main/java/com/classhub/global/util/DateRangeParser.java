package com.classhub.global.util;

import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class DateRangeParser {

    private DateRangeParser() {
    }

    public static DateRange parse(String dateRange) {
        if (dateRange == null || dateRange.isBlank()) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        String[] parts = dateRange.split(",");
        if (parts.length != 2) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        try {
            LocalDate start = LocalDate.parse(parts[0].trim());
            LocalDate end = LocalDate.parse(parts[1].trim());
            if (start.isAfter(end)) {
                throw new BusinessException(RsCode.BAD_REQUEST);
            }
            return new DateRange(start, end);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
    }

    public record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}
