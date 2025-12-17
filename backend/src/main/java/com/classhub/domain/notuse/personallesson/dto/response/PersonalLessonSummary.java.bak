package com.classhub.domain.personallesson.dto.response;

import com.classhub.domain.personallesson.model.PersonalLesson;
import java.time.LocalDate;
import java.util.UUID;

public record PersonalLessonSummary(
        UUID id,
        UUID studentProfileId,
        LocalDate date,
        String title,
        String content
) {

    public static PersonalLessonSummary from(PersonalLesson lesson) {
        return new PersonalLessonSummary(
                lesson.getId(),
                lesson.getStudentProfile().getId(),
                lesson.getDate(),
                lesson.getTitle(),
                lesson.getContent()
        );
    }
}
