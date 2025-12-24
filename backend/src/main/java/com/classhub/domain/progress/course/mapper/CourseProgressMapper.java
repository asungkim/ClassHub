package com.classhub.domain.progress.course.mapper;

import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.progress.course.dto.response.CourseProgressResponse;
import com.classhub.domain.progress.course.model.CourseProgress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseProgressMapper {

    private final MemberRepository memberRepository;

    public CourseProgressResponse toResponse(CourseProgress progress, MemberRole writerRole) {
        String writerName = memberRepository.findById(progress.getWriterId())
                .map(Member::getName)
                .orElse("알 수 없음");

        return new CourseProgressResponse(
                progress.getId(),
                progress.getCourseId(),
                progress.getDate(),
                progress.getTitle(),
                progress.getContent(),
                progress.getWriterId(),
                writerName,
                writerRole,
                progress.getCreatedAt()
        );
    }
}
