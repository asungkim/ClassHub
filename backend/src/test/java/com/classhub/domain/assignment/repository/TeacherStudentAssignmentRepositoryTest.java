package com.classhub.domain.assignment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.assignment.model.TeacherStudentAssignment;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.model.StudentGrade;
import com.classhub.domain.member.model.StudentInfo;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.member.repository.StudentInfoRepository;
import com.classhub.global.config.JpaConfig;
import java.time.LocalDate;
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
class TeacherStudentAssignmentRepositoryTest {

    @Autowired
    private TeacherStudentAssignmentRepository repository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StudentInfoRepository studentInfoRepository;

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

    @Test
    void searchAssignmentsForTeacher_shouldFilterByKeywordAndExclude() {
        UUID teacherId = UUID.randomUUID();
        Member student = memberRepository.save(Member.builder()
                .email("student3@classhub.com")
                .password("encoded")
                .name("박학생")
                .phoneNumber("01033334444")
                .role(MemberRole.STUDENT)
                .build());
        studentInfoRepository.save(StudentInfo.builder()
                .memberId(student.getId())
                .schoolName("대치중학교")
                .grade(StudentGrade.MIDDLE_3)
                .birthDate(LocalDate.of(2010, 4, 2))
                .parentPhone("01066665555")
                .build());
        TeacherStudentAssignment assignment = repository.save(
                TeacherStudentAssignment.create(teacherId, student.getId())
        );

        var page = repository.searchAssignmentsForTeacher(
                teacherId,
                "대치",
                List.of(UUID.randomUUID()),
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getId()).isEqualTo(assignment.getId());

        var excluded = repository.searchAssignmentsForTeacher(
                teacherId,
                "대치",
                List.of(student.getId()),
                PageRequest.of(0, 10)
        );

        assertThat(excluded.getContent()).isEmpty();
    }
}
