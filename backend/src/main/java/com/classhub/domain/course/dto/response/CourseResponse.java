package com.classhub.domain.course.dto.response;

import com.classhub.domain.course.model.Course;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

public record CourseResponse(
        UUID id,
        String name,
        String company,
        UUID teacherId,
        Set<DayOfWeek> daysOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getName(),
                course.getCompany(),
                course.getTeacherId(),
                course.getDaysOfWeek(),
                course.getStartTime(),
                course.getEndTime(),
                course.isActive(),
                course.getCreatedAt(),
                course.getUpdatedAt()
        );
    }
}
