package com.classhub.domain.studentcourse.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.global.config.JpaConfig;
import java.time.LocalDate;
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
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class StudentCourseRecordRepositoryTest {

    @Autowired
    private StudentCourseRecordRepository recordRepository;
    @Autowired
    private CourseRepository courseRepository;

    private UUID studentId;
    private UUID teacherA;
    private UUID teacherB;
    private Course courseA1;
    private Course courseB1;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        teacherA = UUID.randomUUID();
        teacherB = UUID.randomUUID();
        courseA1 = courseRepository.save(createCourse(teacherA));
        courseB1 = courseRepository.save(createCourse(teacherB));
    }

    @Test
    @DisplayName("교사별 학생 기록 조회는 해당 교사의 반만 반환한다")
    void findActiveByStudentIdAndTeacherId_shouldFilterByTeacher() {
        StudentCourseRecord activeA = recordRepository.save(StudentCourseRecord.create(studentId, courseA1.getId(), null, null, null));
        StudentCourseRecord deleted = recordRepository.save(StudentCourseRecord.create(studentId, courseA1.getId(), null, null, null));
        deleted.delete();
        recordRepository.save(deleted);
        recordRepository.save(StudentCourseRecord.create(studentId, courseB1.getId(), null, null, null));

        List<StudentCourseRecord> results = recordRepository.findActiveByStudentIdAndTeacherId(studentId, teacherA);

        assertThat(results)
                .containsExactly(activeA);
    }

    @Test
    @DisplayName("여러 교사 조건 조회는 목록에 포함된 교사의 반만 반환한다")
    void findActiveByStudentIdAndTeacherIds_shouldFilterByTeachers() {
        StudentCourseRecord recordA = recordRepository.save(StudentCourseRecord.create(studentId, courseA1.getId(), null, null, null));
        StudentCourseRecord recordB = recordRepository.save(StudentCourseRecord.create(studentId, courseB1.getId(), null, null, null));
        UUID otherTeacher = UUID.randomUUID();
        Course otherCourse = courseRepository.save(createCourse(otherTeacher));
        recordRepository.save(StudentCourseRecord.create(studentId, otherCourse.getId(), null, null, null));

        List<StudentCourseRecord> results = recordRepository
                .findActiveByStudentIdAndTeacherIds(studentId, List.of(teacherA, teacherB));

        assertThat(results)
                .containsExactlyInAnyOrder(recordA, recordB);
    }

    private Course createCourse(UUID owner) {
        return Course.create(
                UUID.randomUUID(),
                owner,
                "Course",
                "Desc",
                LocalDate.of(2024, Month.JANUARY, 1),
                LocalDate.of(2024, Month.DECEMBER, 31),
                Set.of()
        );
    }
}
