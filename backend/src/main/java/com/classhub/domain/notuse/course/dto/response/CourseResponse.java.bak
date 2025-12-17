package com.classhub.domain.course.dto.response;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.model.CourseSchedule;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CourseResponse(
        UUID id,
        String name,
        String company,
        UUID teacherId,
        List<CourseScheduleResponse> schedules,
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
                course.getSchedules().stream()
                        .map(CourseScheduleResponse::from)
                        .sorted(java.util.Comparator.comparing(CourseScheduleResponse::dayOfWeek))
                        .toList(),
                course.isActive(),
                course.getCreatedAt(),
                course.getUpdatedAt()
        );
    }

    public record CourseScheduleResponse(String dayOfWeek, String startTime, String endTime) {
        public static CourseScheduleResponse from(CourseSchedule schedule) {
            return new CourseScheduleResponse(
                    schedule.getDayOfWeek().name(),
                    schedule.getStartTime().toString(),
                    schedule.getEndTime().toString()
            );
        }
    }
}
