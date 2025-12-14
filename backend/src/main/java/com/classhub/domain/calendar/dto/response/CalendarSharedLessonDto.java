package com.classhub.domain.calendar.dto.response;

import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.sharedlesson.model.SharedLesson;
import java.time.LocalDate;
import java.util.UUID;

public record CalendarSharedLessonDto(
        UUID id,
        UUID courseId,
        String courseName,
        LocalDate date,
        String title,
        String content,
        UUID writerId,
        MemberRole writerRole
) {

    public static CalendarSharedLessonDto from(SharedLesson lesson, MemberRole writerRole) {
        return new CalendarSharedLessonDto(
                lesson.getId(),
                lesson.getCourse().getId(),
                lesson.getCourse().getName(),
                lesson.getDate(),
                lesson.getTitle(),
                lesson.getContent(),
                lesson.getWriterId(),
                writerRole
        );
    }
}
