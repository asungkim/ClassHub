package com.classhub.domain.progress.course.dto.response;

import com.classhub.domain.member.model.MemberRole;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CourseProgressResponse(
        UUID id,
        UUID courseId,
        LocalDate date,
        String title,
        String content,
        UUID writerId,
        String writerName,
        MemberRole writerRole,
        LocalDateTime createdAt
) {
}
