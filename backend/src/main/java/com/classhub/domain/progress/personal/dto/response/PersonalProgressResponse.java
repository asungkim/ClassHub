package com.classhub.domain.progress.personal.dto.response;

import com.classhub.domain.member.model.MemberRole;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PersonalProgressResponse(
        UUID id,
        UUID studentCourseRecordId,
        UUID courseId,
        LocalDate date,
        String title,
        String content,
        UUID writerId,
        MemberRole writerRole,
        LocalDateTime createdAt
) {
}
