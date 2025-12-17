package com.classhub.domain.sharedlesson.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.model.CourseSchedule;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.sharedlesson.dto.request.SharedLessonCreateRequest;
import com.classhub.domain.sharedlesson.dto.request.SharedLessonUpdateRequest;
import com.classhub.domain.sharedlesson.dto.response.SharedLessonResponse;
import com.classhub.domain.sharedlesson.dto.response.SharedLessonSummary;
import com.classhub.domain.sharedlesson.repository.SharedLessonRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SharedLessonServiceTest {

    @Autowired
    private SharedLessonService sharedLessonService;

    @Autowired
    private SharedLessonRepository sharedLessonRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Member teacher;
    private Member anotherTeacher;
    private Course course;

    @BeforeEach
    void setUp() {
        sharedLessonRepository.deleteAll();
        courseRepository.deleteAll();
        memberRepository.deleteAll();

        teacher = memberRepository.save(
                Member.builder()
                        .email("teacher@classhub.com")
                        .password(passwordEncoder.encode("Classhub!1"))
                        .name("Teacher Kim")
                        .role(MemberRole.TEACHER)
                        .build()
        );

        anotherTeacher = memberRepository.save(
                Member.builder()
                        .email("teacher2@classhub.com")
                        .password(passwordEncoder.encode("Classhub!1"))
                        .name("Teacher Han")
                        .role(MemberRole.TEACHER)
                        .build()
        );

        course = courseRepository.save(
                Course.builder()
                        .teacherId(teacher.getId())
                        .name("공통진도반")
                        .company("ClassHub")
                        .schedules(new HashSet<>(Arrays.asList(
                                new CourseSchedule(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0))
                        )))
                        .build()
        );
    }

    @Test
    @DisplayName("Teacher는 SharedLesson을 생성할 수 있다")
    void shouldCreateSharedLesson() {
        SharedLessonResponse response = sharedLessonService.createLesson(
                teacher.getId(),
                new SharedLessonCreateRequest(
                        course.getId(),
                        LocalDate.of(2025, 1, 10),
                        "1주차",
                        "교재 10~20p"
                )
        );

        assertThat(response.title()).isEqualTo("1주차");
        assertThat(response.date()).isEqualTo(LocalDate.of(2025, 1, 10));
    }

    @Test
    @DisplayName("date를 지정하지 않으면 오늘 날짜로 저장된다")
    void shouldAssignTodayWhenDateIsNull() {
        SharedLessonResponse response = sharedLessonService.createLesson(
                teacher.getId(),
                new SharedLessonCreateRequest(
                        course.getId(),
                        null,
                        "오늘 수업",
                        "교재 30~32p"
                )
        );

        assertThat(response.date()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("다른 Teacher의 Course에는 SharedLesson을 생성할 수 없다")
    void shouldFailToCreateWhenCourseNotOwned() {
        Course otherCourse = courseRepository.save(
                Course.builder()
                        .teacherId(anotherTeacher.getId())
                        .name("타반")
                        .company("ClassHub")
                        .schedules(new HashSet<>(Arrays.asList(
                                new CourseSchedule(DayOfWeek.TUESDAY, LocalTime.of(13, 0), LocalTime.of(15, 0))
                        )))
                        .build()
        );

        assertThatThrownBy(() -> sharedLessonService.createLesson(
                teacher.getId(),
                new SharedLessonCreateRequest(
                        otherCourse.getId(),
                        LocalDate.now(),
                        "무단 생성",
                        "내용"
                )
        )).isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.COURSE_FORBIDDEN);
    }

    @Test
    @DisplayName("SharedLesson 목록을 조회할 수 있다")
    void shouldGetLessons() {
        sharedLessonService.createLesson(
                teacher.getId(),
                new SharedLessonCreateRequest(course.getId(), LocalDate.of(2025, 1, 1), "1주차", "내용")
        );
        sharedLessonService.createLesson(
                teacher.getId(),
                new SharedLessonCreateRequest(course.getId(), LocalDate.of(2025, 1, 2), "2주차", "내용")
        );

        Page<SharedLessonSummary> page = sharedLessonService.getLessons(
                teacher.getId(),
                course.getId(),
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 2),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "date", "createdAt"))
        );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(SharedLessonSummary::title)
                .containsExactly("2주차", "1주차");
    }

    @Test
    @DisplayName("SharedLesson 목록 조회 시 Course 소유가 아니면 실패한다")
    void shouldFailToGetLessonsWhenNotOwner() {
        assertThatThrownBy(() -> sharedLessonService.getLessons(
                anotherTeacher.getId(),
                course.getId(),
                null,
                null,
                PageRequest.of(0, 10)
        )).isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.COURSE_FORBIDDEN);
    }

    @Test
    @DisplayName("SharedLesson 단건 조회에 성공한다")
    void shouldGetLessonById() {
        SharedLessonResponse created = sharedLessonService.createLesson(
                teacher.getId(),
                new SharedLessonCreateRequest(course.getId(), LocalDate.now(), "1주차", "내용")
        );

        SharedLessonResponse found = sharedLessonService.getLesson(teacher.getId(), created.id());
        assertThat(found.title()).isEqualTo("1주차");
    }

    @Test
    @DisplayName("SharedLesson 단건 조회 시 권한 없으면 실패한다")
    void shouldFailToGetLessonWhenNotOwner() {
        SharedLessonResponse created = sharedLessonService.createLesson(
                teacher.getId(),
                new SharedLessonCreateRequest(course.getId(), LocalDate.now(), "1주차", "내용")
        );

        assertThatThrownBy(() -> sharedLessonService.getLesson(anotherTeacher.getId(), created.id()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.SHARED_LESSON_NOT_FOUND);
    }

    @Test
    @DisplayName("SharedLesson을 수정할 수 있다")
    void shouldUpdateLesson() {
        SharedLessonResponse created = sharedLessonService.createLesson(
                teacher.getId(),
                new SharedLessonCreateRequest(course.getId(), LocalDate.now(), "1주차", "내용")
        );

        SharedLessonResponse updated = sharedLessonService.updateLesson(
                teacher.getId(),
                created.id(),
                new SharedLessonUpdateRequest(LocalDate.now().plusDays(1), "업데이트", "변경된 내용")
        );

        assertThat(updated.title()).isEqualTo("업데이트");
        assertThat(updated.content()).isEqualTo("변경된 내용");
    }

    @Test
    @DisplayName("SharedLesson 수정 시 권한 없으면 실패한다")
    void shouldFailToUpdateWhenNotOwner() {
        SharedLessonResponse created = sharedLessonService.createLesson(
                teacher.getId(),
                new SharedLessonCreateRequest(course.getId(), LocalDate.now(), "1주차", "내용")
        );

        assertThatThrownBy(() -> sharedLessonService.updateLesson(
                anotherTeacher.getId(),
                created.id(),
                new SharedLessonUpdateRequest(null, "수정", "내용")
        )).isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.SHARED_LESSON_NOT_FOUND);
    }

    @Test
    @DisplayName("SharedLesson을 삭제할 수 있다")
    void shouldDeleteLesson() {
        SharedLessonResponse created = sharedLessonService.createLesson(
                teacher.getId(),
                new SharedLessonCreateRequest(course.getId(), LocalDate.now(), "1주차", "내용")
        );

        sharedLessonService.deleteLesson(teacher.getId(), created.id());
        assertThat(sharedLessonRepository.findById(created.id())).isEmpty();
    }

    @Test
    @DisplayName("SharedLesson 삭제 시 권한 없으면 실패한다")
    void shouldFailToDeleteWhenNotOwner() {
        SharedLessonResponse created = sharedLessonService.createLesson(
                teacher.getId(),
                new SharedLessonCreateRequest(course.getId(), LocalDate.now(), "1주차", "내용")
        );

        assertThatThrownBy(() -> sharedLessonService.deleteLesson(anotherTeacher.getId(), created.id()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.SHARED_LESSON_NOT_FOUND);
    }
}
