package com.classhub.domain.course.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classhub.global.exception.BusinessException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CoursePeriodValidatorTest {

    @Test
    void validate_shouldPassWhenStartBeforeOrEqualsEnd() {
        assertThatCode(() -> CoursePeriodValidator.validate(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 3, 31)
        )).doesNotThrowAnyException();
    }

    @Test
    void validate_shouldThrowWhenEndBeforeStart() {
        assertThatThrownBy(() -> CoursePeriodValidator.validate(
                LocalDate.of(2025, 4, 1),
                LocalDate.of(2025, 3, 31)
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void validate_shouldThrowWhenStartDateMissing() {
        assertThatThrownBy(() -> CoursePeriodValidator.validate(
                null,
                LocalDate.of(2025, 3, 31)
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void validate_shouldThrowWhenEndDateMissing() {
        assertThatThrownBy(() -> CoursePeriodValidator.validate(
                LocalDate.of(2025, 1, 1),
                null
        )).isInstanceOf(BusinessException.class);
    }
}
