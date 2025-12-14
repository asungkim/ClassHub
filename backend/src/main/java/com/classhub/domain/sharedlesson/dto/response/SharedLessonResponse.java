package com.classhub.domain.sharedlesson.dto.response;

import com.classhub.domain.sharedlesson.model.SharedLesson;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record SharedLessonResponse(
        UUID id,
        UUID courseId,
        UUID writerId,
        LocalDate date,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static SharedLessonResponse from(SharedLesson sharedLesson) {
        return new SharedLessonResponse(
                sharedLesson.getId(),
                sharedLesson.getCourse().getId(),
                sharedLesson.getWriterId(),
                sharedLesson.getDate(),
                sharedLesson.getTitle(),
                sharedLesson.getContent(),
                sharedLesson.getCreatedAt(),
                sharedLesson.getUpdatedAt()
        );
    }
}
