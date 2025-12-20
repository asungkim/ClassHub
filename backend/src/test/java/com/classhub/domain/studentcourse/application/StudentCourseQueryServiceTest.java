package com.classhub.domain.studentcourse.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.classhub.domain.course.application.CourseViewAssembler;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.studentcourse.dto.response.StudentCourseResponse;
import com.classhub.domain.studentcourse.model.StudentCourseEnrollment;
import com.classhub.domain.studentcourse.repository.StudentCourseEnrollmentRepository;
import com.classhub.global.response.PageResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StudentCourseQueryServiceTest {

    @Mock
    private StudentCourseEnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseViewAssembler courseViewAssembler;

    @InjectMocks
    private StudentCourseQueryService queryService;

    private UUID studentId;
    private UUID courseId;
    private Course course;
    private CourseResponse courseResponse;
    private StudentCourseEnrollment enrollment;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        course = Course.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "고2 수학",
                "desc",
                LocalDate.now(),
                LocalDate.now().plusMonths(2),
                Set.of()
        );
        ReflectionTestUtils.setField(course, "id", courseId);
        courseResponse = new CourseResponse(
                courseId,
                UUID.randomUUID(),
                "잠실",
                UUID.randomUUID(),
                "러셀",
                "고2 수학",
                "desc",
                LocalDate.now(),
                LocalDate.now().plusMonths(2),
                true,
                List.of()
        );
        enrollment = StudentCourseEnrollment.create(studentId, courseId, LocalDateTime.now());
        ReflectionTestUtils.setField(enrollment, "id", UUID.randomUUID());
    }

    @Test
    void getMyCourses_shouldReturnPagedCourses() {
        Page<StudentCourseEnrollment> page = new PageImpl<>(List.of(enrollment), PageRequest.of(0, 10), 1);
        when(enrollmentRepository.searchActiveEnrollments(eq(studentId), anyString(), eq(PageRequest.of(0, 10))))
                .thenReturn(page);
        when(courseRepository.findAllById(anyCollection())).thenReturn(List.of(course));
        CourseViewAssembler.CourseContext context = new CourseViewAssembler.CourseContext(
                java.util.Map.of(),
                java.util.Map.of(),
                java.util.Map.of()
        );
        when(courseViewAssembler.buildContext(anyCollection())).thenReturn(context);
        when(courseViewAssembler.toCourseResponse(course, context)).thenReturn(courseResponse);

        PageResponse<StudentCourseResponse> response = queryService.getMyCourses(studentId, "수학", 0, 10);

        assertThat(response.content()).hasSize(1);
        StudentCourseResponse first = response.content().getFirst();
        assertThat(first.course().name()).isEqualTo("고2 수학");
        assertThat(first.enrollmentId()).isEqualTo(enrollment.getId());
        verify(enrollmentRepository).searchActiveEnrollments(studentId, "수학", PageRequest.of(0, 10));
    }
}
