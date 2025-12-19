package com.classhub.domain.assignment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.classhub.domain.assignment.dto.TeacherBranchAssignmentStatusFilter;
import com.classhub.domain.assignment.dto.request.TeacherBranchAssignmentCreateRequest;
import com.classhub.domain.assignment.dto.request.TeacherBranchAssignmentCreateRequest.AssignmentCreationMode;
import com.classhub.domain.assignment.dto.request.TeacherBranchAssignmentCreateRequest.BranchInput;
import com.classhub.domain.assignment.dto.request.TeacherBranchAssignmentCreateRequest.CompanyInput;
import com.classhub.domain.assignment.dto.request.TeacherBranchAssignmentCreateRequest.IndividualInput;
import com.classhub.domain.assignment.dto.request.TeacherBranchAssignmentStatusUpdateRequest;
import com.classhub.domain.assignment.dto.response.TeacherBranchAssignmentResponse;
import com.classhub.domain.assignment.model.BranchRole;
import com.classhub.domain.assignment.model.TeacherBranchAssignment;
import com.classhub.domain.assignment.repository.TeacherBranchAssignmentRepository;
import com.classhub.domain.company.branch.application.BranchCommandService;
import com.classhub.domain.company.branch.dto.request.BranchCreateRequest;
import com.classhub.domain.company.branch.dto.response.BranchResponse;
import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.branch.repository.BranchRepository;
import com.classhub.domain.company.company.model.Company;
import com.classhub.domain.company.company.model.CompanyType;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.domain.company.company.application.CompanyCommandService;
import com.classhub.domain.company.company.dto.request.CompanyCreateRequest;
import com.classhub.domain.company.company.dto.response.CompanyResponse;
import com.classhub.domain.company.company.repository.CompanyRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TeacherBranchAssignmentServiceTest {

    @Mock
    private TeacherBranchAssignmentRepository assignmentRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private BranchCommandService branchCommandService;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyCommandService companyCommandService;

    @InjectMocks
    private TeacherBranchAssignmentService teacherBranchAssignmentService;

    private UUID teacherId;
    private Branch branch;
    private Company company;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        branch = Branch.create(companyId, "강남", teacherId, VerifiedStatus.VERIFIED);
        ReflectionTestUtils.setField(branch, "id", UUID.randomUUID());
        company = Company.create("러셀", "desc", CompanyType.ACADEMY, VerifiedStatus.VERIFIED, UUID.randomUUID());
        ReflectionTestUtils.setField(company, "id", companyId);
    }

    @Test
    void createAssignment_shouldCreateNewAssignment_whenBranchAccessible() {
        TeacherBranchAssignmentCreateRequest request = new TeacherBranchAssignmentCreateRequest(
                AssignmentCreationMode.EXISTING_BRANCH,
                branch.getId(),
                null,
                null,
                null,
                null
        );
        when(branchRepository.findById(branch.getId())).thenReturn(Optional.of(branch));
        when(companyRepository.findById(company.getId())).thenReturn(Optional.of(company));
        when(assignmentRepository.findByTeacherMemberIdAndBranchId(teacherId, branch.getId()))
                .thenReturn(Optional.empty());
        when(assignmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherBranchAssignmentResponse response = teacherBranchAssignmentService.createAssignment(teacherId, request);

        assertThat(response.branchId()).isEqualTo(branch.getId());
        assertThat(response.role()).isEqualTo(BranchRole.FREELANCE);
    }

    @Test
    void createAssignment_shouldThrow_whenBranchUnverifiedAndDifferentCreator() {
        UUID creatorId = UUID.randomUUID();
        branch = Branch.create(branch.getCompanyId(), "잠실", creatorId, VerifiedStatus.UNVERIFIED);
        ReflectionTestUtils.setField(branch, "id", UUID.randomUUID());
        when(branchRepository.findById(branch.getId())).thenReturn(Optional.of(branch));

        assertThatThrownBy(() -> teacherBranchAssignmentService.createAssignment(
                teacherId,
                new TeacherBranchAssignmentCreateRequest(
                        AssignmentCreationMode.EXISTING_BRANCH,
                        branch.getId(),
                        null,
                        null,
                        null,
                        null
                )
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }

    @Test
    void createAssignment_shouldEnableExistingAssignment_whenInactive() {
        TeacherBranchAssignment disabled = TeacherBranchAssignment.create(teacherId, branch.getId(), BranchRole.FREELANCE);
        disabled.disable();
        when(branchRepository.findById(branch.getId())).thenReturn(Optional.of(branch));
        when(companyRepository.findById(company.getId())).thenReturn(Optional.of(company));
        when(assignmentRepository.findByTeacherMemberIdAndBranchId(teacherId, branch.getId()))
                .thenReturn(Optional.of(disabled));
        when(assignmentRepository.save(disabled)).thenReturn(disabled);

        TeacherBranchAssignmentResponse response = teacherBranchAssignmentService.createAssignment(
                teacherId,
                new TeacherBranchAssignmentCreateRequest(
                        AssignmentCreationMode.EXISTING_BRANCH,
                        branch.getId(),
                        null,
                        null,
                        null,
                        null
                )
        );

        assertThat(response.deletedAt()).isNull();
        verify(assignmentRepository).save(disabled);
    }

    @Test
    void createAssignment_shouldForceOwnerRole_forIndividualCompany() {
        company = Company.create("개인학원", null, CompanyType.INDIVIDUAL, VerifiedStatus.VERIFIED, teacherId);
        ReflectionTestUtils.setField(company, "id", branch.getCompanyId());
        when(branchRepository.findById(branch.getId())).thenReturn(Optional.of(branch));
        when(companyRepository.findById(company.getId())).thenReturn(Optional.of(company));
        when(assignmentRepository.findByTeacherMemberIdAndBranchId(teacherId, branch.getId()))
                .thenReturn(Optional.empty());
        when(assignmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherBranchAssignmentResponse response = teacherBranchAssignmentService.createAssignment(
                teacherId,
                new TeacherBranchAssignmentCreateRequest(
                        AssignmentCreationMode.EXISTING_BRANCH,
                        branch.getId(),
                        BranchRole.FREELANCE,
                        null,
                        null,
                        null
                )
        );

        assertThat(response.role()).isEqualTo(BranchRole.OWNER);
    }

    @Test
    void createAssignment_shouldCreateIndividualBranch_whenModeNewIndividual() {
        UUID newCompanyId = UUID.randomUUID();
        CompanyResponse companyResponse = new CompanyResponse(
                newCompanyId,
                "개인학원",
                null,
                CompanyType.INDIVIDUAL,
                VerifiedStatus.VERIFIED,
                teacherId,
                java.time.LocalDateTime.now(),
                null
        );
        Branch newBranch = Branch.create(newCompanyId, "강남직영", teacherId, VerifiedStatus.VERIFIED);
        ReflectionTestUtils.setField(newBranch, "id", UUID.randomUUID());
        Company newCompany = Company.create("개인학원", null, CompanyType.INDIVIDUAL, VerifiedStatus.VERIFIED, teacherId);
        ReflectionTestUtils.setField(newCompany, "id", newCompanyId);
        when(companyCommandService.createCompany(eq(teacherId), any(CompanyCreateRequest.class)))
                .thenReturn(companyResponse);
        when(branchRepository.findByCompanyIdAndCreatorMemberIdAndDeletedAtIsNull(newCompanyId, teacherId))
                .thenReturn(List.of(newBranch));
        when(companyRepository.findById(newCompanyId)).thenReturn(Optional.of(newCompany));
        when(assignmentRepository.findByTeacherMemberIdAndBranchId(teacherId, newBranch.getId()))
                .thenReturn(Optional.empty());
        when(assignmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherBranchAssignmentResponse response = teacherBranchAssignmentService.createAssignment(
                teacherId,
                new TeacherBranchAssignmentCreateRequest(
                        AssignmentCreationMode.NEW_INDIVIDUAL,
                        null,
                        null,
                        new IndividualInput("개인학원", "강남직영"),
                        null,
                        null
                )
        );

        assertThat(response.branchName()).isEqualTo("강남직영");
    }

    @Test
    void createAssignment_shouldCreateCompanyBranch_whenModeNewCompany() {
        UUID newCompanyId = UUID.randomUUID();
        CompanyResponse companyResponse = new CompanyResponse(
                newCompanyId,
                "러셀",
                null,
                CompanyType.ACADEMY,
                VerifiedStatus.UNVERIFIED,
                teacherId,
                java.time.LocalDateTime.now(),
                null
        );
        Branch newBranch = Branch.create(newCompanyId, "롯데타워", teacherId, VerifiedStatus.UNVERIFIED);
        ReflectionTestUtils.setField(newBranch, "id", UUID.randomUUID());
        Company newCompany = Company.create("러셀", null, CompanyType.ACADEMY, VerifiedStatus.UNVERIFIED, teacherId);
        ReflectionTestUtils.setField(newCompany, "id", newCompanyId);
        when(companyCommandService.createCompany(eq(teacherId), any(CompanyCreateRequest.class)))
                .thenReturn(companyResponse);
        when(branchRepository.findByCompanyIdAndName(newCompanyId, "롯데타워"))
                .thenReturn(Optional.of(newBranch));
        when(companyRepository.findById(newCompanyId)).thenReturn(Optional.of(newCompany));
        when(assignmentRepository.findByTeacherMemberIdAndBranchId(teacherId, newBranch.getId()))
                .thenReturn(Optional.empty());
        when(assignmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherBranchAssignmentResponse response = teacherBranchAssignmentService.createAssignment(
                teacherId,
                new TeacherBranchAssignmentCreateRequest(
                        AssignmentCreationMode.NEW_COMPANY,
                        null,
                        null,
                        null,
                        new CompanyInput("러셀", "롯데타워"),
                        null
                )
        );

        assertThat(response.branchName()).isEqualTo("롯데타워");
    }

    @Test
    void createAssignment_shouldCreateBranchUnderExistingCompany() {
        BranchResponse branchResponse = new BranchResponse(
                branch.getId(),
                branch.getCompanyId(),
                branch.getName(),
                branch.getVerifiedStatus(),
                branch.getCreatorMemberId(),
                branch.getCreatedAt(),
                branch.getDeletedAt()
        );
        when(branchCommandService.createBranch(eq(teacherId), any(BranchCreateRequest.class)))
                .thenReturn(branchResponse);
        when(branchRepository.findById(branch.getId())).thenReturn(Optional.of(branch));
        when(companyRepository.findById(branch.getCompanyId())).thenReturn(Optional.of(company));
        when(assignmentRepository.findByTeacherMemberIdAndBranchId(teacherId, branch.getId()))
                .thenReturn(Optional.empty());
        when(assignmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherBranchAssignmentResponse response = teacherBranchAssignmentService.createAssignment(
                teacherId,
                new TeacherBranchAssignmentCreateRequest(
                        AssignmentCreationMode.NEW_BRANCH,
                        null,
                        null,
                        null,
                        null,
                        new BranchInput(company.getId(), "강남")
                )
        );

        assertThat(response.companyId()).isEqualTo(company.getId());
    }

    @Test
    void getAssignments_shouldReturnResponses() {
        TeacherBranchAssignment assignment = TeacherBranchAssignment.create(teacherId, branch.getId(), BranchRole.FREELANCE);
        Page<TeacherBranchAssignment> page = new PageImpl<>(List.of(assignment), PageRequest.of(0, 10), 1);
        when(assignmentRepository.findByTeacherMemberIdAndDeletedAtIsNull(eq(teacherId), any()))
                .thenReturn(page);
        when(branchRepository.findAllById(any())).thenReturn(List.of(branch));
        when(companyRepository.findAllById(any())).thenReturn(List.of(company));

        PageResponse<TeacherBranchAssignmentResponse> response = teacherBranchAssignmentService.getAssignments(
                teacherId,
                TeacherBranchAssignmentStatusFilter.ACTIVE,
                0,
                10
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).branchName()).isEqualTo("강남");
    }

    @Test
    void updateAssignmentStatus_shouldDisableAssignment() {
        TeacherBranchAssignment assignment = TeacherBranchAssignment.create(teacherId, branch.getId(), BranchRole.FREELANCE);
        when(assignmentRepository.findByIdAndTeacherMemberId(assignment.getId(), teacherId))
                .thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(assignment)).thenReturn(assignment);
        when(branchRepository.findById(branch.getId())).thenReturn(Optional.of(branch));
        when(companyRepository.findById(company.getId())).thenReturn(Optional.of(company));

        TeacherBranchAssignmentResponse response = teacherBranchAssignmentService.updateAssignmentStatus(
                teacherId,
                assignment.getId(),
                new TeacherBranchAssignmentStatusUpdateRequest(false)
        );

        assertThat(response.deletedAt()).isNotNull();
        verify(assignmentRepository).save(assignment);
    }

    @Test
    void updateAssignmentStatus_shouldThrow_whenAssignmentNotFound() {
        UUID assignmentId = UUID.randomUUID();
        when(assignmentRepository.findByIdAndTeacherMemberId(assignmentId, teacherId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherBranchAssignmentService.updateAssignmentStatus(
                teacherId,
                assignmentId,
                new TeacherBranchAssignmentStatusUpdateRequest(true)
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.TEACHER_BRANCH_ASSIGNMENT_NOT_FOUND);
    }
}
