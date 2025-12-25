package com.classhub.domain.enrollment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.enrollment.model.StudentTeacherRequest;
import com.classhub.domain.enrollment.model.TeacherStudentRequestStatus;
import com.classhub.global.config.JpaConfig;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class StudentTeacherRequestRepositoryTest {

    @Autowired
    private StudentTeacherRequestRepository repository;

    @Test
    void existsByStudentMemberIdAndTeacherMemberIdAndStatusIn_shouldReturnTrueForPending() {
        UUID studentId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        StudentTeacherRequest request = StudentTeacherRequest.builder()
                .studentMemberId(studentId)
                .teacherMemberId(teacherId)
                .status(TeacherStudentRequestStatus.PENDING)
                .message("요청합니다")
                .build();
        repository.save(request);

        boolean exists = repository.existsByStudentMemberIdAndTeacherMemberIdAndStatusIn(
                studentId,
                teacherId,
                EnumSet.of(TeacherStudentRequestStatus.PENDING)
        );

        assertThat(exists).isTrue();
    }

    @Test
    void findByStudentMemberIdAndStatusInOrderByCreatedAtDesc_shouldFilterStatuses() {
        UUID studentId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        StudentTeacherRequest pending = StudentTeacherRequest.builder()
                .studentMemberId(studentId)
                .teacherMemberId(teacherId)
                .status(TeacherStudentRequestStatus.PENDING)
                .message("pending")
                .build();
        StudentTeacherRequest rejected = StudentTeacherRequest.builder()
                .studentMemberId(studentId)
                .teacherMemberId(UUID.randomUUID())
                .status(TeacherStudentRequestStatus.REJECTED)
                .message("rejected")
                .build();
        repository.saveAll(List.of(pending, rejected));

        var page = repository.findByStudentMemberIdAndStatusInOrderByCreatedAtDesc(
                studentId,
                EnumSet.of(TeacherStudentRequestStatus.PENDING),
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getStatus()).isEqualTo(TeacherStudentRequestStatus.PENDING);
    }
}
