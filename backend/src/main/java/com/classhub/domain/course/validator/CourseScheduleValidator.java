package com.classhub.domain.course.validator;

import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import com.classhub.global.validator.ScheduleTimeRangeValidator;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class CourseScheduleValidator {

    private CourseScheduleValidator() {
    }

    public static void validate(Collection<ScheduleInput> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }

        Map<DayOfWeek, List<ScheduleInput>> byDay = schedules.stream()
                .map(CourseScheduleValidator::requireValid)
                .collect(Collectors.groupingBy(
                        ScheduleInput::dayOfWeek,
                        () -> new EnumMap<>(DayOfWeek.class),
                        Collectors.toList()
                ));

        for (List<ScheduleInput> entries : byDay.values()) {
            entries.sort(Comparator.comparing(ScheduleInput::startTime));
            LocalTime prevEnd = null;
            for (ScheduleInput entry : entries) {
                if (prevEnd != null && !entry.startTime().isAfter(prevEnd)) {
                    throw new BusinessException(RsCode.BAD_REQUEST);
                }
                prevEnd = entry.endTime();
            }
        }
    }

    private static ScheduleInput requireValid(ScheduleInput input) {
        if (input == null
                || input.dayOfWeek() == null
                || input.startTime() == null
                || input.endTime() == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        ScheduleTimeRangeValidator.validate(input.startTime(), input.endTime(), RsCode.BAD_REQUEST);
        return input;
    }

    public record ScheduleInput(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
    }
}
