package com.classhub.domain.progress.course.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.progress.course.model.CourseProgress;
import com.classhub.domain.progress.course.repository.CourseProgressRepository;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.config.JpaConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class CourseProgressRepositoryTest {

    @Autowired
    private CourseProgressRepository courseProgressRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private StudentCourseRecordRepository studentCourseRecordRepository;
    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private UUID teacherId;
    private UUID studentId;
    private Course course;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        course = courseRepository.save(createCourse(teacherId));
        studentCourseRecordRepository.save(StudentCourseRecord.create(studentId, course.getId(), null, null, null));
    }

    @Test
    @DisplayName("최근 CourseProgress 조회는 createdAt/id 역순과 커서를 따른다")
    void findRecentByCourseId_shouldApplyOrderingAndCursor() {
        LocalDateTime base = LocalDateTime.of(2024, Month.MARCH, 1, 10, 0);
        CourseProgress oldest = persistProgress(course, base, LocalDate.of(2024, 3, 3), "Lesson A");
        CourseProgress middle = persistProgress(course, base.plusMinutes(10), LocalDate.of(2024, 3, 4), "Lesson B");
        CourseProgress newest = persistProgress(course, base.plusMinutes(20), LocalDate.of(2024, 3, 5), "Lesson C");
        Course otherCourse = courseRepository.save(createCourse(teacherId));
        persistProgress(otherCourse, base.plusMinutes(30), LocalDate.of(2024, 3, 6), "Other course");

        List<CourseProgress> firstBatch = courseProgressRepository
                .findRecentByCourseId(course.getId(), null, null, PageRequest.of(0, 2));

        assertThat(firstBatch)
                .extracting(CourseProgress::getTitle)
                .containsExactly("Lesson C", "Lesson B");

        CourseProgress cursor = firstBatch.get(firstBatch.size() - 1);
        List<CourseProgress> nextBatch = courseProgressRepository
                .findRecentByCourseId(course.getId(), cursor.getCreatedAt(), cursor.getId(), PageRequest.of(0, 5));

        assertThat(nextBatch)
                .extracting(CourseProgress::getTitle)
                .containsExactly("Lesson A");

        assertThat(nextBatch).allMatch(progress -> progress.getCourseId().equals(course.getId()));
    }

    @Test
    @DisplayName("학생 기준 월별 CourseProgress 조회는 기간과 학생 등록 여부에 따라 필터링된다")
    void findByStudentAndDateRange_shouldFilterByStudentEnrollments() {
        LocalDate start = LocalDate.of(2024, Month.MARCH, 1);
        LocalDate end = LocalDate.of(2024, Month.MARCH, 31);

        persistProgress(course, LocalDateTime.of(2024, 3, 5, 9, 0), LocalDate.of(2024, 3, 5), "Included 1");
        persistProgress(course, LocalDateTime.of(2024, 4, 1, 9, 0), LocalDate.of(2024, 4, 1), "Excluded future");

        Course otherCourse = courseRepository.save(createCourse(UUID.randomUUID()));
        studentCourseRecordRepository.save(StudentCourseRecord.create(studentId, otherCourse.getId(), null, null, null));
        persistProgress(otherCourse, LocalDateTime.of(2024, 3, 10, 8, 0), LocalDate.of(2024, 3, 10), "Included 2");

        Course unrelated = courseRepository.save(createCourse(UUID.randomUUID()));
        persistProgress(unrelated, LocalDateTime.of(2024, 3, 7, 8, 0), LocalDate.of(2024, 3, 7), "Excluded 3");

        List<CourseProgress> results = courseProgressRepository.findByStudentAndDateRange(studentId, start, end);

        assertThat(results)
                .extracting(CourseProgress::getTitle)
                .containsExactly("Included 1", "Included 2");
    }

    @Test
    @DisplayName("Course 목록 기준 월별 조회는 해당 Course만 반환한다")
    void findByCourseIdsAndDateRange_shouldFilterByCourseIds() {
        LocalDate start = LocalDate.of(2024, Month.MARCH, 1);
        LocalDate end = LocalDate.of(2024, Month.MARCH, 31);

        persistProgress(course, LocalDateTime.of(2024, 3, 4, 9, 0), LocalDate.of(2024, 3, 4), "Included A");
        persistProgress(course, LocalDateTime.of(2024, 4, 1, 9, 0), LocalDate.of(2024, 4, 1), "Excluded future");

        Course otherCourse = courseRepository.save(createCourse(UUID.randomUUID()));
        persistProgress(otherCourse, LocalDateTime.of(2024, 3, 6, 9, 0), LocalDate.of(2024, 3, 6), "Included B");

        Course unrelated = courseRepository.save(createCourse(UUID.randomUUID()));
        persistProgress(unrelated, LocalDateTime.of(2024, 3, 5, 9, 0), LocalDate.of(2024, 3, 5), "Excluded C");

        List<CourseProgress> results = courseProgressRepository.findByCourseIdsAndDateRange(
                List.of(course.getId(), otherCourse.getId()),
                start,
                end
        );

        assertThat(results)
                .extracting(CourseProgress::getTitle)
                .containsExactly("Included A", "Included B");
    }

    private Course createCourse(UUID ownerId) {
        return Course.create(
                UUID.randomUUID(),
                ownerId,
                "Course",
                "Desc",
                LocalDate.of(2024, Month.JANUARY, 1),
                LocalDate.of(2024, Month.DECEMBER, 31),
                Set.of()
        );
    }

    private CourseProgress persistProgress(Course course,
                                           LocalDateTime createdAt,
                                           LocalDate lessonDate,
                                           String title) {
        CourseProgress progress = CourseProgress.builder()
                .courseId(course.getId())
                .writerId(course.getTeacherMemberId())
                .date(lessonDate)
                .title(title)
                .content("content")
                .build();
        ReflectionTestUtils.setField(progress, "createdAt", createdAt);
        ReflectionTestUtils.setField(progress, "updatedAt", createdAt);
        CourseProgress saved = courseProgressRepository.save(progress);
        entityManager.flush();
        return saved;
    }
}
