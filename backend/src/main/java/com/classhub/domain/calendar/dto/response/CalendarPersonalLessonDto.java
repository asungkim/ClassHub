package com.classhub.domain.calendar.dto.response;

import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.personallesson.model.PersonalLesson;
import java.time.LocalDate;
import java.util.UUID;

public record CalendarPersonalLessonDto(
        UUID id,
        LocalDate date,
        String content,
        UUID writerId,
        MemberRole writerRole
) {

    public static CalendarPersonalLessonDto from(PersonalLesson lesson, MemberRole writerRole) {
        return new CalendarPersonalLessonDto(
                lesson.getId(),
                lesson.getDate(),
                lesson.getContent(),
                lesson.getWriterId(),
                writerRole
        );
    }
}
