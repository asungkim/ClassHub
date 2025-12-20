package com.classhub.domain.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.classhub.domain.assignment.model.BranchRole;
import com.classhub.domain.assignment.model.TeacherBranchAssignment;
import com.classhub.domain.assignment.repository.TeacherBranchAssignmentRepository;
import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.branch.repository.BranchRepository;
import com.classhub.domain.company.company.model.Company;
import com.classhub.domain.company.company.model.CompanyType;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.domain.company.company.repository.CompanyRepository;
import com.classhub.domain.course.dto.CourseStatusFilter;
import com.classhub.domain.course.dto.request.CourseCreateRequest;
import com.classhub.domain.course.dto.request.CourseScheduleRequest;
import com.classhub.domain.course.dto.request.CourseStatusUpdateRequest;
import com.classhub.domain.course.dto.request.CourseUpdateRequest;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.course.application.CourseViewAssembler;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
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
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private TeacherBranchAssignmentRepository teacherBranchAssignmentRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private CompanyRepository companyRepository;

    private CourseService courseService;

    private CourseViewAssembler courseViewAssembler;

    private UUID teacherId;
    private UUID branchId;
    private UUID companyId;
    private Branch branch;
    private Company company;
    private TeacherBranchAssignment assignment;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        branchId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        branch = Branch.create(companyId, "강남", teacherId, VerifiedStatus.VERIFIED);
        ReflectionTestUtils.setField(branch, "id", branchId);
        company = Company.create("러셀", "desc", CompanyType.ACADEMY, VerifiedStatus.VERIFIED, teacherId);
        ReflectionTestUtils.setField(company, "id", companyId);
        assignment = TeacherBranchAssignment.create(teacherId, branchId, BranchRole.FREELANCE);
        courseViewAssembler = new CourseViewAssembler(branchRepository, companyRepository);
        courseService = new CourseService(
                courseRepository,
                teacherBranchAssignmentRepository,
                branchRepository,
                companyRepository,
                courseViewAssembler
        );
    }

    @Test
    void createCourse_shouldPersistAndReturnResponse() {
        CourseCreateRequest request = new CourseCreateRequest(
                branchId,
                "중3 수학",
                "desc",
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                List.of(new CourseScheduleRequest(DayOfWeek.MONDAY, LocalTime.of(7, 0), LocalTime.of(8, 0)))
        );
        UUID courseId = UUID.randomUUID();
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(teacherBranchAssignmentRepository.findByTeacherMemberIdAndBranchId(teacherId, branchId))
                .thenReturn(Optional.of(assignment));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", courseId);
            return saved;
        });

        CourseResponse response = courseService.createCourse(teacherId, request);

        assertThat(response.courseId()).isEqualTo(courseId);
        assertThat(response.branchName()).isEqualTo("강남");
        assertThat(response.companyName()).isEqualTo("러셀");
        assertThat(response.schedules()).hasSize(1);
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void createCourse_shouldThrow_whenAssignmentMissing() {
        CourseCreateRequest request = new CourseCreateRequest(
                branchId,
                "중3 수학",
                null,
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                List.of(new CourseScheduleRequest(DayOfWeek.MONDAY, LocalTime.NOON, LocalTime.NOON.plusHours(1)))
        );
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(teacherBranchAssignmentRepository.findByTeacherMemberIdAndBranchId(teacherId, branchId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.createCourse(teacherId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.COURSE_FORBIDDEN);
    }

    @Test
    void getCourses_shouldReturnPagedResponses() {
        Course course = Course.create(
                branchId,
                teacherId,
                "중3 수학",
                null,
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                Set.of(new Course.CourseSchedule(DayOfWeek.MONDAY, LocalTime.NOON, LocalTime.NOON.plusHours(1)))
        );
        ReflectionTestUtils.setField(course, "id", UUID.randomUUID());
        Page<Course> page = new PageImpl<>(List.of(course), PageRequest.of(0, 10), 1);
        when(courseRepository.searchCourses(eq(teacherId), eq(branchId), eq(CourseStatusFilter.ACTIVE), anyString(), any(PageRequest.class)))
                .thenReturn(page);
        when(branchRepository.findAllById(any())).thenReturn(List.of(branch));
        when(companyRepository.findAllById(any())).thenReturn(List.of(company));

        PageResponse<CourseResponse> response = courseService.getCourses(
                teacherId,
                CourseStatusFilter.ACTIVE,
                branchId,
                "  수학 ",
                0,
                10
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().branchName()).isEqualTo("강남");
        verify(courseRepository).searchCourses(
                eq(teacherId),
                eq(branchId),
                eq(CourseStatusFilter.ACTIVE),
                eq("수학"),
                eq(PageRequest.of(0, 10))
        );
    }

    @Test
    void getCourse_shouldThrow_whenNotFound() {
        UUID courseId = UUID.randomUUID();
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getCourse(teacherId, courseId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.COURSE_NOT_FOUND);
    }

    @Test
    void updateCourse_shouldApplyChanges() {
        Course course = Course.create(
                branchId,
                teacherId,
                "기존 이름",
                null,
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                Set.of(new Course.CourseSchedule(DayOfWeek.MONDAY, LocalTime.NOON, LocalTime.NOON.plusHours(1)))
        );
        ReflectionTestUtils.setField(course, "id", UUID.randomUUID());
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(branchRepository.findAllById(any())).thenReturn(List.of(branch));
        when(companyRepository.findAllById(any())).thenReturn(List.of(company));
        when(courseRepository.save(course)).thenReturn(course);

        LocalDate newStart = course.getStartDate().plusDays(1);
        LocalDate newEnd = course.getEndDate().plusDays(1);
        CourseUpdateRequest request = new CourseUpdateRequest(
                "새 이름",
                "설명",
                newStart,
                newEnd,
                List.of(new CourseScheduleRequest(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)))
        );

        CourseResponse response = courseService.updateCourse(teacherId, course.getId(), request);

        assertThat(response.name()).isEqualTo("새 이름");
        assertThat(response.description()).isEqualTo("설명");
        assertThat(response.startDate()).isEqualTo(newStart);
        assertThat(response.schedules()).hasSize(1);
        verify(courseRepository).save(course);
    }

    @Test
    void updateCourseStatus_shouldToggleActiveFlag() {
        Course course = Course.create(
                branchId,
                teacherId,
                "중3 수학",
                null,
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                Set.of(new Course.CourseSchedule(DayOfWeek.MONDAY, LocalTime.NOON, LocalTime.NOON.plusHours(1)))
        );
        ReflectionTestUtils.setField(course, "id", UUID.randomUUID());
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(branchRepository.findAllById(any())).thenReturn(List.of(branch));
        when(companyRepository.findAllById(any())).thenReturn(List.of(company));
        when(courseRepository.save(course)).thenReturn(course);

        CourseResponse disabled = courseService.updateCourseStatus(
                teacherId,
                course.getId(),
                new CourseStatusUpdateRequest(false)
        );
        assertThat(disabled.active()).isFalse();

        CourseResponse enabled = courseService.updateCourseStatus(
                teacherId,
                course.getId(),
                new CourseStatusUpdateRequest(true)
        );
        assertThat(enabled.active()).isTrue();
    }

    @Test
    void getCoursesWithinPeriod_shouldReturnResponses() {
        Course course = Course.create(
                branchId,
                teacherId,
                "중3 수학",
                null,
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                Set.of(new Course.CourseSchedule(DayOfWeek.MONDAY, LocalTime.NOON, LocalTime.NOON.plusHours(1)))
        );
        ReflectionTestUtils.setField(course, "id", UUID.randomUUID());
        when(courseRepository.findCoursesWithinPeriod(eq(teacherId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(course));
        when(branchRepository.findAllById(any())).thenReturn(List.of(branch));
        when(companyRepository.findAllById(any())).thenReturn(List.of(company));

        List<CourseResponse> responses = courseService.getCoursesWithinPeriod(
                teacherId,
                LocalDate.now(),
                LocalDate.now().plusDays(7)
        );

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().companyName()).isEqualTo("러셀");
    }

    @Test
    void getCoursesWithinPeriod_shouldValidateRange() {
        assertThatThrownBy(() -> courseService.getCoursesWithinPeriod(
                teacherId,
                LocalDate.of(2025, 2, 1),
                LocalDate.of(2025, 1, 1)
        )).isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.BAD_REQUEST);
    }
}
