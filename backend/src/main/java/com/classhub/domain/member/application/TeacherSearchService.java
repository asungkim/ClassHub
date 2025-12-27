package com.classhub.domain.member.application;

import com.classhub.domain.assignment.model.TeacherBranchAssignment;
import com.classhub.domain.assignment.model.TeacherStudentAssignment;
import com.classhub.domain.assignment.repository.TeacherBranchAssignmentRepository;
import com.classhub.domain.assignment.repository.TeacherStudentAssignmentRepository;
import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.branch.repository.BranchRepository;
import com.classhub.domain.company.company.model.Company;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.domain.company.company.repository.CompanyRepository;
import com.classhub.domain.assignment.model.TeacherStudentRequestStatus;
import com.classhub.domain.assignment.repository.StudentTeacherRequestRepository;
import com.classhub.domain.member.dto.response.TeacherSearchResponse;
import com.classhub.domain.member.dto.response.TeacherSearchResponse.TeacherBranchSummary;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.response.PageResponse;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TeacherSearchService {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "name");

    private final MemberRepository memberRepository;
    private final TeacherBranchAssignmentRepository teacherBranchAssignmentRepository;
    private final BranchRepository branchRepository;
    private final CompanyRepository companyRepository;
    private final StudentTeacherRequestRepository studentTeacherRequestRepository;
    private final TeacherStudentAssignmentRepository teacherStudentAssignmentRepository;

    public PageResponse<TeacherSearchResponse> searchTeachers(UUID studentId,
                                                              String keyword,
                                                              UUID companyId,
                                                              UUID branchId,
                                                              int page,
                                                              int size) {
        String trimmedKeyword = trimKeyword(keyword);
        Pageable pageable = PageRequest.of(page, size, DEFAULT_SORT);
        if (trimmedKeyword == null) {
            return PageResponse.from(new PageImpl<>(List.of(), pageable, 0));
        }

        Page<Member> teacherPage = memberRepository.findByRoleAndDeletedAtIsNullAndNameContainingIgnoreCase(
                MemberRole.TEACHER,
                trimmedKeyword,
                pageable
        );
        if (teacherPage.isEmpty()) {
            return PageResponse.from(new PageImpl<>(List.of(), pageable, 0));
        }

        List<Member> candidates = teacherPage.getContent();
        Set<UUID> excludedTeacherIds = resolveExcludedTeacherIds(studentId);
        List<Member> filteredTeachers = candidates.stream()
                .filter(teacher -> !excludedTeacherIds.contains(teacher.getId()))
                .toList();
        if (filteredTeachers.isEmpty()) {
            return PageResponse.from(new PageImpl<>(List.of(), pageable, 0));
        }

        Map<UUID, List<TeacherBranchAssignment>> assignmentMap = loadBranchAssignments(filteredTeachers);
        if (assignmentMap.isEmpty()) {
            return PageResponse.from(new PageImpl<>(List.of(), pageable, 0));
        }

        Map<UUID, Branch> branchMap = loadBranchMap(assignmentMap.values());
        Map<UUID, Company> companyMap = loadCompanyMap(branchMap.values());

        List<TeacherSearchResponse> responses = new ArrayList<>();
        for (Member teacher : filteredTeachers) {
            List<TeacherBranchAssignment> assignments = assignmentMap.get(teacher.getId());
            if (assignments == null || assignments.isEmpty()) {
                continue;
            }
            List<TeacherBranchSummary> summaries = assignments.stream()
                    .map(assignment -> toBranchSummary(assignment, branchMap, companyMap, companyId, branchId))
                    .filter(Objects::nonNull)
                    .toList();
            if (!summaries.isEmpty()) {
                responses.add(TeacherSearchResponse.from(teacher, summaries));
            }
        }

        return PageResponse.from(new PageImpl<>(responses, pageable, responses.size()));
    }

    private Set<UUID> resolveExcludedTeacherIds(UUID studentId) {
        Set<UUID> excluded = new HashSet<>();
        Set<TeacherStudentRequestStatus> statuses = EnumSet.of(
                TeacherStudentRequestStatus.PENDING,
                TeacherStudentRequestStatus.APPROVED,
                TeacherStudentRequestStatus.REJECTED
        );
        excluded.addAll(studentTeacherRequestRepository.findTeacherMemberIdsByStudentMemberIdAndStatusIn(
                studentId,
                statuses
        ));
        excluded.addAll(teacherStudentAssignmentRepository.findByStudentMemberIdAndDeletedAtIsNull(studentId)
                .stream()
                .map(TeacherStudentAssignment::getTeacherMemberId)
                .toList());
        return excluded;
    }

    private Map<UUID, List<TeacherBranchAssignment>> loadBranchAssignments(List<Member> teachers) {
        List<UUID> teacherIds = teachers.stream()
                .map(Member::getId)
                .toList();
        List<TeacherBranchAssignment> assignments = teacherBranchAssignmentRepository
                .findByTeacherMemberIdInAndDeletedAtIsNull(teacherIds);
        return assignments.stream()
                .collect(Collectors.groupingBy(TeacherBranchAssignment::getTeacherMemberId));
    }

    private Map<UUID, Branch> loadBranchMap(Iterable<List<TeacherBranchAssignment>> assignments) {
        Set<UUID> branchIds = new HashSet<>();
        for (List<TeacherBranchAssignment> teacherAssignments : assignments) {
            for (TeacherBranchAssignment assignment : teacherAssignments) {
                branchIds.add(assignment.getBranchId());
            }
        }
        if (branchIds.isEmpty()) {
            return Map.of();
        }
        List<Branch> branches = branchRepository.findAllById(branchIds);
        Map<UUID, Branch> branchMap = new HashMap<>();
        for (Branch branch : branches) {
            branchMap.put(branch.getId(), branch);
        }
        return branchMap;
    }

    private Map<UUID, Company> loadCompanyMap(Iterable<Branch> branches) {
        Set<UUID> companyIds = new HashSet<>();
        for (Branch branch : branches) {
            companyIds.add(branch.getCompanyId());
        }
        if (companyIds.isEmpty()) {
            return Map.of();
        }
        List<Company> companies = companyRepository.findAllById(companyIds);
        Map<UUID, Company> companyMap = new HashMap<>();
        for (Company company : companies) {
            companyMap.put(company.getId(), company);
        }
        return companyMap;
    }

    private TeacherBranchSummary toBranchSummary(TeacherBranchAssignment assignment,
                                                 Map<UUID, Branch> branchMap,
                                                 Map<UUID, Company> companyMap,
                                                 UUID companyId,
                                                 UUID branchId) {
        Branch branch = branchMap.get(assignment.getBranchId());
        if (branch == null || branch.isDeleted() || branch.getVerifiedStatus() != VerifiedStatus.VERIFIED) {
            return null;
        }
        if (branchId != null && !branchId.equals(branch.getId())) {
            return null;
        }
        if (companyId != null && !companyId.equals(branch.getCompanyId())) {
            return null;
        }
        Company company = companyMap.get(branch.getCompanyId());
        if (company == null || company.isDeleted() || company.getVerifiedStatus() != VerifiedStatus.VERIFIED) {
            return null;
        }
        return new TeacherBranchSummary(
                company.getId(),
                company.getName(),
                branch.getId(),
                branch.getName()
        );
    }

    private String trimKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
