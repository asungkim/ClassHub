package com.classhub.domain.course.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classhub.domain.course.validator.CourseScheduleValidator.ScheduleInput;
import com.classhub.global.exception.BusinessException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CourseScheduleValidatorTest {

    @Test
    void validate_shouldPassForNonOverlappingSchedules() {
        assertThatCode(() -> CourseScheduleValidator.validate(List.of(
                new ScheduleInput(DayOfWeek.MONDAY, LocalTime.of(7, 0), LocalTime.of(8, 0)),
                new ScheduleInput(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                new ScheduleInput(DayOfWeek.TUESDAY, LocalTime.of(6, 0), LocalTime.of(7, 0))
        ))).doesNotThrowAnyException();
    }

    @Test
    void validate_shouldThrowWhenSchedulesEmpty() {
        assertThatThrownBy(() -> CourseScheduleValidator.validate(List.of()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void validate_shouldThrowWhenOverlappingWithinSameDay() {
        assertThatThrownBy(() -> CourseScheduleValidator.validate(List.of(
                new ScheduleInput(DayOfWeek.WEDNESDAY, LocalTime.of(7, 0), LocalTime.of(8, 0)),
                new ScheduleInput(DayOfWeek.WEDNESDAY, LocalTime.of(7, 30), LocalTime.of(8, 30))
        ))).isInstanceOf(BusinessException.class);
    }

    @Test
    void validate_shouldThrowWhenEndEqualsOrBeforeStart() {
        assertThatThrownBy(() -> CourseScheduleValidator.validate(List.of(
                new ScheduleInput(DayOfWeek.THURSDAY, LocalTime.of(8, 0), LocalTime.of(8, 0))
        ))).isInstanceOf(BusinessException.class);
    }

    @Test
    void validate_shouldThrowWhenScheduleHasNullField() {
        assertThatThrownBy(() -> CourseScheduleValidator.validate(List.of(
                new ScheduleInput(null, LocalTime.of(8, 0), LocalTime.of(9, 0))
        ))).isInstanceOf(BusinessException.class);
    }
}
