package com.classhub.domain.assignment.application;

import com.classhub.domain.assignment.dto.AssistantAssignmentStatusFilter;
import com.classhub.domain.assignment.dto.response.AssistantAssignmentResponse;
import com.classhub.domain.assignment.dto.response.AssistantSearchResponse;
import com.classhub.domain.assignment.model.TeacherAssistantAssignment;
import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AssistantManagementService {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final TeacherAssistantAssignmentRepository assignmentRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public PageResponse<AssistantAssignmentResponse> getAssistantAssignments(
            UUID teacherId,
            AssistantAssignmentStatusFilter statusFilter,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, DEFAULT_SORT);
        Page<TeacherAssistantAssignment> assignments = switch (statusFilter) {
            case ACTIVE -> assignmentRepository.findByTeacherMemberIdAndDeletedAtIsNull(teacherId, pageable);
            case INACTIVE -> assignmentRepository.findByTeacherMemberIdAndDeletedAtIsNotNull(teacherId, pageable);
            case ALL -> assignmentRepository.findByTeacherMemberId(teacherId, pageable);
        };

        Map<UUID, Member> assistantMap = loadAssistantMap(assignments.getContent());
        Page<AssistantAssignmentResponse> responsePage = assignments.map(
                assignment -> AssistantAssignmentResponse.from(
                        assignment,
                        assistantMap.get(assignment.getAssistantMemberId())
                )
        );
        return PageResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public List<AssistantSearchResponse> searchAssistants(UUID teacherId, String rawEmail) {
        if (rawEmail == null || rawEmail.trim().isEmpty()) {
            return List.of();
        }

        List<Member> assistants = memberRepository
                .findTop5ByRoleAndDeletedAtIsNullAndEmailContainingIgnoreCaseOrderByEmailAsc(
                        MemberRole.ASSISTANT,
                        rawEmail.trim()
                );
        if (assistants.isEmpty()) {
            return List.of();
        }

        List<UUID> assistantIds = assistants.stream()
                .map(Member::getId)
                .toList();
        Map<UUID, TeacherAssistantAssignment> assignmentMap = assignmentRepository
                .findByTeacherMemberIdAndAssistantMemberIdIn(teacherId, assistantIds)
                .stream()
                .collect(Collectors.toMap(TeacherAssistantAssignment::getAssistantMemberId, assignment -> assignment));

        return assistants.stream()
                .map(assistant -> AssistantSearchResponse.from(assistant, assignmentMap.get(assistant.getId())))
                .toList();
    }

    public AssistantAssignmentResponse assignAssistant(UUID teacherId, UUID assistantMemberId) {
        if (assistantMemberId == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        Member assistant = memberRepository.findById(assistantMemberId)
                .orElseThrow(RsCode.MEMBER_NOT_FOUND::toException);
        if (assistant.isDeleted()) {
            throw new BusinessException(RsCode.MEMBER_INACTIVE);
        }
        if (assistant.getRole() != MemberRole.ASSISTANT || assistant.getId().equals(teacherId)) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }

        TeacherAssistantAssignment assignment = assignmentRepository
                .findByTeacherMemberIdAndAssistantMemberId(teacherId, assistantMemberId)
                .map(existing -> {
                    if (existing.isActive()) {
                        throw new BusinessException(RsCode.ASSISTANT_ALREADY_ASSIGNED);
                    }
                    existing.enable();
                    return existing;
                })
                .orElseGet(() -> TeacherAssistantAssignment.create(teacherId, assistantMemberId));

        TeacherAssistantAssignment saved = assignmentRepository.save(assignment);
        return AssistantAssignmentResponse.from(saved, assistant);
    }

    public AssistantAssignmentResponse updateAssistantStatus(
            UUID teacherId,
            UUID assignmentId,
            boolean enabled
    ) {
        TeacherAssistantAssignment assignment = assignmentRepository.findByIdAndTeacherMemberId(assignmentId, teacherId)
                .orElseThrow(RsCode.ASSISTANT_NOT_FOUND::toException);

        if (enabled) {
            assignment.enable();
        } else {
            assignment.disable();
        }

        TeacherAssistantAssignment saved = assignmentRepository.save(assignment);
        Member assistant = memberRepository.findById(saved.getAssistantMemberId())
                .orElseThrow(RsCode.MEMBER_NOT_FOUND::toException);

        return AssistantAssignmentResponse.from(saved, assistant);
    }

    private Map<UUID, Member> loadAssistantMap(List<TeacherAssistantAssignment> assignments) {
        List<UUID> assistantIds = assignments.stream()
                .map(TeacherAssistantAssignment::getAssistantMemberId)
                .distinct()
                .toList();
        List<Member> assistants = memberRepository.findAllById(assistantIds);
        if (assistantIds.size() != assistants.size()) {
            throw new BusinessException(RsCode.MEMBER_NOT_FOUND);
        }
        return assistants.stream()
                .collect(Collectors.toMap(Member::getId, member -> member));
    }
}
