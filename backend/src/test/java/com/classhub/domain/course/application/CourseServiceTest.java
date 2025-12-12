package com.classhub.domain.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classhub.domain.course.dto.request.CourseCreateRequest;
import com.classhub.domain.course.dto.request.CourseUpdateRequest;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CourseServiceTest {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private MemberRepository memberRepository;

    private UUID teacherId;
    private Member teacher;

    @BeforeEach
    void setUp() {
        courseRepository.deleteAll();
        memberRepository.deleteAll();

        teacher = Member.builder()
                .email("teacher@test.com")
                .password("encoded")
                .name("Test Teacher")
                .role(MemberRole.TEACHER)
                .build();
        teacherId = memberRepository.save(teacher).getId();
    }

    @Test
    @DisplayName("유효한 요청으로 Course를 생성하면 성공한다")
    void shouldCreateCourse_whenValidRequest() {
        // given
        CourseCreateRequest request = new CourseCreateRequest(
                "중등 수학 A반",
                "ABC 학원",
                Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                LocalTime.of(14, 0),
                LocalTime.of(16, 0)
        );

        // when
        CourseResponse response = courseService.createCourse(teacherId, request);

        // then
        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("중등 수학 A반");
        assertThat(response.company()).isEqualTo("ABC 학원");
        assertThat(response.teacherId()).isEqualTo(teacherId);
        assertThat(response.daysOfWeek()).contains(DayOfWeek.MONDAY);
        assertThat(response.startTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(response.endTime()).isEqualTo(LocalTime.of(16, 0));
        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("요일이 비어 있으면 Course 생성에 실패한다")
    void shouldThrowException_whenDaysOfWeekEmptyOnCreate() {
        // given
        CourseCreateRequest request = new CourseCreateRequest(
                "중등 수학 A반",
                "ABC 학원",
                Set.of(),
                LocalTime.of(14, 0),
                LocalTime.of(16, 0)
        );

        // when & then
        assertThatThrownBy(() -> courseService.createCourse(teacherId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("종료 시간이 시작 시간보다 이르면 Course 생성에 실패한다")
    void shouldThrowException_whenEndTimeIsBeforeStartTimeOnCreate() {
        // given
        CourseCreateRequest request = new CourseCreateRequest(
                "중등 수학 A반",
                "ABC 학원",
                Set.of(DayOfWeek.MONDAY),
                LocalTime.of(16, 0),
                LocalTime.of(14, 0)
        );

        // when & then
        assertThatThrownBy(() -> courseService.createCourse(teacherId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("Teacher의 모든 Course를 조회할 수 있다")
    void shouldReturnAllCourses_whenTeacherHasCourses() {
        // given
        createTestCourse("중등 수학 A반", teacherId);
        createTestCourse("중등 수학 B반", teacherId);

        // when
        List<CourseResponse> courses = courseService.getCoursesByTeacher(teacherId, null);

        // then
        assertThat(courses).hasSize(2);
        assertThat(courses).extracting(CourseResponse::name)
                .containsExactlyInAnyOrder("중등 수학 A반", "중등 수학 B반");
    }

    @Test
    @DisplayName("isActive 필터로 활성 Course만 조회할 수 있다")
    void shouldFilterByActive_whenSearchingCourses() {
        // given
        createTestCourse("활성 반", teacherId);
        Course inactiveCourse = createTestCourse("비활성 반", teacherId);
        inactiveCourse.deactivate();
        courseRepository.save(inactiveCourse);

        // when
        List<CourseResponse> activeCourses = courseService.getCoursesByTeacher(teacherId, true);
        List<CourseResponse> inactiveCourses = courseService.getCoursesByTeacher(teacherId, false);

        // then
        assertThat(activeCourses).hasSize(1);
        assertThat(activeCourses.get(0).name()).isEqualTo("활성 반");
        assertThat(inactiveCourses).hasSize(1);
        assertThat(inactiveCourses.get(0).name()).isEqualTo("비활성 반");
    }

    @Test
    @DisplayName("Course가 없으면 빈 리스트를 반환한다")
    void shouldReturnEmptyList_whenNoCoursesExist() {
        // when
        List<CourseResponse> courses = courseService.getCoursesByTeacher(teacherId, null);

        // then
        assertThat(courses).isEmpty();
    }

    @Test
    @DisplayName("Course ID로 상세 정보를 조회할 수 있다")
    void shouldReturnCourse_whenCourseExists() {
        // given
        Course course = createTestCourse("중등 수학 A반", teacherId);

        // when
        CourseResponse response = courseService.getCourseById(course.getId(), teacherId);

        // then
        assertThat(response.id()).isEqualTo(course.getId());
        assertThat(response.name()).isEqualTo("중등 수학 A반");
    }

    @Test
    @DisplayName("존재하지 않는 Course ID로 조회하면 예외가 발생한다")
    void shouldThrowException_whenCourseNotFound() {
        // given
        UUID nonExistentId = UUID.randomUUID();

        // when & then
        assertThatThrownBy(() -> courseService.getCourseById(nonExistentId, teacherId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 Teacher의 Course를 조회하려고 하면 예외가 발생한다")
    void shouldThrowException_whenGettingOtherTeachersCourse() {
        // given
        Course course = createTestCourse("중등 수학 A반", teacherId);

        Member anotherTeacher = Member.builder()
                .email("another@test.com")
                .password("encoded")
                .name("Another Teacher")
                .role(MemberRole.TEACHER)
                .build();
        UUID anotherTeacherId = memberRepository.save(anotherTeacher).getId();

        // when & then
        assertThatThrownBy(() -> courseService.getCourseById(course.getId(), anotherTeacherId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("Course 정보를 수정할 수 있다")
    void shouldUpdateCourse_whenValidRequest() {
        // given
        Course course = createTestCourse("중등 수학 A반", teacherId);
        CourseUpdateRequest request = new CourseUpdateRequest(
                "중등 수학 B반",
                "XYZ 학원",
                Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                LocalTime.of(15, 0),
                LocalTime.of(17, 0)
        );

        // when
        CourseResponse response = courseService.updateCourse(course.getId(), teacherId, request);

        // then
        assertThat(response.name()).isEqualTo("중등 수학 B반");
        assertThat(response.company()).isEqualTo("XYZ 학원");
        assertThat(response.daysOfWeek()).contains(DayOfWeek.MONDAY);
        assertThat(response.startTime()).isEqualTo(LocalTime.of(15, 0));
        assertThat(response.endTime()).isEqualTo(LocalTime.of(17, 0));
    }

    @Test
    @DisplayName("다른 Teacher의 Course를 수정하려고 하면 예외가 발생한다")
    void shouldThrowException_whenUpdatingOtherTeachersCourse() {
        // given
        Course course = createTestCourse("중등 수학 A반", teacherId);
        UUID anotherTeacherId = createAnotherTeacher();
        CourseUpdateRequest request = new CourseUpdateRequest(
                "중등 수학 B반",
                null,
                null,
                null,
                null
        );

        // when & then
        assertThatThrownBy(() -> courseService.updateCourse(course.getId(), anotherTeacherId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("Course를 비활성화할 수 있다")
    void shouldDeactivateCourse_whenValidRequest() {
        // given
        Course course = createTestCourse("중등 수학 A반", teacherId);

        // when
        courseService.deactivateCourse(course.getId(), teacherId);

        // then
        Course updated = courseRepository.findById(course.getId()).orElseThrow();
        assertThat(updated.isActive()).isFalse();
    }

    @Test
    @DisplayName("다른 Teacher의 Course를 비활성화하려고 하면 예외가 발생한다")
    void shouldThrowException_whenDeactivatingOtherTeachersCourse() {
        // given
        Course course = createTestCourse("중등 수학 A반", teacherId);
        UUID anotherTeacherId = createAnotherTeacher();

        // when & then
        assertThatThrownBy(() -> courseService.deactivateCourse(course.getId(), anotherTeacherId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("Course를 활성화할 수 있다")
    void shouldActivateCourse_whenValidRequest() {
        // given
        Course course = createTestCourse("중등 수학 A반", teacherId);
        course.deactivate();
        courseRepository.save(course);

        // when
        courseService.activateCourse(course.getId(), teacherId);

        // then
        Course updated = courseRepository.findById(course.getId()).orElseThrow();
        assertThat(updated.isActive()).isTrue();
    }

    @Test
    @DisplayName("다른 Teacher의 Course를 활성화하려고 하면 예외가 발생한다")
    void shouldThrowException_whenActivatingOtherTeachersCourse() {
        // given
        Course course = createTestCourse("중등 수학 A반", teacherId);
        course.deactivate();
        courseRepository.save(course);
        UUID anotherTeacherId = createAnotherTeacher();

        // when & then
        assertThatThrownBy(() -> courseService.activateCourse(course.getId(), anotherTeacherId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("Teacher가 아니면 Course를 생성할 수 없다")
    void shouldThrowException_whenMemberIsNotTeacher() {
        // given
        Member student = memberRepository.save(
                Member.builder()
                        .email("student@test.com")
                        .password("encoded")
                        .name("Student User")
                        .role(MemberRole.STUDENT)
                        .build()
        );
        CourseCreateRequest request = new CourseCreateRequest(
                "중등 수학 A반",
                "ABC 학원",
                Set.of(DayOfWeek.MONDAY),
                LocalTime.of(14, 0),
                LocalTime.of(16, 0)
        );

        // when & then
        assertThatThrownBy(() -> courseService.createCourse(student.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.COURSE_FORBIDDEN);
    }

    @Test
    @DisplayName("존재하지 않는 Teacher로 요청하면 예외가 발생한다")
    void shouldThrowException_whenTeacherNotFound() {
        // given
        CourseCreateRequest request = new CourseCreateRequest(
                "중등 수학 A반",
                "ABC 학원",
                Set.of(DayOfWeek.MONDAY),
                LocalTime.of(14, 0),
                LocalTime.of(16, 0)
        );

        // when & then
        assertThatThrownBy(() -> courseService.createCourse(UUID.randomUUID(), request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 Course를 활성화하려 하면 예외가 발생한다")
    void shouldThrowException_whenActivatingNonExistingCourse() {
        // when & then
        assertThatThrownBy(() -> courseService.activateCourse(UUID.randomUUID(), teacherId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.COURSE_NOT_FOUND);
    }

    private Course createTestCourse(String name, UUID teacherId) {
        Course course = Course.builder()
                .name(name)
                .company("ABC 학원")
                .teacherId(teacherId)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(16, 0))
                .build();
        return courseRepository.save(course);
    }

    private UUID createAnotherTeacher() {
        return memberRepository.save(
                Member.builder()
                        .email(UUID.randomUUID() + "@test.com")
                        .password("encoded")
                        .name("Another Teacher")
                        .role(MemberRole.TEACHER)
                        .build()
        ).getId();
    }
}
