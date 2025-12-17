package com.classhub.domain.personallesson.dto.response;

import com.classhub.domain.personallesson.model.PersonalLesson;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PersonalLessonResponse(
        UUID id,
        UUID studentProfileId,
        UUID teacherId,
        UUID writerId,
        LocalDate date,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static PersonalLessonResponse from(PersonalLesson lesson) {
        return new PersonalLessonResponse(
                lesson.getId(),
                lesson.getStudentProfile().getId(),
                lesson.getTeacherId(),
                lesson.getWriterId(),
                lesson.getDate(),
                lesson.getTitle(),
                lesson.getContent(),
                lesson.getCreatedAt(),
                lesson.getUpdatedAt()
        );
    }
}
