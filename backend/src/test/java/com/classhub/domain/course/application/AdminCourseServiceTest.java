package com.classhub.domain.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.classhub.domain.course.dto.CourseStatusFilter;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
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
class AdminCourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseViewAssembler courseViewAssembler;

    @InjectMocks
    private AdminCourseService adminCourseService;

    @Test
    void searchCourses_shouldReturnMappedPage() {
        UUID teacherId = UUID.randomUUID();
        Course course = Course.create(
                UUID.randomUUID(),
                teacherId,
                "입시 수학",
                "설명",
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                Collections.emptySet()
        );
        Page<Course> page = new PageImpl<>(List.of(course), PageRequest.of(0, 10), 1);
        CourseResponse response = new CourseResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "지점",
                UUID.randomUUID(),
                "회사",
                "입시 수학",
                "설명",
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                true,
                List.of()
        );
        CourseViewAssembler.CourseContext context = new CourseViewAssembler.CourseContext(
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap()
        );
        when(courseRepository.searchCoursesForAdmin(any(), any(), any(), any(), any(), any()))
                .thenReturn(page);
        when(courseViewAssembler.buildContext(page.getContent())).thenReturn(context);
        when(courseViewAssembler.toCourseResponse(course, context)).thenReturn(response);

        PageResponse<CourseResponse> result = adminCourseService.searchCourses(
                teacherId,
                null,
                null,
                CourseStatusFilter.ALL,
                "",
                0,
                10
        );

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).name()).isEqualTo("입시 수학");
        verify(courseRepository).searchCoursesForAdmin(eq(teacherId), any(), any(), eq(CourseStatusFilter.ALL), any(), any());
    }

    @Test
    void deleteCourse_shouldRemoveEntity() {
        UUID courseId = UUID.randomUUID();
        Course course = Course.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "과학",
                null,
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                Set.of()
        );
        when(courseRepository.findById(courseId)).thenReturn(java.util.Optional.of(course));

        adminCourseService.deleteCourse(courseId);

        verify(courseRepository).delete(course);
    }

    @Test
    void deleteCourse_shouldThrowWhenNotFound() {
        UUID courseId = UUID.randomUUID();
        when(courseRepository.findById(courseId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> adminCourseService.deleteCourse(courseId))
                .isInstanceOf(BusinessException.class);
    }
}
