package com.classhub.domain.course.validator;

import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;

public final class CoursePeriodValidator {

    private CoursePeriodValidator() {
    }

    public static void validate(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        if (endDate.isBefore(startDate)) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
    }
}
