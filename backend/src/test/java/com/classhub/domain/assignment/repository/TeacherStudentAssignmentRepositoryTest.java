package com.classhub.domain.assignment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.assignment.model.TeacherStudentAssignment;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.model.StudentGrade;
import com.classhub.domain.member.model.StudentInfo;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.member.repository.StudentInfoRepository;
import com.classhub.domain.studentcourse.model.StudentCourseAssignment;
import com.classhub.domain.studentcourse.repository.StudentCourseAssignmentRepository;
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

    @Autowired
    private StudentCourseAssignmentRepository studentCourseAssignmentRepository;

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

    @Test
    void searchAssignmentsForTeachers_shouldReturnAssignmentsForMultipleTeachers() {
        UUID teacherA = UUID.randomUUID();
        UUID teacherB = UUID.randomUUID();
        Member student = memberRepository.save(Member.builder()
                .email("student4@classhub.com")
                .password("encoded")
                .name("최학생")
                .phoneNumber("01055556666")
                .role(MemberRole.STUDENT)
                .build());
        studentInfoRepository.save(StudentInfo.builder()
                .memberId(student.getId())
                .schoolName("서초중학교")
                .grade(StudentGrade.MIDDLE_1)
                .birthDate(LocalDate.of(2012, 6, 15))
                .parentPhone("01012121212")
                .build());
        TeacherStudentAssignment assignmentA = repository.save(
                TeacherStudentAssignment.create(teacherA, student.getId())
        );
        repository.save(TeacherStudentAssignment.create(teacherB, UUID.randomUUID()));

        var page = repository.searchAssignmentsForTeachers(
                List.of(teacherA),
                "서초",
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getId()).isEqualTo(assignmentA.getId());
    }

    @Test
    void searchAssignmentsForTeacherByCourse_shouldFilterAssignments() {
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID otherCourseId = UUID.randomUUID();
        Member student = memberRepository.save(Member.builder()
                .email("student5@classhub.com")
                .password("encoded")
                .name("한학생")
                .phoneNumber("01010101010")
                .role(MemberRole.STUDENT)
                .build());
        studentInfoRepository.save(StudentInfo.builder()
                .memberId(student.getId())
                .schoolName("잠실중학교")
                .grade(StudentGrade.MIDDLE_2)
                .birthDate(LocalDate.of(2011, 8, 11))
                .parentPhone("01099990000")
                .build());
        TeacherStudentAssignment assignment = repository.save(
                TeacherStudentAssignment.create(teacherId, student.getId())
        );
        studentCourseAssignmentRepository.save(
                StudentCourseAssignment.create(student.getId(), courseId, teacherId, null)
        );

        var match = repository.searchAssignmentsForTeacherByCourse(
                teacherId,
                courseId,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(match.getContent()).hasSize(1);
        assertThat(match.getContent().getFirst().getId()).isEqualTo(assignment.getId());

        var empty = repository.searchAssignmentsForTeacherByCourse(
                teacherId,
                otherCourseId,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(empty.getContent()).isEmpty();
    }

    @Test
    void searchAssignmentsForTeachersByCourse_shouldFilterAssignments() {
        UUID teacherId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        Member student = memberRepository.save(Member.builder()
                .email("student6@classhub.com")
                .password("encoded")
                .name("윤학생")
                .phoneNumber("01030303030")
                .role(MemberRole.STUDENT)
                .build());
        studentInfoRepository.save(StudentInfo.builder()
                .memberId(student.getId())
                .schoolName("송파중학교")
                .grade(StudentGrade.MIDDLE_1)
                .birthDate(LocalDate.of(2012, 3, 3))
                .parentPhone("01012121212")
                .build());
        TeacherStudentAssignment assignment = repository.save(
                TeacherStudentAssignment.create(teacherId, student.getId())
        );
        studentCourseAssignmentRepository.save(
                StudentCourseAssignment.create(student.getId(), courseId, teacherId, null)
        );

        var page = repository.searchAssignmentsForTeachersByCourse(
                List.of(teacherId),
                courseId,
                "윤",
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getId()).isEqualTo(assignment.getId());
    }
}
