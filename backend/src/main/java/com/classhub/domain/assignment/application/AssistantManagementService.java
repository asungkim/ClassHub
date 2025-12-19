package com.classhub.domain.assignment.application;

import com.classhub.domain.assignment.dto.AssistantAssignmentStatusFilter;
import com.classhub.domain.assignment.dto.response.AssistantAssignmentResponse;
import com.classhub.domain.assignment.model.TeacherAssistantAssignment;
import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.member.model.Member;
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
import org.springframework.data.domain.PageImpl;
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
