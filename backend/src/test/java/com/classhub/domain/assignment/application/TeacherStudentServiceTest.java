package com.classhub.domain.assignment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.classhub.domain.assignment.dto.response.TeacherStudentDetailResponse;
import com.classhub.domain.assignment.dto.response.TeacherStudentCourseResponse;
import com.classhub.domain.assignment.model.TeacherAssistantAssignment;
import com.classhub.domain.assignment.model.TeacherStudentAssignment;
import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.assignment.repository.TeacherStudentAssignmentRepository;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.dto.response.StudentSummaryResponse;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.model.StudentGrade;
import com.classhub.domain.member.model.StudentInfo;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.member.repository.StudentInfoRepository;
import com.classhub.domain.studentcourse.model.StudentCourseAssignment;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseAssignmentRepository;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TeacherStudentServiceTest {

    @Mock
    private TeacherStudentAssignmentRepository teacherStudentAssignmentRepository;
    @Mock
    private TeacherAssistantAssignmentRepository teacherAssistantAssignmentRepository;
    @Mock
    private StudentCourseAssignmentRepository studentCourseAssignmentRepository;
    @Mock
    private StudentCourseRecordRepository studentCourseRecordRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private StudentInfoRepository studentInfoRepository;

    @InjectMocks
    private TeacherStudentService teacherStudentService;

    private UUID teacherId;
    private UUID assistantId;
    private UUID studentId;
    private Member student;
    private StudentInfo studentInfo;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        assistantId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        student = Member.builder()
                .email("student@classhub.com")
                .password("encoded")
                .name("학생")
                .phoneNumber("01011112222")
                .role(MemberRole.STUDENT)
                .build();
        ReflectionTestUtils.setField(student, "id", studentId);
        studentInfo = StudentInfo.builder()
                .memberId(studentId)
                .schoolName("중앙중학교")
                .grade(StudentGrade.MIDDLE_2)
                .birthDate(LocalDate.of(2011, 2, 1))
                .parentPhone("01088887777")
                .build();
    }

    @Test
    void getTeacherStudents_shouldReturnStudentSummaries_forTeacher() {
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        TeacherStudentAssignment assignment = TeacherStudentAssignment.create(teacherId, studentId);
        ReflectionTestUtils.setField(assignment, "id", UUID.randomUUID());
        given(teacherStudentAssignmentRepository.searchAssignmentsForTeacherByCourse(
                eq(teacherId),
                eq(null),
                eq("학생"),
                any(PageRequest.class)
        )).willReturn(new PageImpl<>(List.of(assignment), PageRequest.of(0, 10), 1));
        given(memberRepository.findAllById(List.of(studentId))).willReturn(List.of(student));
        given(studentInfoRepository.findByMemberIdIn(List.of(studentId))).willReturn(List.of(studentInfo));

        PageResponse<StudentSummaryResponse> response = teacherStudentService.getTeacherStudents(
                principal,
                null,
                "학생",
                0,
                10
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().memberId()).isEqualTo(studentId);
    }

    @Test
    void getTeacherStudents_shouldReturnEmpty_forAssistantWithoutAssignments() {
        MemberPrincipal principal = new MemberPrincipal(assistantId, MemberRole.ASSISTANT);
        given(teacherAssistantAssignmentRepository.findByAssistantMemberIdAndDeletedAtIsNull(assistantId))
                .willReturn(List.of());

        PageResponse<StudentSummaryResponse> response = teacherStudentService.getTeacherStudents(
                principal,
                null,
                null,
                0,
                10
        );

        assertThat(response.content()).isEmpty();
        verify(teacherStudentAssignmentRepository, never())
                .searchDistinctStudentIdsForTeachers(any(), any(), any(), any());
    }

    @Test
    void getTeacherStudents_shouldReturnDistinctSummaries_forAssistant() {
        MemberPrincipal principal = new MemberPrincipal(assistantId, MemberRole.ASSISTANT);
        TeacherAssistantAssignment assignment = TeacherAssistantAssignment.create(teacherId, assistantId);
        given(teacherAssistantAssignmentRepository.findByAssistantMemberIdAndDeletedAtIsNull(assistantId))
                .willReturn(List.of(assignment));
        given(teacherStudentAssignmentRepository.searchDistinctStudentIdsForTeachers(
                eq(List.of(teacherId)),
                eq(null),
                eq(null),
                any(PageRequest.class)
        )).willReturn(new PageImpl<>(List.of(studentId), PageRequest.of(0, 10), 1));
        given(memberRepository.findAllById(List.of(studentId))).willReturn(List.of(student));
        given(studentInfoRepository.findByMemberIdIn(List.of(studentId))).willReturn(List.of(studentInfo));

        PageResponse<StudentSummaryResponse> response = teacherStudentService.getTeacherStudents(
                principal,
                null,
                null,
                0,
                10
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().memberId()).isEqualTo(studentId);
    }

    @Test
    void getTeacherStudentDetail_shouldReturnCoursesWithAssignments() {
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        UUID courseId = UUID.randomUUID();
        Course course = Course.create(UUID.randomUUID(), teacherId, "수학", null,
                LocalDate.now(), LocalDate.now().plusDays(30), null);
        ReflectionTestUtils.setField(course, "id", courseId);
        ReflectionTestUtils.setField(course, "createdAt", LocalDateTime.now().minusDays(1));
        StudentCourseAssignment assignment = StudentCourseAssignment.create(studentId, courseId, teacherId, null);
        ReflectionTestUtils.setField(assignment, "id", UUID.randomUUID());
        StudentCourseRecord record = StudentCourseRecord.create(studentId, courseId, null, null, null);
        ReflectionTestUtils.setField(record, "id", UUID.randomUUID());

        given(teacherStudentAssignmentRepository.existsByTeacherMemberIdAndStudentMemberIdAndDeletedAtIsNull(
                teacherId,
                studentId
        )).willReturn(true);
        given(memberRepository.findById(studentId)).willReturn(Optional.of(student));
        given(studentInfoRepository.findByMemberId(studentId)).willReturn(Optional.of(studentInfo));
        given(studentCourseAssignmentRepository.findByStudentMemberId(studentId))
                .willReturn(List.of(assignment));
        given(studentCourseRecordRepository.findByStudentMemberIdAndTeacherMemberIdIn(studentId, List.of(teacherId)))
                .willReturn(List.of(record));
        given(courseRepository.findAllById(any()))
                .willReturn(List.of(course));

        TeacherStudentDetailResponse response = teacherStudentService.getTeacherStudentDetail(principal, studentId);

        assertThat(response.student().memberId()).isEqualTo(studentId);
        assertThat(response.courses()).hasSize(1);
        TeacherStudentCourseResponse courseResponse = response.courses().getFirst();
        assertThat(courseResponse.courseId()).isEqualTo(courseId);
        assertThat(courseResponse.assignmentId()).isEqualTo(assignment.getId());
        assertThat(courseResponse.recordId()).isEqualTo(record.getId());
        assertThat(courseResponse.assignmentActive()).isTrue();
    }

    @Test
    void getTeacherStudentDetail_shouldThrow_whenStudentNotAssigned() {
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        given(teacherStudentAssignmentRepository.existsByTeacherMemberIdAndStudentMemberIdAndDeletedAtIsNull(
                teacherId,
                studentId
        )).willReturn(false);

        assertThatThrownBy(() -> teacherStudentService.getTeacherStudentDetail(principal, studentId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }
}
