package com.classhub.domain.progress.personal.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.progress.personal.model.PersonalProgress;
import com.classhub.domain.progress.personal.repository.PersonalProgressRepository;
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
class PersonalProgressRepositoryTest {

    @Autowired
    private PersonalProgressRepository personalProgressRepository;
    @Autowired
    private StudentCourseRecordRepository studentCourseRecordRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private UUID teacherId;
    private UUID studentId;
    private StudentCourseRecord record;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        Course course = courseRepository.save(createCourse(teacherId));
        record = studentCourseRecordRepository.save(StudentCourseRecord.create(studentId, course.getId(), null, null, null));
    }

    @Test
    @DisplayName("최근 PersonalProgress 조회는 createdAt/id 역순과 커서를 따른다")
    void findRecentByRecordId_shouldApplyOrderingAndCursor() {
        LocalDateTime base = LocalDateTime.of(2024, Month.MARCH, 1, 8, 0);
        PersonalProgress oldest = persistProgress(record, base, LocalDate.of(2024, 3, 3), "Note A");
        PersonalProgress middle = persistProgress(record, base.plusMinutes(15), LocalDate.of(2024, 3, 4), "Note B");
        PersonalProgress newest = persistProgress(record, base.plusMinutes(30), LocalDate.of(2024, 3, 5), "Note C");

        List<PersonalProgress> firstBatch = personalProgressRepository
                .findRecentByRecordId(record.getId(), null, null, PageRequest.of(0, 2));

        assertThat(firstBatch)
                .extracting(PersonalProgress::getTitle)
                .containsExactly("Note C", "Note B");

        PersonalProgress cursor = firstBatch.get(firstBatch.size() - 1);
        List<PersonalProgress> nextBatch = personalProgressRepository
                .findRecentByRecordId(record.getId(), cursor.getCreatedAt(), cursor.getId(), PageRequest.of(0, 5));

        assertThat(nextBatch)
                .extracting(PersonalProgress::getTitle)
                .containsExactly("Note A");
    }

    @Test
    @DisplayName("학생 기준 월별 PersonalProgress 조회는 기간과 학생 등록 여부에 따라 필터링된다")
    void findByStudentAndDateRange_shouldReturnOnlyMatchingRecords() {
        LocalDate start = LocalDate.of(2024, Month.MARCH, 1);
        LocalDate end = LocalDate.of(2024, Month.MARCH, 31);

        persistProgress(record, LocalDateTime.of(2024, 3, 5, 9, 0), LocalDate.of(2024, 3, 5), "Included 1");
        persistProgress(record, LocalDateTime.of(2024, 4, 1, 9, 0), LocalDate.of(2024, 4, 1), "Excluded future");

        Course otherCourse = courseRepository.save(createCourse(UUID.randomUUID()));
        StudentCourseRecord otherRecord = studentCourseRecordRepository.save(
                StudentCourseRecord.create(studentId, otherCourse.getId(), null, null, null)
        );
        persistProgress(otherRecord, LocalDateTime.of(2024, 3, 10, 9, 0), LocalDate.of(2024, 3, 10), "Included 2");

        UUID anotherStudent = UUID.randomUUID();
        StudentCourseRecord anotherRecord = studentCourseRecordRepository.save(
                StudentCourseRecord.create(anotherStudent, otherCourse.getId(), null, null, null)
        );
        persistProgress(anotherRecord, LocalDateTime.of(2024, 3, 12, 9, 0), LocalDate.of(2024, 3, 12), "Excluded other student");

        List<PersonalProgress> results = personalProgressRepository.findByStudentAndDateRange(studentId, start, end);

        assertThat(results)
                .extracting(PersonalProgress::getTitle)
                .containsExactly("Included 1", "Included 2");
    }

    @Test
    @DisplayName("StudentCourseRecord 목록 기준 월별 조회는 지정된 기록만 반환한다")
    void findByRecordIdsAndDateRange_shouldFilterByRecordIds() {
        LocalDate start = LocalDate.of(2024, Month.MARCH, 1);
        LocalDate end = LocalDate.of(2024, Month.MARCH, 31);

        persistProgress(record, LocalDateTime.of(2024, 3, 5, 9, 0), LocalDate.of(2024, 3, 5), "Included A");
        persistProgress(record, LocalDateTime.of(2024, 4, 1, 9, 0), LocalDate.of(2024, 4, 1), "Excluded future");

        Course otherCourse = courseRepository.save(createCourse(UUID.randomUUID()));
        StudentCourseRecord otherRecord = studentCourseRecordRepository.save(
                StudentCourseRecord.create(studentId, otherCourse.getId(), null, null, null)
        );
        persistProgress(otherRecord, LocalDateTime.of(2024, 3, 8, 9, 0), LocalDate.of(2024, 3, 8), "Included B");

        UUID anotherStudent = UUID.randomUUID();
        StudentCourseRecord unrelated = studentCourseRecordRepository.save(
                StudentCourseRecord.create(anotherStudent, otherCourse.getId(), null, null, null)
        );
        persistProgress(unrelated, LocalDateTime.of(2024, 3, 6, 9, 0), LocalDate.of(2024, 3, 6), "Excluded C");

        List<PersonalProgress> results = personalProgressRepository.findByRecordIdsAndDateRange(
                List.of(record.getId(), otherRecord.getId()),
                start,
                end
        );

        assertThat(results)
                .extracting(PersonalProgress::getTitle)
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

    private PersonalProgress persistProgress(StudentCourseRecord record,
                                              LocalDateTime createdAt,
                                              LocalDate lessonDate,
                                              String title) {
        PersonalProgress progress = PersonalProgress.builder()
                .studentCourseRecordId(record.getId())
                .writerId(UUID.randomUUID())
                .date(lessonDate)
                .title(title)
                .content("memo")
                .build();
        ReflectionTestUtils.setField(progress, "createdAt", createdAt);
        ReflectionTestUtils.setField(progress, "updatedAt", createdAt);
        PersonalProgress saved = personalProgressRepository.save(progress);
        entityManager.flush();
        return saved;
    }
}
