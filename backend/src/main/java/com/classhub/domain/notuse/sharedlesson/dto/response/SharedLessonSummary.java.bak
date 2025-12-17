package com.classhub.domain.sharedlesson.dto.response;

import com.classhub.domain.sharedlesson.model.SharedLesson;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record SharedLessonSummary(
        UUID id,
        UUID courseId,
        LocalDate date,
        String title,
        LocalDateTime createdAt
) {

    public static SharedLessonSummary from(SharedLesson sharedLesson) {
        return new SharedLessonSummary(
                sharedLesson.getId(),
                sharedLesson.getCourse().getId(),
                sharedLesson.getDate(),
                sharedLesson.getTitle(),
                sharedLesson.getCreatedAt()
        );
    }
}
