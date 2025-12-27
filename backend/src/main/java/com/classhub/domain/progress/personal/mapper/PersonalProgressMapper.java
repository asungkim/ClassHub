package com.classhub.domain.progress.personal.mapper;

import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.progress.personal.dto.response.PersonalProgressResponse;
import com.classhub.domain.progress.personal.model.PersonalProgress;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PersonalProgressMapper {

    private final MemberRepository memberRepository;

    public PersonalProgressResponse toResponse(PersonalProgress progress,
                                               UUID courseId) {
        Member writer = memberRepository.findById(progress.getWriterId())
                .orElse(null);
        String writerName = writer == null ? "알 수 없음" : writer.getName();
        MemberRole writerRole = writer == null ? null : writer.getRole();

        return new PersonalProgressResponse(
                progress.getId(),
                progress.getStudentCourseRecordId(),
                courseId,
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
