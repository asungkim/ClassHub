package com.classhub.domain.course.application;

import com.classhub.domain.assignment.model.TeacherAssistantAssignment;
import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.course.dto.CourseStatusFilter;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.dto.response.CourseWithTeacherResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AssistantCourseService {

    private final TeacherAssistantAssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final MemberRepository memberRepository;
    private final CourseViewAssembler courseViewAssembler;

    @Transactional(readOnly = true)
    public PageResponse<CourseWithTeacherResponse> getCourses(UUID assistantId,
                                                              UUID teacherFilter,
                                                              CourseStatusFilter status,
                                                              String keyword,
                                                              int page,
                                                              int size) {
        List<TeacherAssistantAssignment> assignments = assignmentRepository
                .findByAssistantMemberIdAndDeletedAtIsNull(assistantId);
        if (assignments.isEmpty()) {
            Page<CourseWithTeacherResponse> emptyPage = Page.empty(PageRequest.of(page, size));
            return PageResponse.from(emptyPage);
        }

        Set<UUID> teacherIds = assignments.stream()
                .map(TeacherAssistantAssignment::getTeacherMemberId)
                .collect(Collectors.toSet());

        if (teacherFilter != null) {
            if (!teacherIds.contains(teacherFilter)) {
                throw new BusinessException(RsCode.COURSE_FORBIDDEN);
            }
            teacherIds = Set.of(teacherFilter);
        }

        Page<Course> courses = courseRepository.searchCoursesForAssistant(
                teacherIds,
                status,
                normalizeKeyword(keyword),
                PageRequest.of(page, size)
        );
        CourseViewAssembler.CourseContext context = courseViewAssembler.buildContext(courses.getContent());
        Map<UUID, Member> teacherMap = loadTeachers(courses.getContent());

        Page<CourseWithTeacherResponse> mapped = courses.map(course -> {
            CourseResponse base = courseViewAssembler.toCourseResponse(course, context);
            Member teacher = teacherMap.get(course.getTeacherMemberId());
            if (teacher == null) {
                throw new BusinessException(RsCode.MEMBER_NOT_FOUND);
            }
            return CourseWithTeacherResponse.from(base, teacher.getId(), teacher.getName());
        });
        return PageResponse.from(mapped);
    }

    private Map<UUID, Member> loadTeachers(Collection<Course> courses) {
        List<UUID> teacherIds = courses.stream()
                .map(Course::getTeacherMemberId)
                .distinct()
                .toList();
        if (teacherIds.isEmpty()) {
            return Map.of();
        }
        List<Member> members = memberRepository.findAllById(teacherIds);
        if (members.size() < teacherIds.size()) {
            throw new BusinessException(RsCode.MEMBER_NOT_FOUND);
        }
        return members.stream()
                .collect(Collectors.toMap(Member::getId, member -> member));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
