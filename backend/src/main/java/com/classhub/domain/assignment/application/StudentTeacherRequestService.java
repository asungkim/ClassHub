package com.classhub.domain.assignment.application;

import com.classhub.domain.assignment.model.TeacherAssistantAssignment;
import com.classhub.domain.assignment.model.TeacherBranchAssignment;
import com.classhub.domain.assignment.model.TeacherStudentAssignment;
import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.assignment.repository.TeacherBranchAssignmentRepository;
import com.classhub.domain.assignment.repository.TeacherStudentAssignmentRepository;
import com.classhub.domain.assignment.dto.request.StudentTeacherRequestCreateRequest;
import com.classhub.domain.assignment.dto.response.StudentTeacherRequestResponse;
import com.classhub.domain.assignment.model.StudentTeacherRequest;
import com.classhub.domain.assignment.model.TeacherStudentRequestStatus;
import com.classhub.domain.assignment.repository.StudentTeacherRequestRepository;
import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.branch.repository.BranchRepository;
import com.classhub.domain.company.company.model.Company;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.domain.company.company.repository.CompanyRepository;
import com.classhub.domain.member.dto.response.TeacherSearchResponse;
import com.classhub.domain.member.dto.response.TeacherSearchResponse.TeacherBranchSummary;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.model.StudentInfo;
import com.classhub.domain.member.repository.StudentInfoRepository;
import com.classhub.domain.member.dto.response.StudentSummaryResponse;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.util.KstTime;
import java.time.LocalDate;
import java.time.Period;
import java.time.LocalDateTime;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StudentTeacherRequestService {

    private static final Set<TeacherStudentRequestStatus> DUPLICATE_CHECK_STATUSES =
            EnumSet.of(TeacherStudentRequestStatus.PENDING,
                    TeacherStudentRequestStatus.APPROVED,
                    TeacherStudentRequestStatus.REJECTED);

    private final StudentTeacherRequestRepository requestRepository;
    private final TeacherStudentAssignmentRepository teacherStudentAssignmentRepository;
    private final MemberRepository memberRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final TeacherAssistantAssignmentRepository assistantAssignmentRepository;
    private final TeacherBranchAssignmentRepository teacherBranchAssignmentRepository;
    private final BranchRepository branchRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public StudentTeacherRequestResponse createRequest(UUID studentId, StudentTeacherRequestCreateRequest request) {
        UUID teacherId = Objects.requireNonNull(request.teacherId(), "teacherId must not be null");
        Member teacher = memberRepository.findById(teacherId)
                .orElseThrow(RsCode.MEMBER_NOT_FOUND::toException);
        if (teacher.isDeleted()) {
            throw new BusinessException(RsCode.MEMBER_INACTIVE);
        }
        if (teacher.getRole() != MemberRole.TEACHER || teacher.getId().equals(studentId)) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        boolean hasRequest = requestRepository.existsByStudentMemberIdAndTeacherMemberIdAndStatusIn(
                studentId,
                teacherId,
                DUPLICATE_CHECK_STATUSES
        );
        if (hasRequest) {
            throw new BusinessException(RsCode.TEACHER_STUDENT_REQUEST_CONFLICT);
        }
        boolean alreadyAssigned = teacherStudentAssignmentRepository
                .existsByTeacherMemberIdAndStudentMemberIdAndDeletedAtIsNull(teacherId, studentId);
        if (alreadyAssigned) {
            throw new BusinessException(RsCode.TEACHER_STUDENT_ALREADY_ASSIGNED);
        }
        StudentTeacherRequest entity = StudentTeacherRequest.builder()
                .studentMemberId(studentId)
                .teacherMemberId(teacherId)
                .status(TeacherStudentRequestStatus.PENDING)
                .message(trimMessage(request.message()))
                .build();
        StudentTeacherRequest saved = requestRepository.save(entity);
        TeacherSearchResponse teacherResponse = buildTeacherResponse(teacher);
        StudentSummaryResponse studentSummary = buildStudentSummary(studentId);
        return toResponse(saved, teacherResponse, studentSummary);
    }

    public PageResponse<StudentTeacherRequestResponse> getMyRequests(UUID studentId,
                                                                     Set<TeacherStudentRequestStatus> statuses,
                                                                     int page,
                                                                     int size) {
        Set<TeacherStudentRequestStatus> effectiveStatuses = (statuses == null || statuses.isEmpty())
                ? EnumSet.of(TeacherStudentRequestStatus.PENDING)
                : EnumSet.copyOf(statuses);
        Pageable pageable = PageRequest.of(page, size);
        Page<StudentTeacherRequest> requestPage = requestRepository
                .findByStudentMemberIdAndStatusInOrderByCreatedAtDesc(studentId, effectiveStatuses, pageable);
        if (requestPage.isEmpty()) {
            return PageResponse.from(new PageImpl<>(List.of(), pageable, 0));
        }
        StudentSummaryResponse studentSummary = buildStudentSummary(studentId);
        Map<UUID, TeacherSearchResponse> teacherMap = buildTeacherResponseMap(requestPage.getContent());
        List<StudentTeacherRequestResponse> content = requestPage.stream()
                .map(request -> {
                    TeacherSearchResponse teacher = teacherMap.get(request.getTeacherMemberId());
                    if (teacher == null) {
                        throw new BusinessException(RsCode.MEMBER_NOT_FOUND);
                    }
                    return toResponse(request, teacher, studentSummary);
                })
                .toList();
        Page<StudentTeacherRequestResponse> dtoPage = new PageImpl<>(content, pageable, requestPage.getTotalElements());
        return PageResponse.from(dtoPage);
    }

    @Transactional
    public StudentTeacherRequestResponse cancelRequest(UUID studentId, UUID requestId) {
        StudentTeacherRequest request = requestRepository.findById(requestId)
                .orElseThrow(RsCode.TEACHER_STUDENT_REQUEST_NOT_FOUND::toException);
        if (!request.getStudentMemberId().equals(studentId)) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        if (request.getStatus() != TeacherStudentRequestStatus.PENDING) {
            throw new BusinessException(RsCode.INVALID_TEACHER_STUDENT_REQUEST_STATE);
        }
        request.cancel(studentId, null);
        Member teacher = memberRepository.findById(request.getTeacherMemberId())
                .orElseThrow(RsCode.MEMBER_NOT_FOUND::toException);
        TeacherSearchResponse teacherResponse = buildTeacherResponse(teacher);
        StudentSummaryResponse studentSummary = buildStudentSummary(studentId);
        return toResponse(request, teacherResponse, studentSummary);
    }

    public PageResponse<StudentTeacherRequestResponse> getRequestsForTeacher(UUID teacherId,
                                                                             Set<TeacherStudentRequestStatus> statuses,
                                                                             String keyword,
                                                                             int page,
                                                                             int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<StudentTeacherRequest> requestPage = requestRepository.searchRequestsForTeacher(
                teacherId,
                resolveStatuses(statuses),
                normalizeKeyword(keyword),
                pageable);
        return toPageResponse(requestPage, pageable);
    }

    public PageResponse<StudentTeacherRequestResponse> getRequestsForAssistant(UUID assistantId,
                                                                               Set<TeacherStudentRequestStatus> statuses,
                                                                               String keyword,
                                                                               int page,
                                                                               int size) {
        PageRequest pageable = PageRequest.of(page, size);
        List<TeacherAssistantAssignment> assignments = assistantAssignmentRepository
                .findByAssistantMemberIdAndDeletedAtIsNull(assistantId);
        if (assignments.isEmpty()) {
            return PageResponse.from(new PageImpl<>(List.of(), pageable, 0));
        }
        List<UUID> teacherIds = assignments.stream()
                .map(TeacherAssistantAssignment::getTeacherMemberId)
                .distinct()
                .toList();
        Page<StudentTeacherRequest> requestPage = requestRepository.searchRequestsForTeachers(
                teacherIds,
                resolveStatuses(statuses),
                normalizeKeyword(keyword),
                pageable);
        return toPageResponse(requestPage, pageable);
    }

    @Transactional
    public StudentTeacherRequestResponse approveRequest(UUID processorId, UUID requestId) {
        StudentTeacherRequest request = loadRequest(requestId);
        ensurePending(request);
        ensurePermission(processorId, request.getTeacherMemberId());
        boolean alreadyAssigned = teacherStudentAssignmentRepository
                .existsByTeacherMemberIdAndStudentMemberIdAndDeletedAtIsNull(
                        request.getTeacherMemberId(),
                        request.getStudentMemberId()
                );
        if (alreadyAssigned) {
            throw new BusinessException(RsCode.TEACHER_STUDENT_ALREADY_ASSIGNED);
        }
        request.approve(processorId, LocalDateTime.now(KstTime.clock()));
        teacherStudentAssignmentRepository.save(
                TeacherStudentAssignment.create(request.getTeacherMemberId(), request.getStudentMemberId()));
        return buildResponseWithSummary(request);
    }

    @Transactional
    public StudentTeacherRequestResponse rejectRequest(UUID processorId, UUID requestId) {
        StudentTeacherRequest request = loadRequest(requestId);
        ensurePending(request);
        ensurePermission(processorId, request.getTeacherMemberId());
        request.reject(processorId, LocalDateTime.now(KstTime.clock()));
        return buildResponseWithSummary(request);
    }

    private TeacherSearchResponse buildTeacherResponse(Member teacher) {
        List<TeacherBranchSummary> branches = loadTeacherBranchSummaries(List.of(teacher.getId()))
                .getOrDefault(teacher.getId(), List.of());
        return TeacherSearchResponse.from(teacher, branches);
    }

    private Map<UUID, TeacherSearchResponse> buildTeacherResponseMap(
            List<StudentTeacherRequest> requests
    ) {
        List<UUID> teacherIds = requests.stream()
                .map(StudentTeacherRequest::getTeacherMemberId)
                .distinct()
                .toList();
        List<Member> teachers = memberRepository.findAllById(teacherIds);
        if (teachers.size() < teacherIds.size()) {
            throw new BusinessException(RsCode.MEMBER_NOT_FOUND);
        }
        Map<UUID, List<TeacherBranchSummary>> branchSummaryMap = loadTeacherBranchSummaries(teacherIds);
        return teachers.stream()
                .collect(Collectors.toMap(
                        Member::getId,
                        teacher -> TeacherSearchResponse.from(
                                teacher,
                                branchSummaryMap.getOrDefault(teacher.getId(), List.of())
                        )
                ));
    }

    private Map<UUID, List<TeacherBranchSummary>> loadTeacherBranchSummaries(List<UUID> teacherIds) {
        List<TeacherBranchAssignment> assignments = teacherBranchAssignmentRepository
                .findByTeacherMemberIdInAndDeletedAtIsNull(teacherIds);
        if (assignments.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<TeacherBranchAssignment>> assignmentMap = assignments.stream()
                .collect(Collectors.groupingBy(TeacherBranchAssignment::getTeacherMemberId));

        Map<UUID, Branch> branchMap = loadBranchMap(assignments);
        Map<UUID, Company> companyMap = loadCompanyMap(branchMap.values());

        Map<UUID, List<TeacherBranchSummary>> summaries = new HashMap<>();
        for (Map.Entry<UUID, List<TeacherBranchAssignment>> entry : assignmentMap.entrySet()) {
            List<TeacherBranchSummary> branchSummaries = entry.getValue().stream()
                    .map(assignment -> toBranchSummary(assignment, branchMap, companyMap))
                    .filter(Objects::nonNull)
                    .toList();
            if (!branchSummaries.isEmpty()) {
                summaries.put(entry.getKey(), branchSummaries);
            }
        }
        return summaries;
    }

    private Map<UUID, Branch> loadBranchMap(List<TeacherBranchAssignment> assignments) {
        Set<UUID> branchIds = assignments.stream()
                .map(TeacherBranchAssignment::getBranchId)
                .collect(Collectors.toCollection(HashSet::new));
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
                                                 Map<UUID, Company> companyMap) {
        Branch branch = branchMap.get(assignment.getBranchId());
        if (branch == null || branch.isDeleted() || branch.getVerifiedStatus() != VerifiedStatus.VERIFIED) {
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

    private StudentTeacherRequestResponse toResponse(StudentTeacherRequest request,
                                                     TeacherSearchResponse teacher,
                                                     StudentSummaryResponse student) {
        return new StudentTeacherRequestResponse(
                request.getId(),
                teacher,
                student,
                request.getStatus(),
                request.getMessage(),
                request.getProcessedAt(),
                request.getProcessedByMemberId(),
                request.getCreatedAt()
        );
    }

    private PageResponse<StudentTeacherRequestResponse> toPageResponse(Page<StudentTeacherRequest> requestPage,
                                                                       PageRequest pageable) {
        if (requestPage.isEmpty()) {
            Page<StudentTeacherRequestResponse> empty = new PageImpl<>(List.of(), pageable, 0);
            return PageResponse.from(empty);
        }
        Map<UUID, TeacherSearchResponse> teacherMap = buildTeacherResponseMap(requestPage.getContent());
        Map<UUID, StudentSummaryResponse> studentMap = buildStudentSummaryMap(requestPage.getContent());
        List<StudentTeacherRequestResponse> content = requestPage.getContent().stream()
                .map(request -> {
                    TeacherSearchResponse teacher = teacherMap.get(request.getTeacherMemberId());
                    StudentSummaryResponse student = studentMap.get(request.getStudentMemberId());
                    if (teacher == null) {
                        throw new BusinessException(RsCode.MEMBER_NOT_FOUND);
                    }
                    if (student == null) {
                        throw new BusinessException(RsCode.STUDENT_PROFILE_NOT_FOUND);
                    }
                    return toResponse(request, teacher, student);
                })
                .toList();
        Page<StudentTeacherRequestResponse> dtoPage = new PageImpl<>(
                content,
                pageable,
                requestPage.getTotalElements());
        return PageResponse.from(dtoPage);
    }

    private StudentTeacherRequestResponse buildResponseWithSummary(StudentTeacherRequest request) {
        Member teacher = memberRepository.findById(request.getTeacherMemberId())
                .orElseThrow(RsCode.MEMBER_NOT_FOUND::toException);
        TeacherSearchResponse teacherResponse = buildTeacherResponse(teacher);
        StudentSummaryResponse studentSummary = buildStudentSummary(request.getStudentMemberId());
        return toResponse(request, teacherResponse, studentSummary);
    }

    private StudentSummaryResponse buildStudentSummary(UUID studentId) {
        Member member = memberRepository.findById(studentId)
                .orElseThrow(RsCode.MEMBER_NOT_FOUND::toException);
        StudentInfo info = studentInfoRepository.findByMemberId(studentId)
                .orElseThrow(RsCode.STUDENT_PROFILE_NOT_FOUND::toException);
        return toStudentSummary(member, info);
    }

    private Map<UUID, StudentSummaryResponse> buildStudentSummaryMap(List<StudentTeacherRequest> requests) {
        List<UUID> studentIds = requests.stream()
                .map(StudentTeacherRequest::getStudentMemberId)
                .distinct()
                .toList();
        if (studentIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Member> memberMap = memberRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(Member::getId, member -> member));
        if (memberMap.size() < studentIds.size()) {
            throw new BusinessException(RsCode.MEMBER_NOT_FOUND);
        }
        Map<UUID, StudentInfo> infoMap = studentInfoRepository.findByMemberIdIn(studentIds).stream()
                .collect(Collectors.toMap(StudentInfo::getMemberId, info -> info));
        if (infoMap.size() < studentIds.size()) {
            throw new BusinessException(RsCode.STUDENT_PROFILE_NOT_FOUND);
        }
        return studentIds.stream()
                .collect(Collectors.toMap(
                        studentId -> studentId,
                        studentId -> toStudentSummary(memberMap.get(studentId), infoMap.get(studentId))));
    }

    private StudentSummaryResponse toStudentSummary(Member member, StudentInfo info) {
        return StudentSummaryResponse.builder()
                .memberId(member.getId())
                .name(member.getName())
                .email(member.getEmail())
                .phoneNumber(member.getPhoneNumber())
                .schoolName(info.getSchoolName())
                .grade(info.getGrade().name())
                .birthDate(info.getBirthDate())
                .age(calculateAge(info.getBirthDate()))
                .parentPhone(info.getParentPhone())
                .build();
    }

    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now(KstTime.clock())).getYears();
    }

    private StudentTeacherRequest loadRequest(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(RsCode.TEACHER_STUDENT_REQUEST_NOT_FOUND::toException);
    }

    private void ensurePermission(UUID processorId, UUID teacherId) {
        if (teacherId.equals(processorId)) {
            return;
        }
        boolean allowed = assistantAssignmentRepository
                .findByTeacherMemberIdAndAssistantMemberIdAndDeletedAtIsNull(teacherId, processorId)
                .isPresent();
        if (!allowed) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
    }

    private void ensurePending(StudentTeacherRequest request) {
        if (request.getStatus() != TeacherStudentRequestStatus.PENDING) {
            throw new BusinessException(RsCode.INVALID_TEACHER_STUDENT_REQUEST_STATE);
        }
    }

    private Set<TeacherStudentRequestStatus> resolveStatuses(Set<TeacherStudentRequestStatus> statuses) {
        return (statuses == null || statuses.isEmpty())
                ? EnumSet.of(TeacherStudentRequestStatus.PENDING)
                : EnumSet.copyOf(statuses);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimMessage(String message) {
        if (message == null) {
            return null;
        }
        String trimmed = message.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
