package com.classhub.domain.studentcourse.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.studentcourse.model.StudentCourseAssignment;
import com.classhub.global.config.JpaConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class StudentCourseAssignmentRepositoryTest {

    @Autowired
    private StudentCourseAssignmentRepository repository;

    @Test
    void existsByStudentMemberIdAndCourseId_shouldReturnTrueWhenExists() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        StudentCourseAssignment assignment = StudentCourseAssignment.create(studentId, courseId, teacherId, null);
        repository.save(assignment);

        boolean exists = repository.existsByStudentMemberIdAndCourseId(studentId, courseId);

        assertThat(exists).isTrue();
    }

    @Test
    void findStudentMemberIdsByCourseId_shouldReturnAllAssignedStudents() {
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        StudentCourseAssignment first = StudentCourseAssignment.create(UUID.randomUUID(), courseId, teacherId, null);
        StudentCourseAssignment second = StudentCourseAssignment.create(UUID.randomUUID(), courseId, teacherId, null);
        repository.saveAll(List.of(first, second));

        List<UUID> studentIds = repository.findStudentMemberIdsByCourseId(courseId);

        assertThat(studentIds).containsExactlyInAnyOrder(first.getStudentMemberId(), second.getStudentMemberId());
    }
}
