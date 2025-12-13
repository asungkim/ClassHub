package com.classhub.domain.course.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.model.CourseSchedule;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CourseRepositoryTest {

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void shouldSaveAndFindCourse_whenValidCourse() {
        // given
        UUID teacherId = UUID.randomUUID();
        Course course = Course.builder()
                .name("중등 수학 A반")
                .company("ABC 학원")
                .teacherId(teacherId)
                .schedules(scheduleEntities(
                        schedule(DayOfWeek.MONDAY, 14, 0, 16, 0),
                        schedule(DayOfWeek.FRIDAY, 14, 0, 16, 0)
                ))
                .build();

        // when
        Course saved = courseRepository.save(course);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("중등 수학 A반");
        assertThat(saved.getCompany()).isEqualTo("ABC 학원");
        assertThat(saved.getTeacherId()).isEqualTo(teacherId);
        assertThat(saved.getSchedules()).hasSize(2);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindByTeacherId_whenCoursesExist() {
        // given
        UUID teacherId = UUID.randomUUID();
        Course course1 = createCourse("중등 수학 A반", teacherId);
        Course course2 = createCourse("중등 수학 B반", teacherId);
        courseRepository.save(course1);
        courseRepository.save(course2);

        // when
        List<Course> courses = courseRepository.findByTeacherId(teacherId);

        // then
        assertThat(courses).hasSize(2);
        assertThat(courses).extracting(Course::getName)
                .containsExactlyInAnyOrder("중등 수학 A반", "중등 수학 B반");
    }

    @Test
    void shouldFilterByActive_whenSearchingByTeacherIdAndActive() {
        // given
        UUID teacherId = UUID.randomUUID();
        Course activeCourse = createCourse("활성 반", teacherId);
        Course inactiveCourse = createCourse("비활성 반", teacherId);
        inactiveCourse.deactivate();

        courseRepository.save(activeCourse);
        courseRepository.save(inactiveCourse);

        // when
        List<Course> activeCourses = courseRepository.findByTeacherIdAndActive(teacherId, true);
        List<Course> inactiveCourses = courseRepository.findByTeacherIdAndActive(teacherId, false);

        // then
        assertThat(activeCourses).hasSize(1);
        assertThat(activeCourses.get(0).getName()).isEqualTo("활성 반");
        assertThat(inactiveCourses).hasSize(1);
        assertThat(inactiveCourses.get(0).getName()).isEqualTo("비활성 반");
    }

    @Test
    void shouldReturnEmptyList_whenNoCoursesExist() {
        // given
        UUID teacherId = UUID.randomUUID();

        // when
        List<Course> courses = courseRepository.findByTeacherId(teacherId);

        // then
        assertThat(courses).isEmpty();
    }

    @Test
    void shouldFindByIdAndTeacherId_whenCourseExists() {
        // given
        UUID teacherId = UUID.randomUUID();
        Course course = createCourse("중등 수학 A반", teacherId);
        Course saved = courseRepository.save(course);

        // when
        var found = courseRepository.findByIdAndTeacherId(saved.getId(), teacherId);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("중등 수학 A반");
    }

    @Test
    void shouldReturnEmpty_whenFindingByWrongTeacherId() {
        // given
        UUID teacherId = UUID.randomUUID();
        UUID anotherTeacherId = UUID.randomUUID();
        Course course = createCourse("중등 수학 A반", teacherId);
        Course saved = courseRepository.save(course);

        // when
        var found = courseRepository.findByIdAndTeacherId(saved.getId(), anotherTeacherId);

        // then
        assertThat(found).isEmpty();
    }

    private Course createCourse(String name, UUID teacherId) {
        return Course.builder()
                .name(name)
                .company("ABC 학원")
                .teacherId(teacherId)
                .schedules(scheduleEntities(
                        schedule(DayOfWeek.MONDAY, 14, 0, 16, 0),
                        schedule(DayOfWeek.FRIDAY, 14, 0, 16, 0)
                ))
                .build();
    }

    private CourseSchedule schedule(DayOfWeek day, int startHour, int startMinute, int endHour, int endMinute) {
        return new CourseSchedule(day, LocalTime.of(startHour, startMinute), LocalTime.of(endHour, endMinute));
    }

    private java.util.Set<CourseSchedule> scheduleEntities(CourseSchedule... schedules) {
        return new java.util.HashSet<>(java.util.Arrays.asList(schedules));
    }
}
