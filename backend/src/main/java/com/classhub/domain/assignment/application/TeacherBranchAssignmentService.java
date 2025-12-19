package com.classhub.domain.assignment.application;

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
import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.branch.repository.BranchRepository;
import com.classhub.domain.company.branch.application.BranchCommandService;
import com.classhub.domain.company.branch.dto.request.BranchCreateRequest;
import com.classhub.domain.company.branch.dto.response.BranchResponse;
import com.classhub.domain.company.company.model.Company;
import com.classhub.domain.company.company.model.CompanyType;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.domain.company.company.repository.CompanyRepository;
import com.classhub.domain.company.company.application.CompanyCommandService;
import com.classhub.domain.company.company.dto.request.CompanyCreateRequest;
import com.classhub.domain.company.company.dto.response.CompanyResponse;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TeacherBranchAssignmentService {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final TeacherBranchAssignmentRepository assignmentRepository;
    private final BranchRepository branchRepository;
    private final BranchCommandService branchCommandService;
    private final CompanyRepository companyRepository;
    private final CompanyCommandService companyCommandService;

    public TeacherBranchAssignmentResponse createAssignment(
            UUID teacherId,
            TeacherBranchAssignmentCreateRequest request
    ) {
        Branch branch = resolveBranchForCreation(teacherId, request);
        Company company = companyRepository.findById(branch.getCompanyId())
                .orElseThrow(RsCode.COMPANY_NOT_FOUND::toException);

        TeacherBranchAssignment assignment = assignmentRepository
                .findByTeacherMemberIdAndBranchId(teacherId, branch.getId())
                .map(existing -> {
                    if (existing.isActive()) {
                        throw new BusinessException(RsCode.BRANCH_ALREADY_ASSIGNED);
                    }
                    existing.enable();
                    return existing;
                })
                .orElseGet(() -> TeacherBranchAssignment.create(
                        teacherId,
                        branch.getId(),
                        resolveRole(company.getType(), request.role())
                ));

        TeacherBranchAssignment saved = assignmentRepository.save(assignment);
        return toResponse(saved, branch, company);
    }

    @Transactional(readOnly = true)
    public PageResponse<TeacherBranchAssignmentResponse> getAssignments(
            UUID teacherId,
            TeacherBranchAssignmentStatusFilter statusFilter,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, DEFAULT_SORT);
        Page<TeacherBranchAssignment> assignments = switch (statusFilter) {
            case ACTIVE -> assignmentRepository.findByTeacherMemberIdAndDeletedAtIsNull(teacherId, pageable);
            case INACTIVE -> assignmentRepository.findByTeacherMemberIdAndDeletedAtIsNotNull(teacherId, pageable);
            case ALL -> assignmentRepository.findByTeacherMemberId(teacherId, pageable);
        };

        Map<UUID, Branch> branchMap = loadBranchMap(assignments.getContent());
        Map<UUID, Company> companyMap = loadCompanyMap(branchMap.values());

        Page<TeacherBranchAssignmentResponse> mapped = assignments.map(assignment -> {
            Branch branch = branchMap.get(assignment.getBranchId());
            if (branch == null) {
                throw new BusinessException(RsCode.BRANCH_NOT_FOUND);
            }
            Company company = companyMap.get(branch.getCompanyId());
            if (company == null) {
                throw new BusinessException(RsCode.COMPANY_NOT_FOUND);
            }
            return toResponse(assignment, branch, company);
        });

        return PageResponse.from(mapped);
    }

    public TeacherBranchAssignmentResponse updateAssignmentStatus(
            UUID teacherId,
            UUID assignmentId,
            TeacherBranchAssignmentStatusUpdateRequest request
    ) {
        TeacherBranchAssignment assignment = assignmentRepository.findByIdAndTeacherMemberId(assignmentId, teacherId)
                .orElseThrow(RsCode.TEACHER_BRANCH_ASSIGNMENT_NOT_FOUND::toException);

        if (Boolean.TRUE.equals(request.enabled())) {
            assignment.enable();
        } else if (Boolean.FALSE.equals(request.enabled())) {
            assignment.disable();
        } else {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }

        TeacherBranchAssignment saved = assignmentRepository.save(assignment);
        Branch branch = branchRepository.findById(saved.getBranchId())
                .orElseThrow(RsCode.BRANCH_NOT_FOUND::toException);
        Company company = companyRepository.findById(branch.getCompanyId())
                .orElseThrow(RsCode.COMPANY_NOT_FOUND::toException);
        return toResponse(saved, branch, company);
    }

    private void ensureBranchAccessible(Branch branch, UUID teacherId) {
        if (branch.isDeleted()) {
            throw new BusinessException(RsCode.BRANCH_NOT_FOUND);
        }
        if (branch.getVerifiedStatus() == VerifiedStatus.UNVERIFIED) {
            UUID creatorId = branch.getCreatorMemberId();
            if (creatorId == null || !creatorId.equals(teacherId)) {
                throw new BusinessException(RsCode.FORBIDDEN);
            }
        }
    }

    private BranchRole resolveRole(CompanyType companyType, BranchRole requestedRole) {
        if (companyType == CompanyType.INDIVIDUAL) {
            return BranchRole.OWNER;
        }
        return requestedRole != null ? requestedRole : BranchRole.FREELANCE;
    }

    private Branch resolveBranchForCreation(UUID teacherId, TeacherBranchAssignmentCreateRequest request) {
        if (request == null || request.mode() == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        return switch (request.mode()) {
            case EXISTING_BRANCH -> resolveExistingBranch(teacherId, request.branchId());
            case NEW_INDIVIDUAL -> createIndividualBranch(teacherId, request.individual());
            case NEW_COMPANY -> createCompanyWithBranch(teacherId, request.company());
            case NEW_BRANCH -> createBranchUnderExistingCompany(teacherId, request.branch());
        };
    }

    private Branch resolveExistingBranch(UUID teacherId, UUID branchId) {
        if (branchId == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(RsCode.BRANCH_NOT_FOUND::toException);
        ensureBranchAccessible(branch, teacherId);
        return branch;
    }

    private Branch createIndividualBranch(UUID teacherId, IndividualInput input) {
        if (input == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        CompanyCreateRequest companyRequest = new CompanyCreateRequest(
                input.companyName(),
                null,
                CompanyType.INDIVIDUAL,
                input.branchName()
        );
        CompanyResponse response = companyCommandService.createCompany(teacherId, companyRequest);
        return findBranchForNewCompany(response.companyId(), input.branchName(), teacherId);
    }

    private Branch createCompanyWithBranch(UUID teacherId, CompanyInput input) {
        if (input == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        CompanyCreateRequest companyRequest = new CompanyCreateRequest(
                input.companyName(),
                null,
                CompanyType.ACADEMY,
                input.branchName()
        );
        CompanyResponse response = companyCommandService.createCompany(teacherId, companyRequest);
        return findBranchForNewCompany(response.companyId(), input.branchName(), teacherId);
    }

    private Branch createBranchUnderExistingCompany(UUID teacherId, BranchInput input) {
        if (input == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        BranchResponse created = branchCommandService.createBranch(
                teacherId,
                new BranchCreateRequest(input.companyId(), input.branchName())
        );
        return branchRepository.findById(created.branchId())
                .orElseThrow(RsCode.BRANCH_NOT_FOUND::toException);
    }

    private Branch findBranchForNewCompany(UUID companyId, String branchName, UUID teacherId) {
        if (branchName != null && !branchName.isBlank()) {
            return branchRepository.findByCompanyIdAndName(companyId, branchName.trim())
                    .orElseGet(() -> branchRepository.findByCompanyIdAndCreatorMemberIdAndDeletedAtIsNull(companyId, teacherId)
                            .stream()
                            .findFirst()
                            .orElseThrow(RsCode.BRANCH_NOT_FOUND::toException));
        }
        return branchRepository.findByCompanyIdAndCreatorMemberIdAndDeletedAtIsNull(companyId, teacherId)
                .stream()
                .findFirst()
                .orElseThrow(RsCode.BRANCH_NOT_FOUND::toException);
    }

    private Map<UUID, Branch> loadBranchMap(List<TeacherBranchAssignment> assignments) {
        if (assignments.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UUID> branchIds = assignments.stream()
                .map(TeacherBranchAssignment::getBranchId)
                .distinct()
                .toList();
        List<Branch> branches = StreamSupport.stream(
                branchRepository.findAllById(branchIds).spliterator(),
                false
        ).toList();
        Map<UUID, Branch> branchMap = branches.stream()
                .collect(Collectors.toMap(Branch::getId, Function.identity()));
        if (branchMap.size() != branchIds.size()) {
            throw new BusinessException(RsCode.BRANCH_NOT_FOUND);
        }
        return branchMap;
    }

    private Map<UUID, Company> loadCompanyMap(Collection<Branch> branches) {
        if (branches.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UUID> companyIds = branches.stream()
                .map(Branch::getCompanyId)
                .distinct()
                .toList();
        List<Company> companies = StreamSupport.stream(
                companyRepository.findAllById(companyIds).spliterator(),
                false
        ).toList();
        Map<UUID, Company> companyMap = companies.stream()
                .collect(Collectors.toMap(Company::getId, Function.identity()));
        if (companyMap.size() != companyIds.size()) {
            throw new BusinessException(RsCode.COMPANY_NOT_FOUND);
        }
        return companyMap;
    }

    private TeacherBranchAssignmentResponse toResponse(
            TeacherBranchAssignment assignment,
            Branch branch,
            Company company
    ) {
        return new TeacherBranchAssignmentResponse(
                assignment.getId(),
                branch.getId(),
                branch.getName(),
                company.getId(),
                company.getName(),
                company.getType(),
                branch.getVerifiedStatus(),
                assignment.getRole(),
                assignment.getCreatedAt(),
                assignment.getDeletedAt()
        );
    }
}
