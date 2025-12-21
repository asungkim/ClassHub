package com.classhub.domain.progress.course.mapper;

import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.progress.course.dto.response.CourseProgressResponse;
import com.classhub.domain.progress.course.model.CourseProgress;
import org.springframework.stereotype.Component;

@Component
public class CourseProgressMapper {

    public CourseProgressResponse toResponse(CourseProgress progress, MemberRole writerRole) {
        return new CourseProgressResponse(
                progress.getId(),
                progress.getCourseId(),
                progress.getDate(),
                progress.getTitle(),
                progress.getContent(),
                progress.getWriterId(),
                writerRole,
                progress.getCreatedAt()
        );
    }
}
