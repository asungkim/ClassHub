package com.classhub.domain.studentcourseenrollment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.studentcourseenrollment.model.StudentCourseEnrollment;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class StudentCourseEnrollmentRepositoryTest {

    @Autowired
    private StudentCourseEnrollmentRepository repository;

    private UUID studentProfileId;
    private UUID teacherId;
    private UUID firstCourseId;
    private UUID secondCourseId;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        studentProfileId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        firstCourseId = UUID.randomUUID();
        secondCourseId = UUID.randomUUID();
    }

    @Test
    @DisplayName("학생은 여러 Course에 등록될 수 있고 조회할 수 있다")
    void findAllByStudentProfileId() {
        repository.save(StudentCourseEnrollment.builder()
                .studentProfileId(studentProfileId)
                .courseId(firstCourseId)
                .teacherId(teacherId)
                .build());
        repository.save(StudentCourseEnrollment.builder()
                .studentProfileId(studentProfileId)
                .courseId(secondCourseId)
                .teacherId(teacherId)
                .build());

        List<StudentCourseEnrollment> enrollments = repository.findAllByStudentProfileId(studentProfileId);

        assertThat(enrollments).hasSize(2);
        assertThat(enrollments).extracting(StudentCourseEnrollment::getCourseId)
                .containsExactlyInAnyOrder(firstCourseId, secondCourseId);
    }

    @Test
    @DisplayName("Course 기준으로 학생 목록을 조회할 수 있다")
    void findAllByCourseId() {
        UUID otherStudentId = UUID.randomUUID();
        repository.save(StudentCourseEnrollment.builder()
                .studentProfileId(studentProfileId)
                .courseId(firstCourseId)
                .teacherId(teacherId)
                .build());
        repository.save(StudentCourseEnrollment.builder()
                .studentProfileId(otherStudentId)
                .courseId(firstCourseId)
                .teacherId(teacherId)
                .build());

        List<StudentCourseEnrollment> enrollments = repository.findAllByCourseId(firstCourseId);

        assertThat(enrollments).hasSize(2);
        assertThat(enrollments).extracting(StudentCourseEnrollment::getStudentProfileId)
                .containsExactlyInAnyOrder(studentProfileId, otherStudentId);
    }

    @Test
    @DisplayName("학생과 Course 조합의 중복 등록 여부를 확인한다")
    void existsByStudentProfileIdAndCourseId() {
        repository.save(StudentCourseEnrollment.builder()
                .studentProfileId(studentProfileId)
                .courseId(firstCourseId)
                .teacherId(teacherId)
                .build());

        boolean exists = repository.existsByStudentProfileIdAndCourseId(studentProfileId, firstCourseId);
        boolean notExists = repository.existsByStudentProfileIdAndCourseId(studentProfileId, secondCourseId);

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
}
