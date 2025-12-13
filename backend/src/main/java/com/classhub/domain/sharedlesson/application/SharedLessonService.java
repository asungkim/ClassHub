package com.classhub.domain.sharedlesson.application;

import com.classhub.domain.sharedlesson.dto.request.SharedLessonCreateRequest;
import com.classhub.domain.sharedlesson.dto.request.SharedLessonUpdateRequest;
import com.classhub.domain.sharedlesson.dto.response.SharedLessonResponse;
import com.classhub.domain.sharedlesson.dto.response.SharedLessonSummary;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.sharedlesson.repository.SharedLessonRepository;
import com.classhub.domain.sharedlesson.dto.request.SharedLessonCreateRequest;
import com.classhub.domain.sharedlesson.dto.request.SharedLessonUpdateRequest;
import com.classhub.domain.sharedlesson.dto.response.SharedLessonResponse;
import com.classhub.domain.sharedlesson.dto.response.SharedLessonSummary;
import com.classhub.domain.sharedlesson.model.SharedLesson;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SharedLessonService {

    private final SharedLessonRepository sharedLessonRepository;
    private final CourseRepository courseRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public SharedLessonResponse createLesson(UUID principalId, SharedLessonCreateRequest request) {
        Member teacher = getTeacher(principalId);
        Course course = getCourseOwnedByTeacher(request.courseId(), teacher.getId());
        LocalDate date = request.date() != null ? request.date() : LocalDate.now();

        SharedLesson sharedLesson = SharedLesson.builder()
                .course(course)
                .writerId(teacher.getId())
                .date(date)
                .title(request.title())
                .content(request.content())
                .build();

        return SharedLessonResponse.from(sharedLessonRepository.save(sharedLesson));
    }

    @Transactional(readOnly = true)
    public Page<SharedLessonSummary> getLessons(
            UUID principalId,
            UUID courseId,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    ) {
        Member teacher = getTeacher(principalId);
        getCourseOwnedByTeacher(courseId, teacher.getId());
        Page<SharedLesson> page;
        if (from != null || to != null) {
            LocalDate start = from != null ? from : LocalDate.MIN;
            LocalDate end = to != null ? to : LocalDate.MAX;
            page = sharedLessonRepository.findAllByCourse_TeacherIdAndCourse_IdAndDateBetween(
                    teacher.getId(),
                    courseId,
                    start,
                    end,
                    pageable
            );
        } else {
            page = sharedLessonRepository.findAllByCourse_TeacherIdAndCourse_Id(
                    teacher.getId(),
                    courseId,
                    pageable
            );
        }

        return page.map(SharedLessonSummary::from);
    }

    @Transactional(readOnly = true)
    public SharedLessonResponse getLesson(UUID principalId, UUID sharedLessonId) {
        Member teacher = getTeacher(principalId);
        SharedLesson sharedLesson = getSharedLessonOwnedByTeacher(sharedLessonId, teacher.getId());
        return SharedLessonResponse.from(sharedLesson);
    }

    @Transactional
    public SharedLessonResponse updateLesson(
            UUID principalId,
            UUID sharedLessonId,
            SharedLessonUpdateRequest request
    ) {
        Member teacher = getTeacher(principalId);
        SharedLesson sharedLesson = getSharedLessonOwnedByTeacher(sharedLessonId, teacher.getId());
        sharedLesson.update(request.date(), request.title(), request.content());
        return SharedLessonResponse.from(sharedLessonRepository.save(sharedLesson));
    }

    @Transactional
    public void deleteLesson(UUID principalId, UUID sharedLessonId) {
        Member teacher = getTeacher(principalId);
        SharedLesson sharedLesson = getSharedLessonOwnedByTeacher(sharedLessonId, teacher.getId());
        sharedLessonRepository.delete(sharedLesson);
    }

    private Member getTeacher(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(RsCode.UNAUTHENTICATED::toException);
        if (member.getRole() != MemberRole.TEACHER) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        return member;
    }

    private Course getCourseOwnedByTeacher(UUID courseId, UUID teacherId) {
        return courseRepository.findByIdAndTeacherId(courseId, teacherId)
                .orElseThrow(RsCode.COURSE_FORBIDDEN::toException);
    }

    private SharedLesson getSharedLessonOwnedByTeacher(UUID sharedLessonId, UUID teacherId) {
        return sharedLessonRepository.findByIdAndCourse_TeacherId(sharedLessonId, teacherId)
                .orElseThrow(RsCode.SHARED_LESSON_NOT_FOUND::toException);
    }
}
