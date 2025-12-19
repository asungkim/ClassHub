package com.classhub.domain.assignment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.assignment.model.TeacherAssistantAssignment;
import com.classhub.global.config.JpaConfig;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
class TeacherAssistantAssignmentRepositoryTest {

    @Autowired
    private TeacherAssistantAssignmentRepository repository;

    private UUID teacherId;
    private UUID assistantId1;
    private UUID assistantId2;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        assistantId1 = UUID.randomUUID();
        assistantId2 = UUID.randomUUID();
    }

    @Test
    void findByTeacherMemberIdAndDeletedAtIsNull_shouldReturnOnlyActiveAssignments() {
        TeacherAssistantAssignment active = repository.save(
                TeacherAssistantAssignment.create(teacherId, assistantId1)
        );
        TeacherAssistantAssignment inactive = repository.save(
                TeacherAssistantAssignment.create(teacherId, assistantId2)
        );
        inactive.disable();
        repository.save(inactive);

        Page<TeacherAssistantAssignment> result = repository
                .findByTeacherMemberIdAndDeletedAtIsNull(teacherId, PageRequest.of(0, 10));

        assertThat(result.getContent())
                .hasSize(1)
                .first()
                .extracting(TeacherAssistantAssignment::getId)
                .isEqualTo(active.getId());
    }

    @Test
    void findByTeacherMemberIdAndDeletedAtIsNotNull_shouldReturnOnlyDisabledAssignments() {
        TeacherAssistantAssignment active = repository.save(
                TeacherAssistantAssignment.create(teacherId, assistantId1)
        );
        active.disable();
        repository.save(active);
        repository.save(TeacherAssistantAssignment.create(teacherId, assistantId2));

        Page<TeacherAssistantAssignment> result = repository
                .findByTeacherMemberIdAndDeletedAtIsNotNull(teacherId, PageRequest.of(0, 10));

        assertThat(result.getContent())
                .hasSize(1)
                .first()
                .extracting(TeacherAssistantAssignment::getId)
                .isEqualTo(active.getId());
    }

    @Test
    void findByTeacherMemberId_shouldReturnAllAssignmentsForTeacher() {
        TeacherAssistantAssignment a1 = repository.save(
                TeacherAssistantAssignment.create(teacherId, assistantId1)
        );
        TeacherAssistantAssignment a2 = repository.save(
                TeacherAssistantAssignment.create(teacherId, assistantId2)
        );

        Page<TeacherAssistantAssignment> result = repository
                .findByTeacherMemberId(teacherId, PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(TeacherAssistantAssignment::getId)
                .containsExactlyInAnyOrder(a1.getId(), a2.getId());
    }

    @Test
    void findByIdAndTeacherMemberId_shouldReturnAssignment_whenTeacherOwnsIt() {
        TeacherAssistantAssignment assignment = repository.save(
                TeacherAssistantAssignment.create(teacherId, assistantId1)
        );

        assertThat(repository.findByIdAndTeacherMemberId(assignment.getId(), teacherId))
                .isPresent();

        assertThat(repository.findByIdAndTeacherMemberId(assignment.getId(), UUID.randomUUID()))
                .isEmpty();
    }

    @Test
    void findByTeacherMemberIdAndAssistantMemberIdIn_shouldReturnAssignmentsMatchingIds() {
        TeacherAssistantAssignment a1 = repository.save(
                TeacherAssistantAssignment.create(teacherId, assistantId1)
        );
        TeacherAssistantAssignment a2 = repository.save(
                TeacherAssistantAssignment.create(teacherId, assistantId2)
        );
        UUID outsiderAssistant = UUID.randomUUID();

        var result = repository.findByTeacherMemberIdAndAssistantMemberIdIn(
                teacherId,
                java.util.List.of(assistantId1, assistantId2, outsiderAssistant)
        );

        assertThat(result)
                .extracting(TeacherAssistantAssignment::getId)
                .containsExactlyInAnyOrder(a1.getId(), a2.getId());
    }

    @Test
    void findByTeacherMemberIdAndAssistantMemberId_shouldReturnAssignment_whenExists() {
        TeacherAssistantAssignment assignment = repository.save(
                TeacherAssistantAssignment.create(teacherId, assistantId1)
        );

        assertThat(repository.findByTeacherMemberIdAndAssistantMemberId(teacherId, assistantId1))
                .contains(assignment);

        assertThat(repository.findByTeacherMemberIdAndAssistantMemberId(teacherId, UUID.randomUUID()))
                .isEmpty();
    }
}
