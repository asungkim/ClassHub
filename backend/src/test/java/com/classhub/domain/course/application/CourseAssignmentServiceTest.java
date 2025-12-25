package com.classhub.domain.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.classhub.domain.assignment.model.TeacherStudentAssignment;
import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.assignment.repository.TeacherStudentAssignmentRepository;
import com.classhub.domain.course.dto.response.CourseResponse;
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
import com.classhub.domain.studentcourse.dto.request.StudentCourseAssignmentCreateRequest;
import com.classhub.domain.studentcourse.dto.response.StudentCourseAssignmentResponse;
import com.classhub.domain.studentcourse.model.StudentCourseAssignment;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseAssignmentRepository;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
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
class CourseAssignmentServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private TeacherAssistantAssignmentRepository assistantAssignmentRepository;
    @Mock
    private TeacherStudentAssignmentRepository teacherStudentAssignmentRepository;
    @Mock
    private StudentCourseAssignmentRepository studentCourseAssignmentRepository;
    @Mock
    private StudentCourseRecordRepository studentCourseRecordRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private StudentInfoRepository studentInfoRepository;
    @Mock
    private CourseViewAssembler courseViewAssembler;

    @InjectMocks
    private CourseAssignmentService courseAssignmentService;

    private UUID teacherId;
    private UUID assistantId;
    private UUID studentId;
    private Course course;
    private Member student;
    private StudentInfo studentInfo;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        assistantId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        course = Course.create(UUID.randomUUID(), teacherId, "수학", null, LocalDate.now(), LocalDate.now().plusDays(7), null);
        ReflectionTestUtils.setField(course, "id", UUID.randomUUID());
        student = Member.builder()
                .email("student@classhub.com")
                .password("encoded")
                .name("학생")
                .phoneNumber("01099990000")
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
    void getAssignableCourses_shouldReturnEmptyForAssistantWithoutAssignments() {
        MemberPrincipal principal = new MemberPrincipal(assistantId, MemberRole.ASSISTANT);
        given(assistantAssignmentRepository.findByAssistantMemberIdAndDeletedAtIsNull(assistantId))
                .willReturn(List.of());

        PageResponse<CourseResponse> response = courseAssignmentService.getAssignableCourses(
                principal,
                null,
                null,
                0,
                10
        );

        assertThat(response.content()).isEmpty();
        verify(courseRepository, never()).searchAssignableCoursesForTeachers(any(), any(), any(), any(), any());
    }

    @Test
    void getAssignmentCandidates_shouldReturnStudentSummaries() {
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        TeacherStudentAssignment assignment = TeacherStudentAssignment.create(teacherId, studentId);
        ReflectionTestUtils.setField(assignment, "id", UUID.randomUUID());
        given(courseRepository.findById(course.getId())).willReturn(Optional.of(course));
        given(teacherStudentAssignmentRepository.searchAssignmentsForTeacher(
                eq(teacherId),
                eq("학생"),
                any(),
                any()
        )).willReturn(new PageImpl<>(List.of(assignment), PageRequest.of(0, 10), 1));
        given(memberRepository.findAllById(List.of(studentId))).willReturn(List.of(student));
        given(studentInfoRepository.findByMemberIdIn(List.of(studentId))).willReturn(List.of(studentInfo));

        PageResponse<StudentSummaryResponse> response = courseAssignmentService.getAssignmentCandidates(
                principal,
                course.getId(),
                "학생",
                0,
                10
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().memberId()).isEqualTo(studentId);
    }

    @Test
    void createAssignment_shouldCreateAssignmentAndRecord() {
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        StudentCourseAssignmentCreateRequest request = new StudentCourseAssignmentCreateRequest(studentId, course.getId());
        given(courseRepository.findById(course.getId())).willReturn(Optional.of(course));
        given(teacherStudentAssignmentRepository.existsByTeacherMemberIdAndStudentMemberIdAndDeletedAtIsNull(
                teacherId,
                studentId
        )).willReturn(true);
        given(studentCourseAssignmentRepository.existsByStudentMemberIdAndCourseId(studentId, course.getId()))
                .willReturn(false);
        given(studentCourseAssignmentRepository.save(any(StudentCourseAssignment.class))).willAnswer(invocation -> {
            StudentCourseAssignment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            return saved;
        });
        given(studentCourseRecordRepository.findByStudentMemberIdAndCourseIdAndDeletedAtIsNull(studentId, course.getId()))
                .willReturn(Optional.empty());

        StudentCourseAssignmentResponse response = courseAssignmentService.createAssignment(principal, request);

        assertThat(response.assignmentId()).isNotNull();
        assertThat(response.active()).isTrue();
        verify(studentCourseRecordRepository).save(any(StudentCourseRecord.class));
    }

    @Test
    void createAssignment_shouldThrow_whenNotLinkedToTeacher() {
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        StudentCourseAssignmentCreateRequest request = new StudentCourseAssignmentCreateRequest(studentId, course.getId());
        given(courseRepository.findById(course.getId())).willReturn(Optional.of(course));
        given(teacherStudentAssignmentRepository.existsByTeacherMemberIdAndStudentMemberIdAndDeletedAtIsNull(
                teacherId,
                studentId
        )).willReturn(false);

        assertThatThrownBy(() -> courseAssignmentService.createAssignment(principal, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }
}
