package com.classhub.domain.studentcourse.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.anyList;

import com.classhub.domain.course.application.CourseViewAssembler;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.studentcourse.dto.response.StudentMyCourseResponse;
import com.classhub.domain.studentcourse.model.StudentCourseAssignment;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseAssignmentRepository;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.response.PageResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StudentCourseQueryServiceTest {

    @Mock
    private StudentCourseAssignmentRepository assignmentRepository;
    @Mock
    private StudentCourseRecordRepository recordRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseViewAssembler courseViewAssembler;

    @InjectMocks
    private StudentCourseQueryService queryService;

    @Test
    void getMyCourses_shouldIncludeActiveAndInactiveAssignments() {
        UUID studentId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID courseId1 = UUID.randomUUID();
        UUID courseId2 = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        StudentCourseAssignment assignmentActive = StudentCourseAssignment.create(
                studentId,
                courseId1,
                teacherId,
                LocalDateTime.of(2025, Month.JANUARY, 1, 10, 0)
        );
        UUID assignmentId1 = UUID.randomUUID();
        ReflectionTestUtils.setField(assignmentActive, "id", assignmentId1);

        StudentCourseAssignment assignmentInactive = StudentCourseAssignment.create(
                studentId,
                courseId2,
                teacherId,
                LocalDateTime.of(2025, Month.JANUARY, 2, 10, 0)
        );
        assignmentInactive.deactivate();
        UUID assignmentId2 = UUID.randomUUID();
        ReflectionTestUtils.setField(assignmentInactive, "id", assignmentId2);

        PageRequest pageable = PageRequest.of(0, 20);
        given(assignmentRepository.findByStudentMemberIdWithActiveCourse(studentId, pageable))
                .willReturn(new PageImpl<>(List.of(assignmentActive, assignmentInactive), pageable, 2));

        Course course1 = Course.create(
                branchId,
                teacherId,
                "Course 1",
                null,
                LocalDate.of(2025, Month.JANUARY, 1),
                LocalDate.of(2025, Month.MARCH, 1),
                Set.of()
        );
        ReflectionTestUtils.setField(course1, "id", courseId1);
        Course course2 = Course.create(
                branchId,
                teacherId,
                "Course 2",
                null,
                LocalDate.of(2025, Month.JANUARY, 1),
                LocalDate.of(2025, Month.MARCH, 1),
                Set.of()
        );
        ReflectionTestUtils.setField(course2, "id", courseId2);

        List<Course> courses = List.of(course1, course2);
        given(courseRepository.findAllById(anyList())).willReturn(courses);
        CourseViewAssembler.CourseContext context = new CourseViewAssembler.CourseContext(
                Map.of(),
                Map.of(),
                Map.of()
        );
        given(courseViewAssembler.buildContext(courses)).willReturn(context);
        CourseResponse response1 = new CourseResponse(
                courseId1,
                branchId,
                "Branch",
                companyId,
                "Company",
                "Course 1",
                null,
                LocalDate.of(2025, Month.JANUARY, 1),
                LocalDate.of(2025, Month.MARCH, 1),
                true,
                List.of()
        );
        CourseResponse response2 = new CourseResponse(
                courseId2,
                branchId,
                "Branch",
                companyId,
                "Company",
                "Course 2",
                null,
                LocalDate.of(2025, Month.JANUARY, 1),
                LocalDate.of(2025, Month.MARCH, 1),
                true,
                List.of()
        );
        given(courseViewAssembler.toCourseResponse(course1, context)).willReturn(response1);
        given(courseViewAssembler.toCourseResponse(course2, context)).willReturn(response2);

        StudentCourseRecord record = StudentCourseRecord.create(studentId, courseId1, null, null, null);
        UUID recordId = UUID.randomUUID();
        ReflectionTestUtils.setField(record, "id", recordId);
        given(recordRepository.findByStudentMemberIdAndDeletedAtIsNull(studentId))
                .willReturn(List.of(record));

        PageResponse<StudentMyCourseResponse> result = queryService.getMyCourses(studentId, 0, 20);

        assertThat(result.content()).hasSize(2);
        Map<UUID, StudentMyCourseResponse> responseMap = result.content().stream()
                .collect(java.util.stream.Collectors.toMap(
                        response -> response.course().courseId(),
                        Function.identity()
                ));
        StudentMyCourseResponse first = responseMap.get(courseId1);
        StudentMyCourseResponse second = responseMap.get(courseId2);

        assertThat(first.assignmentId()).isEqualTo(assignmentId1);
        assertThat(first.assignmentActive()).isTrue();
        assertThat(first.recordId()).isEqualTo(recordId);

        assertThat(second.assignmentId()).isEqualTo(assignmentId2);
        assertThat(second.assignmentActive()).isFalse();
        assertThat(second.recordId()).isNull();
    }
}
