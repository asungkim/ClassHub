package com.classhub.domain.enrollment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.classhub.domain.assignment.repository.TeacherBranchAssignmentRepository;
import com.classhub.domain.assignment.repository.TeacherStudentAssignmentRepository;
import com.classhub.domain.company.branch.repository.BranchRepository;
import com.classhub.domain.company.company.repository.CompanyRepository;
import com.classhub.domain.enrollment.dto.request.StudentTeacherRequestCreateRequest;
import com.classhub.domain.enrollment.dto.response.StudentTeacherRequestResponse;
import com.classhub.domain.enrollment.model.StudentTeacherRequest;
import com.classhub.domain.enrollment.model.TeacherStudentRequestStatus;
import com.classhub.domain.enrollment.repository.StudentTeacherRequestRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StudentTeacherRequestServiceTest {

    @Mock
    private StudentTeacherRequestRepository requestRepository;
    @Mock
    private TeacherStudentAssignmentRepository teacherStudentAssignmentRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private TeacherBranchAssignmentRepository teacherBranchAssignmentRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private StudentTeacherRequestService studentTeacherRequestService;

    private UUID studentId;
    private UUID teacherId;
    private Member teacher;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        teacher = Member.builder()
                .email("teacher@classhub.com")
                .password("encoded")
                .name("Teacher Kim")
                .phoneNumber("01012345678")
                .role(MemberRole.TEACHER)
                .build();
        ReflectionTestUtils.setField(teacher, "id", teacherId);
    }

    @Test
    void createRequest_shouldCreate_whenTeacherIsValid() {
        StudentTeacherRequestCreateRequest request = new StudentTeacherRequestCreateRequest(teacherId, "요청합니다");
        given(memberRepository.findById(teacherId)).willReturn(Optional.of(teacher));
        given(requestRepository.existsByStudentMemberIdAndTeacherMemberIdAndStatusIn(
                any(),
                any(),
                any()
        )).willReturn(false);
        given(teacherStudentAssignmentRepository.existsByTeacherMemberIdAndStudentMemberIdAndDeletedAtIsNull(
                any(),
                any()
        )).willReturn(false);
        given(requestRepository.save(any(StudentTeacherRequest.class))).willAnswer(invocation -> {
            StudentTeacherRequest entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
            return entity;
        });
        given(teacherBranchAssignmentRepository.findByTeacherMemberIdInAndDeletedAtIsNull(any()))
                .willReturn(List.of());

        StudentTeacherRequestResponse response = studentTeacherRequestService.createRequest(studentId, request);

        assertThat(response.requestId()).isNotNull();
        assertThat(response.status()).isEqualTo(TeacherStudentRequestStatus.PENDING);
    }

    @Test
    void createRequest_shouldThrow_whenAlreadyAssigned() {
        StudentTeacherRequestCreateRequest request = new StudentTeacherRequestCreateRequest(teacherId, null);
        given(memberRepository.findById(teacherId)).willReturn(Optional.of(teacher));
        given(requestRepository.existsByStudentMemberIdAndTeacherMemberIdAndStatusIn(
                any(),
                any(),
                any()
        )).willReturn(false);
        given(teacherStudentAssignmentRepository.existsByTeacherMemberIdAndStudentMemberIdAndDeletedAtIsNull(
                any(),
                any()
        )).willReturn(true);

        assertThatThrownBy(() -> studentTeacherRequestService.createRequest(studentId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.TEACHER_STUDENT_ALREADY_ASSIGNED);
    }

    @Test
    void cancelRequest_shouldThrow_whenRequestIsNotPending() {
        UUID requestId = UUID.randomUUID();
        StudentTeacherRequest request = StudentTeacherRequest.builder()
                .studentMemberId(studentId)
                .teacherMemberId(teacherId)
                .status(TeacherStudentRequestStatus.APPROVED)
                .build();
        ReflectionTestUtils.setField(request, "id", requestId);
        given(requestRepository.findById(requestId)).willReturn(Optional.of(request));

        assertThatThrownBy(() -> studentTeacherRequestService.cancelRequest(studentId, requestId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.INVALID_TEACHER_STUDENT_REQUEST_STATE);
    }
}
