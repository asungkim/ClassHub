package com.classhub.domain.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.classhub.domain.assignment.model.TeacherAssistantAssignment;
import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.course.dto.CourseStatusFilter;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.dto.response.CourseWithTeacherResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AssistantCourseServiceTest {

    @Mock
    private TeacherAssistantAssignmentRepository assignmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CourseViewAssembler courseViewAssembler;

    @InjectMocks
    private AssistantCourseService assistantCourseService;

    @Test
    void getCourses_shouldReturnResponsesForAssignedTeachers() {
        UUID assistantId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        TeacherAssistantAssignment assignment = TeacherAssistantAssignment.create(teacherId, assistantId);
        when(assignmentRepository.findByAssistantMemberIdAndDeletedAtIsNull(assistantId))
                .thenReturn(List.of(assignment));

        Course course = Course.create(
                UUID.randomUUID(),
                teacherId,
                "국어",
                null,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                Collections.emptySet()
        );
        Page<Course> page = new PageImpl<>(List.of(course), PageRequest.of(0, 10), 1);
        when(courseRepository.searchCoursesForAssistant(anyCollection(), any(), any(), any()))
                .thenReturn(page);

        CourseViewAssembler.CourseContext context = new CourseViewAssembler.CourseContext(
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap()
        );
        when(courseViewAssembler.buildContext(page.getContent())).thenReturn(context);
        CourseResponse baseResponse = new CourseResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "지점",
                UUID.randomUUID(),
                "회사",
                "국어",
                null,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                true,
                List.of()
        );
        when(courseViewAssembler.toCourseResponse(course, context)).thenReturn(baseResponse);

        Member teacher = mock(Member.class);
        when(teacher.getId()).thenReturn(teacherId);
        when(teacher.getName()).thenReturn("Teacher Park");
        when(memberRepository.findAllById(anyCollection())).thenReturn(List.of(teacher));

        PageResponse<CourseWithTeacherResponse> result = assistantCourseService.getCourses(
                assistantId,
                null,
                CourseStatusFilter.ALL,
                null,
                0,
                10
        );

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).teacherName()).isEqualTo("Teacher Park");
        verify(courseRepository).searchCoursesForAssistant(anyCollection(), eq(CourseStatusFilter.ALL), any(), any());
    }

    @Test
    void getCourses_shouldReturnEmptyWhenNoAssignments() {
        UUID assistantId = UUID.randomUUID();
        when(assignmentRepository.findByAssistantMemberIdAndDeletedAtIsNull(assistantId))
                .thenReturn(List.of());

        PageResponse<CourseWithTeacherResponse> result = assistantCourseService.getCourses(
                assistantId,
                null,
                CourseStatusFilter.ACTIVE,
                null,
                0,
                10
        );

        assertThat(result.content()).isEmpty();
        verify(courseRepository, never()).searchCoursesForAssistant(anyCollection(), any(), any(), any());
    }

    @Test
    void getCourses_shouldThrowWhenTeacherFilterNotAssigned() {
        UUID assistantId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        when(assignmentRepository.findByAssistantMemberIdAndDeletedAtIsNull(assistantId))
                .thenReturn(List.of(TeacherAssistantAssignment.create(UUID.randomUUID(), assistantId)));

        assertThatThrownBy(() -> assistantCourseService.getCourses(
                assistantId,
                teacherId,
                CourseStatusFilter.ALL,
                null,
                0,
                10
        )).isInstanceOf(BusinessException.class);
    }
}
