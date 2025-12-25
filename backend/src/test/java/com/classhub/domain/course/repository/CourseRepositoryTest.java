package com.classhub.domain.course.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.course.dto.CourseStatusFilter;
import com.classhub.domain.course.model.Course;
import com.classhub.global.config.JpaConfig;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class CourseRepositoryTest {

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void searchCourses_shouldFilterByStatusBranchAndKeyword() {
        UUID teacherId = UUID.randomUUID();
        UUID anotherTeacher = UUID.randomUUID();
        UUID branchA = UUID.randomUUID();
        UUID branchB = UUID.randomUUID();

        Course activeCourse = createCourse(branchA, teacherId, "중3 수학", LocalDate.now(), LocalDate.now().plusMonths(3));
        Course inactiveCourse = createCourse(branchA, teacherId, "고2 물리", LocalDate.now(), LocalDate.now().plusMonths(2));
        inactiveCourse.delete();
        Course otherBranch = createCourse(branchB, teacherId, "중2 영어", LocalDate.now(), LocalDate.now().plusMonths(1));
        Course otherTeacherCourse = createCourse(branchA, anotherTeacher, "중1 과학", LocalDate.now(), LocalDate.now().plusMonths(1));

        courseRepository.saveAll(List.of(activeCourse, inactiveCourse, otherBranch, otherTeacherCourse));

        Page<Course> result = courseRepository.searchCourses(
                teacherId,
                branchA,
                CourseStatusFilter.ACTIVE,
                "수학",
                PageRequest.of(0, 10)
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("중3 수학");
    }

    @Test
    void searchCourses_shouldReturnInactiveCourses() {
        UUID teacherId = UUID.randomUUID();
        Course inactiveCourse = createCourse(UUID.randomUUID(), teacherId, "테스트 과목", LocalDate.now(), LocalDate.now().plusMonths(1));
        inactiveCourse.delete();
        courseRepository.save(inactiveCourse);

        Page<Course> result = courseRepository.searchCourses(
                teacherId,
                null,
                CourseStatusFilter.INACTIVE,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().isDeleted()).isTrue();
    }

    @Test
    void findCoursesWithinPeriod_shouldReturnOverlappingCourses() {
        UUID teacherId = UUID.randomUUID();
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 31);

        Course overlapping = createCourse(UUID.randomUUID(), teacherId, "겨울 특강", start.minusDays(1), end.minusDays(10));
        Course outside = createCourse(UUID.randomUUID(), teacherId, "봄 특강", end.plusDays(1), end.plusMonths(1));
        Course deleted = createCourse(UUID.randomUUID(), teacherId, "비활성", start, end);
        deleted.delete();

        courseRepository.saveAll(List.of(overlapping, outside, deleted));

        List<Course> results = courseRepository.findCoursesWithinPeriod(teacherId, start, end);

        assertThat(results)
                .hasSize(1)
                .first()
                .extracting(Course::getName)
                .isEqualTo("겨울 특강");
    }

    @Test
    void searchAssignableCoursesForTeacher_shouldExcludeEndedOrDeleted() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        LocalDate today = LocalDate.now();

        Course active = createCourse(branchId, teacherId, "배치 가능", today.minusDays(1), today.plusDays(10));
        Course ended = createCourse(branchId, teacherId, "종료됨", today.minusDays(10), today.minusDays(1));
        Course deleted = createCourse(branchId, teacherId, "삭제됨", today.minusDays(1), today.plusDays(5));
        deleted.delete();
        courseRepository.saveAll(List.of(active, ended, deleted));

        Page<Course> result = courseRepository.searchAssignableCoursesForTeacher(
                teacherId,
                null,
                null,
                today,
                PageRequest.of(0, 10)
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("배치 가능");
    }

    @Test
    void searchAssignableCoursesForTeachers_shouldFilterByBranchAndKeyword() {
        UUID teacherId = UUID.randomUUID();
        UUID anotherTeacher = UUID.randomUUID();
        UUID branchA = UUID.randomUUID();
        UUID branchB = UUID.randomUUID();
        LocalDate today = LocalDate.now();

        Course match = createCourse(branchA, teacherId, "중3 과학", today, today.plusDays(7));
        Course otherBranch = createCourse(branchB, teacherId, "중3 과학", today, today.plusDays(7));
        Course otherTeacher = createCourse(branchA, anotherTeacher, "중3 과학", today, today.plusDays(7));
        courseRepository.saveAll(List.of(match, otherBranch, otherTeacher));

        Page<Course> result = courseRepository.searchAssignableCoursesForTeachers(
                List.of(teacherId),
                branchA,
                "과학",
                today,
                PageRequest.of(0, 10)
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getBranchId()).isEqualTo(branchA);
    }

    private Course createCourse(UUID branchId,
                                UUID teacherId,
                                String name,
                                LocalDate startDate,
                                LocalDate endDate) {
        Set<Course.CourseSchedule> schedules = Set.of(
                new Course.CourseSchedule(DayOfWeek.MONDAY, LocalTime.of(7, 0), LocalTime.of(8, 0))
        );
        return Course.create(branchId, teacherId, name, null, startDate, endDate, schedules);
    }
}
