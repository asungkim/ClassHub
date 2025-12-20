package com.classhub.domain.enrollment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.classhub.domain.assignment.model.TeacherAssistantAssignment;
import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.course.application.CourseViewAssembler;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.enrollment.dto.response.TeacherEnrollmentRequestResponse;
import com.classhub.domain.enrollment.model.EnrollmentStatus;
import com.classhub.domain.enrollment.model.StudentEnrollmentRequest;
import com.classhub.domain.enrollment.repository.StudentEnrollmentRequestRepository;
import com.classhub.domain.member.dto.response.StudentSummaryResponse;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.model.StudentGrade;
import com.classhub.domain.member.model.StudentInfo;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.member.repository.StudentInfoRepository;
import com.classhub.domain.studentcourse.model.StudentCourseEnrollment;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseEnrollmentRepository;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StudentEnrollmentApprovalServiceTest {

    @Mock
    private StudentEnrollmentRequestRepository requestRepository;

    @Mock
    private StudentCourseEnrollmentRepository enrollmentRepository;

    @Mock
    private StudentCourseRecordRepository recordRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseViewAssembler courseViewAssembler;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private StudentInfoRepository studentInfoRepository;

    @Mock
    private TeacherAssistantAssignmentRepository assistantAssignmentRepository;

    @InjectMocks
    private StudentEnrollmentApprovalService approvalService;

    private UUID teacherId;
    private UUID assistantId;
    private UUID studentId;
    private UUID courseId;
    private StudentEnrollmentRequest request;
    private Course course;
    private CourseResponse courseResponse;
    private Member studentMember;
    private StudentInfo studentInfo;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        assistantId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        request = StudentEnrollmentRequest.builder()
                .studentMemberId(studentId)
                .courseId(courseId)
                .status(EnrollmentStatus.PENDING)
                .message("지원합니다")
                .build();
        ReflectionTestUtils.setField(request, "id", UUID.randomUUID());
        course = Course.create(
                UUID.randomUUID(),
                teacherId,
                "중3 수학",
                "desc",
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                Set.of()
        );
        ReflectionTestUtils.setField(course, "id", courseId);
        courseResponse = new CourseResponse(
                courseId,
                UUID.randomUUID(),
                "강남",
                UUID.randomUUID(),
                "러셀",
                "중3 수학",
                "desc",
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                true,
                List.of()
        );
        studentMember = Member.builder()
                .role(MemberRole.STUDENT)
                .email("student@classhub.dev")
                .name("홍길동")
                .phoneNumber("010-0000-0000")
                .password("encoded")
                .build();
        ReflectionTestUtils.setField(studentMember, "id", studentId);
        studentInfo = StudentInfo.builder()
                .memberId(studentId)
                .schoolName("ClassHub 중학교")
                .grade(StudentGrade.MIDDLE_3)
                .birthDate(LocalDate.now().minusYears(15))
                .parentPhone("010-1111-2222")
                .build();
    }

    @Test
    void shouldListRequestsForTeacher_withStudentData() {
        Page<StudentEnrollmentRequest> page = new PageImpl<>(List.of(request), PageRequest.of(0, 10), 1);
        when(requestRepository.searchRequestsForTeacher(
                eq(teacherId),
                eq(courseId),
                anySet(),
                eq("gil"),
                any(PageRequest.class)
        )).thenReturn(page);
        when(courseRepository.findAllById(anyCollection())).thenReturn(List.of(course));
        CourseViewAssembler.CourseContext context = new CourseViewAssembler.CourseContext(
                java.util.Map.of(),
                java.util.Map.of(),
                java.util.Map.of()
        );
        when(courseViewAssembler.buildContext(anyCollection())).thenReturn(context);
        when(courseViewAssembler.toCourseResponse(course, context)).thenReturn(courseResponse);
        when(memberRepository.findAllById(anyCollection())).thenReturn(List.of(studentMember));
        when(studentInfoRepository.findByMemberIdIn(anyCollection())).thenReturn(List.of(studentInfo));

        PageResponse<TeacherEnrollmentRequestResponse> response = approvalService.getRequestsForTeacher(
                teacherId,
                courseId,
                Set.of(EnrollmentStatus.PENDING),
                "gil",
                0,
                10
        );

        assertThat(response.content()).hasSize(1);
        TeacherEnrollmentRequestResponse dto = response.content().getFirst();
        assertThat(dto.requestId()).isEqualTo(request.getId());
        StudentSummaryResponse summary = dto.student();
        assertThat(summary.name()).isEqualTo("홍길동");
        assertThat(summary.schoolName()).isEqualTo("ClassHub 중학교");
        verify(requestRepository).searchRequestsForTeacher(
                eq(teacherId),
                eq(courseId),
                anySet(),
                eq("gil"),
                eq(PageRequest.of(0, 10))
        );
    }

    @Test
    void shouldListRequestsForAssistant_whenAssignedTeachersExist() {
        Page<StudentEnrollmentRequest> page = new PageImpl<>(List.of(request), PageRequest.of(0, 5), 1);
        when(assistantAssignmentRepository.findByAssistantMemberIdAndDeletedAtIsNull(assistantId))
                .thenReturn(List.of(TeacherAssistantAssignment.create(teacherId, assistantId)));
        when(requestRepository.searchRequestsForTeachers(eq(List.of(teacherId)), any(), anySet(), any(), any(PageRequest.class)))
                .thenReturn(page);
        when(courseRepository.findAllById(anyCollection())).thenReturn(List.of(course));
        CourseViewAssembler.CourseContext context = new CourseViewAssembler.CourseContext(
                java.util.Map.of(),
                java.util.Map.of(),
                java.util.Map.of()
        );
        when(courseViewAssembler.buildContext(anyCollection())).thenReturn(context);
        when(courseViewAssembler.toCourseResponse(course, context)).thenReturn(courseResponse);
        when(memberRepository.findAllById(anyCollection())).thenReturn(List.of(studentMember));
        when(studentInfoRepository.findByMemberIdIn(anyCollection())).thenReturn(List.of(studentInfo));

        PageResponse<TeacherEnrollmentRequestResponse> response = approvalService.getRequestsForAssistant(
                assistantId,
                null,
                Set.of(EnrollmentStatus.PENDING),
                null,
                0,
                5
        );

        assertThat(response.content()).hasSize(1);
        verify(requestRepository).searchRequestsForTeachers(
                eq(List.of(teacherId)),
                eq(null),
                anySet(),
                eq(null),
                eq(PageRequest.of(0, 5))
        );
    }

    @Test
    void getRequestDetail_shouldReturnWhenActorHasPermission() {
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(memberRepository.findById(studentId)).thenReturn(Optional.of(studentMember));
        when(studentInfoRepository.findByMemberId(studentId)).thenReturn(Optional.of(studentInfo));
        CourseViewAssembler.CourseContext context = new CourseViewAssembler.CourseContext(
                java.util.Map.of(),
                java.util.Map.of(),
                java.util.Map.of()
        );
        when(courseViewAssembler.buildContext(anyCollection())).thenReturn(context);
        when(courseViewAssembler.toCourseResponse(course, context)).thenReturn(courseResponse);

        TeacherEnrollmentRequestResponse response = approvalService.getRequestDetail(teacherId, request.getId());

        assertThat(response.requestId()).isEqualTo(request.getId());
        assertThat(response.student().name()).isEqualTo("홍길동");
    }

    @Test
    void shouldThrow_whenAssistantHasNoAssignments() {
        when(assistantAssignmentRepository.findByAssistantMemberIdAndDeletedAtIsNull(assistantId))
                .thenReturn(List.of());

        PageResponse<TeacherEnrollmentRequestResponse> response = approvalService.getRequestsForAssistant(
                assistantId,
                null,
                Set.of(EnrollmentStatus.PENDING),
                null,
                0,
                5
        );

        assertThat(response.content()).isEmpty();
        verify(requestRepository, never()).searchRequestsForTeachers(any(), any(), anySet(), any(), any());
    }

    @Test
    void shouldApproveRequest_andCreateEnrollmentAndRecord() {
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByStudentMemberIdAndCourseIdAndDeletedAtIsNull(studentId, courseId))
                .thenReturn(false);
        when(memberRepository.findById(studentId)).thenReturn(Optional.of(studentMember));
        when(studentInfoRepository.findByMemberId(studentId)).thenReturn(Optional.of(studentInfo));
        CourseViewAssembler.CourseContext context = new CourseViewAssembler.CourseContext(
                java.util.Map.of(),
                java.util.Map.of(),
                java.util.Map.of()
        );
        when(courseViewAssembler.buildContext(anyCollection())).thenReturn(context);
        when(courseViewAssembler.toCourseResponse(course, context)).thenReturn(courseResponse);

        TeacherEnrollmentRequestResponse response = approvalService.approveRequest(teacherId, request.getId());

        assertThat(response.status()).isEqualTo(EnrollmentStatus.APPROVED);
        ArgumentCaptor<StudentCourseEnrollment> enrollmentCaptor = ArgumentCaptor.forClass(StudentCourseEnrollment.class);
        verify(enrollmentRepository).save(enrollmentCaptor.capture());
        assertThat(enrollmentCaptor.getValue().getStudentMemberId()).isEqualTo(studentId);
        ArgumentCaptor<StudentCourseRecord> recordCaptor = ArgumentCaptor.forClass(StudentCourseRecord.class);
        verify(recordRepository).save(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getStudentMemberId()).isEqualTo(studentId);
    }

    @Test
    void shouldRejectRequest_andSetProcessedFields() {
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(memberRepository.findById(studentId)).thenReturn(Optional.of(studentMember));
        when(studentInfoRepository.findByMemberId(studentId)).thenReturn(Optional.of(studentInfo));
        CourseViewAssembler.CourseContext context = new CourseViewAssembler.CourseContext(
                java.util.Map.of(),
                java.util.Map.of(),
                java.util.Map.of()
        );
        when(courseViewAssembler.buildContext(anyCollection())).thenReturn(context);
        when(courseViewAssembler.toCourseResponse(course, context)).thenReturn(courseResponse);

        TeacherEnrollmentRequestResponse response = approvalService.rejectRequest(teacherId, request.getId());

        assertThat(response.status()).isEqualTo(EnrollmentStatus.REJECTED);
        verify(enrollmentRepository, never()).save(any(StudentCourseEnrollment.class));
        verify(recordRepository, never()).save(any(StudentCourseRecord.class));
    }

    @Test
    void shouldThrow_whenApprovingNonPendingRequest() {
        request.approve(teacherId, LocalDateTime.now());
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> approvalService.approveRequest(teacherId, request.getId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.INVALID_ENROLLMENT_REQUEST_STATE);
    }

    @Test
    void shouldThrow_whenAssistantWithoutPermissionApproves() {
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(assistantAssignmentRepository.findByTeacherMemberIdAndAssistantMemberIdAndDeletedAtIsNull(
                course.getTeacherMemberId(),
                assistantId
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> approvalService.approveRequest(assistantId, request.getId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }
}
