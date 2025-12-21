package com.classhub.domain.progress.personal.mapper;

import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.progress.personal.dto.response.PersonalProgressResponse;
import com.classhub.domain.progress.personal.model.PersonalProgress;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PersonalProgressMapper {

    public PersonalProgressResponse toResponse(PersonalProgress progress,
                                               UUID courseId,
                                               MemberRole writerRole) {
        return new PersonalProgressResponse(
                progress.getId(),
                progress.getStudentCourseRecordId(),
                courseId,
                progress.getDate(),
                progress.getTitle(),
                progress.getContent(),
                progress.getWriterId(),
                writerRole,
                progress.getCreatedAt()
        );
    }
}
