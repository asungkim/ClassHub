package com.classhub.domain.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.dto.response.CourseScheduleResponse;
import com.classhub.domain.course.dto.response.PublicCourseResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.response.PageResponse;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
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
class PublicCourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CourseViewAssembler courseViewAssembler;

    @InjectMocks
    private PublicCourseService publicCourseService;

    @Test
    void searchCourses_shouldReturnMappedResponses() {
        UUID branchId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        Course course = Course.create(
                branchId,
                teacherId,
                "국어",
                "심화",
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                Collections.emptySet()
        );
        Page<Course> page = new PageImpl<>(List.of(course), PageRequest.of(0, 10), 1);
        when(courseRepository.searchPublicCourses(any(), any(), any(), any(), eq(true), any()))
                .thenReturn(page);

        CourseViewAssembler.CourseContext context = new CourseViewAssembler.CourseContext(
                Map.of(),
                Map.of(),
                Map.of()
        );
        when(courseViewAssembler.buildContext(page.getContent())).thenReturn(context);
        CourseResponse response = new CourseResponse(
                UUID.randomUUID(),
                branchId,
                "강남",
                UUID.randomUUID(),
                "클래스",
                "국어",
                "심화",
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                true,
                List.of(new CourseScheduleResponse(
                        DayOfWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(11, 0)
                ))
        );
        when(courseViewAssembler.toCourseResponse(course, context)).thenReturn(response);

        Member teacher = mock(Member.class);
        when(teacher.getId()).thenReturn(teacherId);
        when(teacher.getName()).thenReturn("Teacher Kim");
        when(memberRepository.findAllById(any())).thenReturn(List.of(teacher));

        PageResponse<PublicCourseResponse> result = publicCourseService.searchCourses(
                null,
                null,
                null,
                null,
                true,
                0,
                10
        );

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).teacherName()).isEqualTo("Teacher Kim");
        assertThat(result.content().get(0).scheduleSummary()).contains("월 09:00~11:00");
    }

    @Test
    void searchCourses_shouldForwardFilters() {
        UUID companyId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        Page<Course> emptyPage = Page.empty(PageRequest.of(0, 5));
        when(courseRepository.searchPublicCourses(any(), any(), any(), any(), eq(false), any()))
                .thenReturn(emptyPage);
        when(courseViewAssembler.buildContext(emptyPage.getContent()))
                .thenReturn(new CourseViewAssembler.CourseContext(Map.of(), Map.of(), Map.of()));

        publicCourseService.searchCourses(
                companyId,
                branchId,
                teacherId,
                "  math ",
                false,
                0,
                5
        );

        verify(courseRepository).searchPublicCourses(
                eq(companyId),
                eq(branchId),
                eq(teacherId),
                eq("math"),
                eq(false),
                any()
        );
    }
}
