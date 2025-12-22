package com.classhub.domain.studentcourse.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.global.config.JpaConfig;
import java.time.LocalDate;
import java.time.Month;
import java.util.Set;
import java.util.UUID;
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
    private StudentCourseRecordRepository studentCourseRecordRepository;
    @Autowired
    private CourseRepository courseRepository;

    @Test
    void countByDefaultClinicSlotIdAndDeletedAtIsNull_shouldCountOnlyActiveRecords() {
        UUID slotId = UUID.randomUUID();
        Course course = courseRepository.save(createCourse());

        studentCourseRecordRepository.save(
                StudentCourseRecord.create(UUID.randomUUID(), course.getId(), null, slotId, null)
        );
        studentCourseRecordRepository.save(
                StudentCourseRecord.create(UUID.randomUUID(), course.getId(), null, UUID.randomUUID(), null)
        );
        StudentCourseRecord deletedRecord = studentCourseRecordRepository.save(
                StudentCourseRecord.create(UUID.randomUUID(), course.getId(), null, slotId, null)
        );
        deletedRecord.delete();
        studentCourseRecordRepository.save(deletedRecord);

        long count = studentCourseRecordRepository.countByDefaultClinicSlotIdAndDeletedAtIsNull(slotId);

        assertThat(count).isEqualTo(1L);
    }

    private Course createCourse() {
        return Course.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Course",
                "Desc",
                LocalDate.of(2024, Month.JANUARY, 1),
                LocalDate.of(2024, Month.DECEMBER, 31),
                Set.of()
        );
    }
}
