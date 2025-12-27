package com.classhub.domain.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.classhub.domain.assignment.model.TeacherBranchAssignment;
import com.classhub.domain.assignment.model.TeacherStudentAssignment;
import com.classhub.domain.assignment.repository.TeacherBranchAssignmentRepository;
import com.classhub.domain.assignment.repository.TeacherStudentAssignmentRepository;
import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.branch.repository.BranchRepository;
import com.classhub.domain.company.company.model.Company;
import com.classhub.domain.company.company.model.CompanyType;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.domain.company.company.repository.CompanyRepository;
import com.classhub.domain.assignment.model.TeacherStudentRequestStatus;
import com.classhub.domain.assignment.repository.StudentTeacherRequestRepository;
import com.classhub.domain.member.dto.response.TeacherSearchResponse;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.response.PageResponse;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;

import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TeacherSearchServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private TeacherBranchAssignmentRepository teacherBranchAssignmentRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private StudentTeacherRequestRepository studentTeacherRequestRepository;
    @Mock
    private TeacherStudentAssignmentRepository teacherStudentAssignmentRepository;

    @InjectMocks
    private TeacherSearchService teacherSearchService;

    private UUID studentId;
    private UUID teacherId;
    private UUID branchId;
    private UUID companyId;
    private Member teacher;
    private Branch branch;
    private Company company;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        branchId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        teacher = Member.builder()
                .email("teacher@classhub.com")
                .password("encoded")
                .name("Teacher Kim")
                .phoneNumber("01012345678")
                .role(MemberRole.TEACHER)
                .build();
        ReflectionTestUtils.setField(teacher, "id", teacherId);
        branch = Branch.create(companyId, "강남", teacherId, VerifiedStatus.VERIFIED);
        ReflectionTestUtils.setField(branch, "id", branchId);
        company = Company.create("러셀", "desc", CompanyType.ACADEMY, VerifiedStatus.VERIFIED, teacherId);
        ReflectionTestUtils.setField(company, "id", companyId);
    }

    @Test
    void searchTeachers_shouldReturnTeachersWithVerifiedBranches() {
        Page<Member> page = new PageImpl<>(List.of(teacher), PageRequest.of(0, 10), 1);
        TeacherBranchAssignment assignment = TeacherBranchAssignment.create(teacherId, branchId, com.classhub.domain.assignment.model.BranchRole.OWNER);
        when(memberRepository.findByRoleAndDeletedAtIsNullAndNameContainingIgnoreCase(
                eq(MemberRole.TEACHER),
                eq("kim"),
                any()
        )).thenReturn(page);
        when(studentTeacherRequestRepository.findTeacherMemberIdsByStudentMemberIdAndStatusIn(
                studentId,
                EnumSet.of(TeacherStudentRequestStatus.PENDING, TeacherStudentRequestStatus.APPROVED, TeacherStudentRequestStatus.REJECTED)
        )).thenReturn(List.of());
        when(teacherStudentAssignmentRepository.findByStudentMemberIdAndDeletedAtIsNull(studentId))
                .thenReturn(List.of());
        when(teacherBranchAssignmentRepository.findByTeacherMemberIdInAndDeletedAtIsNull(anyList()))
                .thenReturn(List.of(assignment));
        when(branchRepository.findAllById(any())).thenReturn(List.of(branch));
        when(companyRepository.findAllById(any())).thenReturn(List.of(company));

        PageResponse<TeacherSearchResponse> response = teacherSearchService.searchTeachers(
                studentId,
                "kim",
                null,
                null,
                0,
                10
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().teacherId()).isEqualTo(teacherId);
        assertThat(response.content().getFirst().branches()).hasSize(1);
    }

    @Test
    void searchTeachers_shouldExcludeTeachersWithPendingRequests() {
        Page<Member> page = new PageImpl<>(List.of(teacher), PageRequest.of(0, 10), 1);
        when(memberRepository.findByRoleAndDeletedAtIsNullAndNameContainingIgnoreCase(
                eq(MemberRole.TEACHER),
                eq("kim"),
                any()
        )).thenReturn(page);
        when(studentTeacherRequestRepository.findTeacherMemberIdsByStudentMemberIdAndStatusIn(
                studentId,
                EnumSet.of(TeacherStudentRequestStatus.PENDING, TeacherStudentRequestStatus.APPROVED, TeacherStudentRequestStatus.REJECTED)
        )).thenReturn(List.of(teacherId));
        when(teacherStudentAssignmentRepository.findByStudentMemberIdAndDeletedAtIsNull(studentId))
                .thenReturn(List.of());

        PageResponse<TeacherSearchResponse> response = teacherSearchService.searchTeachers(
                studentId,
                "kim",
                null,
                null,
                0,
                10
        );

        assertThat(response.content()).isEmpty();
    }
}
