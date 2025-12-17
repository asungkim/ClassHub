package com.classhub.domain.sharedlesson.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.model.CourseSchedule;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.sharedlesson.model.SharedLesson;
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
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SharedLessonRepositoryTest {

    @Autowired
    private SharedLessonRepository sharedLessonRepository;

    @Autowired
    private CourseRepository courseRepository;

    private UUID teacherId;
    private Course course;

    @BeforeEach
    void setUp() {
        sharedLessonRepository.deleteAll();
        courseRepository.deleteAll();
        teacherId = UUID.randomUUID();
        course = courseRepository.save(
                Course.builder()
                        .teacherId(teacherId)
                        .name("수학 반")
                        .company("ClassHub")
                        .schedules(new HashSet<>(Arrays.asList(
                                new CourseSchedule(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0))
                        )))
                        .build()
        );
    }

    @Test
    @DisplayName("Course와 Teacher 기준으로 SharedLesson 목록을 조회한다")
    void shouldFindAllByCourseAndTeacher() {
        createSharedLesson(LocalDate.of(2025, 1, 3), "세 번째");
        createSharedLesson(LocalDate.of(2025, 1, 2), "두 번째");
        createSharedLesson(LocalDate.of(2025, 1, 1), "첫 번째");

        Page<SharedLesson> page = sharedLessonRepository.findAllByCourse_TeacherIdAndCourse_Id(
                teacherId,
                course.getId(),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "date", "createdAt"))
        );

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting(SharedLesson::getTitle)
                .containsExactly("세 번째", "두 번째", "첫 번째");
    }

    @Test
    @DisplayName("날짜 범위로 SharedLesson을 조회한다")
    void shouldFindAllByCourseAndTeacherWithinDateRange() {
        createSharedLesson(LocalDate.of(2025, 1, 3), "세 번째");
        createSharedLesson(LocalDate.of(2025, 1, 2), "두 번째");
        createSharedLesson(LocalDate.of(2025, 1, 1), "첫 번째");

        Page<SharedLesson> page = sharedLessonRepository.findAllByCourse_TeacherIdAndCourse_IdAndDateBetween(
                teacherId,
                course.getId(),
                LocalDate.of(2025, 1, 2),
                LocalDate.of(2025, 1, 3),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "date", "createdAt"))
        );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(SharedLesson::getTitle)
                .containsExactly("세 번째", "두 번째");
    }

    @Test
    @DisplayName("Teacher가 다르면 SharedLesson 조회 결과가 비어있다")
    void shouldReturnEmptyWhenTeacherMismatch() {
        SharedLesson sharedLesson = createSharedLesson(LocalDate.now(), "테스트");

        assertThat(sharedLessonRepository.findByIdAndCourse_TeacherId(
                sharedLesson.getId(),
                UUID.randomUUID())
        ).isEmpty();
    }

    @Test
    @DisplayName("Teacher가 일치하면 SharedLesson을 찾을 수 있다")
    void shouldFindLessonByIdAndTeacher() {
        SharedLesson sharedLesson = createSharedLesson(LocalDate.now(), "테스트");

        assertThat(sharedLessonRepository.findByIdAndCourse_TeacherId(sharedLesson.getId(), teacherId))
                .isPresent();
    }

    private SharedLesson createSharedLesson(LocalDate date, String title) {
        return sharedLessonRepository.save(
                SharedLesson.builder()
                        .course(course)
                        .writerId(teacherId)
                        .date(date)
                        .title(title)
                        .content("내용")
                        .build()
        );
    }
}
