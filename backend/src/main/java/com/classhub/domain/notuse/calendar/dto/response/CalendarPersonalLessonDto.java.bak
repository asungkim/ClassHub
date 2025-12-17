package com.classhub.domain.calendar.dto.response;

import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.personallesson.model.PersonalLesson;
import java.time.LocalDate;
import java.util.UUID;

public record CalendarPersonalLessonDto(
        UUID id,
        LocalDate date,
        String title,
        String content,
        UUID writerId,
        MemberRole writerRole,
        boolean editable
) {

    public static CalendarPersonalLessonDto from(
            PersonalLesson lesson,
            MemberRole writerRole,
            boolean editable
    ) {
        return new CalendarPersonalLessonDto(
                lesson.getId(),
                lesson.getDate(),
                lesson.getTitle(),
                lesson.getContent(),
                lesson.getWriterId(),
                writerRole,
                editable
        );
    }
}
