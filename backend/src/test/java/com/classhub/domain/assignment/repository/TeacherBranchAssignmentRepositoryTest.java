package com.classhub.domain.assignment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.assignment.model.BranchRole;
import com.classhub.domain.assignment.model.TeacherBranchAssignment;
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
class TeacherBranchAssignmentRepositoryTest {

    @Autowired
    private TeacherBranchAssignmentRepository repository;

    private UUID teacherId;
    private UUID branchId1;
    private UUID branchId2;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        branchId1 = UUID.randomUUID();
        branchId2 = UUID.randomUUID();
    }

    @Test
    void findByTeacherMemberIdAndDeletedAtIsNull_shouldReturnOnlyActiveAssignments() {
        TeacherBranchAssignment active = repository.save(
                TeacherBranchAssignment.create(teacherId, branchId1, BranchRole.FREELANCE)
        );
        TeacherBranchAssignment disabled = repository.save(
                TeacherBranchAssignment.create(teacherId, branchId2, BranchRole.FREELANCE)
        );
        disabled.disable();
        repository.save(disabled);

        Page<TeacherBranchAssignment> result = repository.findByTeacherMemberIdAndDeletedAtIsNull(
                teacherId,
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent())
                .hasSize(1)
                .first()
                .extracting(TeacherBranchAssignment::getId)
                .isEqualTo(active.getId());
    }

    @Test
    void findByTeacherMemberIdAndDeletedAtIsNotNull_shouldReturnOnlyDisabledAssignments() {
        TeacherBranchAssignment disabled = repository.save(
                TeacherBranchAssignment.create(teacherId, branchId1, BranchRole.OWNER)
        );
        disabled.disable();
        repository.save(disabled);
        repository.save(TeacherBranchAssignment.create(teacherId, branchId2, BranchRole.OWNER));

        Page<TeacherBranchAssignment> result = repository.findByTeacherMemberIdAndDeletedAtIsNotNull(
                teacherId,
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent())
                .hasSize(1)
                .first()
                .extracting(TeacherBranchAssignment::getId)
                .isEqualTo(disabled.getId());
    }

    @Test
    void findByTeacherMemberId_shouldReturnAllAssignmentsRegardlessOfStatus() {
        TeacherBranchAssignment a1 = repository.save(
                TeacherBranchAssignment.create(teacherId, branchId1, BranchRole.OWNER)
        );
        TeacherBranchAssignment a2 = repository.save(
                TeacherBranchAssignment.create(teacherId, branchId2, BranchRole.FREELANCE)
        );
        a2.disable();
        repository.save(a2);

        Page<TeacherBranchAssignment> result = repository.findByTeacherMemberId(
                teacherId,
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent())
                .extracting(TeacherBranchAssignment::getId)
                .containsExactlyInAnyOrder(a1.getId(), a2.getId());
    }

    @Test
    void findByTeacherMemberIdAndBranchId_shouldReturnAssignmentForTeacher() {
        TeacherBranchAssignment assignment = repository.save(
                TeacherBranchAssignment.create(teacherId, branchId1, BranchRole.OWNER)
        );

        assertThat(repository.findByTeacherMemberIdAndBranchId(teacherId, branchId1))
                .contains(assignment);
        assertThat(repository.findByTeacherMemberIdAndBranchId(UUID.randomUUID(), branchId1))
                .isEmpty();
    }

    @Test
    void findByIdAndTeacherMemberId_shouldRespectOwnership() {
        TeacherBranchAssignment assignment = repository.save(
                TeacherBranchAssignment.create(teacherId, branchId1, BranchRole.FREELANCE)
        );

        assertThat(repository.findByIdAndTeacherMemberId(assignment.getId(), teacherId))
                .contains(assignment);
        assertThat(repository.findByIdAndTeacherMemberId(assignment.getId(), UUID.randomUUID()))
                .isEmpty();
    }
}
