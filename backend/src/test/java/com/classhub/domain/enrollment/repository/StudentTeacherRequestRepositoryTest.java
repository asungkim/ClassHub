package com.classhub.domain.enrollment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.enrollment.model.StudentTeacherRequest;
import com.classhub.domain.enrollment.model.TeacherStudentRequestStatus;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.model.StudentGrade;
import com.classhub.domain.member.model.StudentInfo;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.member.repository.StudentInfoRepository;
import com.classhub.global.config.JpaConfig;
import java.time.LocalDate;
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

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StudentInfoRepository studentInfoRepository;

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

    @Test
    void searchRequestsForTeacher_shouldFilterByKeywordAndStatus() {
        UUID teacherId = UUID.randomUUID();
        Member student = memberRepository.save(Member.builder()
                .email("student@classhub.com")
                .password("encoded")
                .name("홍길동")
                .phoneNumber("01011112222")
                .role(MemberRole.STUDENT)
                .build());
        studentInfoRepository.save(StudentInfo.builder()
                .memberId(student.getId())
                .schoolName("대치중학교")
                .grade(StudentGrade.MIDDLE_2)
                .birthDate(LocalDate.of(2011, 3, 1))
                .parentPhone("01099998888")
                .build());

        StudentTeacherRequest matching = StudentTeacherRequest.builder()
                .studentMemberId(student.getId())
                .teacherMemberId(teacherId)
                .status(TeacherStudentRequestStatus.PENDING)
                .message("요청합니다")
                .build();
        StudentTeacherRequest nonMatching = StudentTeacherRequest.builder()
                .studentMemberId(student.getId())
                .teacherMemberId(UUID.randomUUID())
                .status(TeacherStudentRequestStatus.PENDING)
                .message("다른 선생님")
                .build();
        repository.saveAll(List.of(matching, nonMatching));

        var page = repository.searchRequestsForTeacher(
                teacherId,
                EnumSet.of(TeacherStudentRequestStatus.PENDING),
                "대치",
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getTeacherMemberId()).isEqualTo(teacherId);
    }

    @Test
    void searchRequestsForTeachers_shouldReturnOnlyAssignedTeachers() {
        UUID teacherId = UUID.randomUUID();
        UUID otherTeacherId = UUID.randomUUID();
        Member student = memberRepository.save(Member.builder()
                .email("student2@classhub.com")
                .password("encoded")
                .name("김학생")
                .phoneNumber("01022223333")
                .role(MemberRole.STUDENT)
                .build());
        studentInfoRepository.save(StudentInfo.builder()
                .memberId(student.getId())
                .schoolName("서초중학교")
                .grade(StudentGrade.MIDDLE_1)
                .birthDate(LocalDate.of(2012, 5, 10))
                .parentPhone("01088887777")
                .build());

        StudentTeacherRequest request = StudentTeacherRequest.builder()
                .studentMemberId(student.getId())
                .teacherMemberId(teacherId)
                .status(TeacherStudentRequestStatus.PENDING)
                .build();
        StudentTeacherRequest otherRequest = StudentTeacherRequest.builder()
                .studentMemberId(student.getId())
                .teacherMemberId(otherTeacherId)
                .status(TeacherStudentRequestStatus.PENDING)
                .build();
        repository.saveAll(List.of(request, otherRequest));

        var page = repository.searchRequestsForTeachers(
                List.of(teacherId),
                EnumSet.of(TeacherStudentRequestStatus.PENDING),
                "김",
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getTeacherMemberId()).isEqualTo(teacherId);
    }
}
