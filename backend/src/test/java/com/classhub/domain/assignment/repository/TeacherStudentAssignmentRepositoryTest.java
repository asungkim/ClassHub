package com.classhub.domain.assignment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.assignment.model.TeacherStudentAssignment;
import com.classhub.global.config.JpaConfig;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class TeacherStudentAssignmentRepositoryTest {

    @Autowired
    private TeacherStudentAssignmentRepository repository;

    @Test
    void existsByTeacherMemberIdAndStudentMemberIdAndDeletedAtIsNull_shouldRespectSoftDelete() {
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        TeacherStudentAssignment assignment = repository.save(
                TeacherStudentAssignment.create(teacherId, studentId)
        );

        assertThat(repository.existsByTeacherMemberIdAndStudentMemberIdAndDeletedAtIsNull(teacherId, studentId))
                .isTrue();

        assignment.disable();
        repository.save(assignment);

        assertThat(repository.existsByTeacherMemberIdAndStudentMemberIdAndDeletedAtIsNull(teacherId, studentId))
                .isFalse();
    }
}
