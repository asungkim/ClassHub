package com.classhub.domain.assignment.web;

import com.classhub.domain.assignment.application.AssistantManagementService;
import com.classhub.domain.assignment.dto.AssistantAssignmentStatusFilter;
import com.classhub.domain.assignment.dto.request.AssistantAssignmentStatusUpdateRequest;
import com.classhub.domain.assignment.dto.response.AssistantAssignmentResponse;
import com.classhub.domain.invitation.dto.response.InvitationSummaryResponse;
import com.classhub.domain.invitation.model.InvitationStatus;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teachers/me")
@Tag(name = "Assistant Management API", description = "Teacher 전용 조교/초대 관리 API")
public class AssistantManagementController {

    private final AssistantManagementService assistantManagementService;

    @GetMapping("/assistants")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "조교 목록 조회", description = "교사가 배정된 조교 목록을 상태별로 확인한다.")
    public RsData<PageResponse<AssistantAssignmentResponse>> getAssistants(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID teacherId = validateTeacher(principal);
        AssistantAssignmentStatusFilter statusFilter = parseAssignmentStatus(status);
        PageResponse<AssistantAssignmentResponse> response = assistantManagementService.getAssistantAssignments(
                teacherId,
                statusFilter,
                page,
                size
        );
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PatchMapping("/assistants/{assignmentId}")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "조교 활성/비활성화", description = "교사가 특정 조교의 접근 권한을 켜거나 끈다.")
    public RsData<AssistantAssignmentResponse> updateAssistantStatus(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID assignmentId,
            @Valid @RequestBody AssistantAssignmentStatusUpdateRequest request
    ) {
        UUID teacherId = validateTeacher(principal);
        AssistantAssignmentResponse response = assistantManagementService.updateAssistantStatus(
                teacherId,
                assignmentId,
                request.enabled()
        );
        return RsData.from(RsCode.SUCCESS, response);
    }

    @GetMapping("/invitations")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "조교 초대 목록 조회", description = "초대 상태별로 교사가 발행한 조교 초대를 확인한다.")
    public RsData<PageResponse<InvitationSummaryResponse>> getAssistantInvitations(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID teacherId = validateTeacher(principal);
        InvitationStatus invitationStatus = parseInvitationStatus(status);
        PageResponse<InvitationSummaryResponse> response = assistantManagementService.getAssistantInvitations(
                teacherId,
                invitationStatus,
                page,
                size
        );
        return RsData.from(RsCode.SUCCESS, response);
    }

    private UUID validateTeacher(MemberPrincipal principal) {
        if (principal == null || principal.role() != MemberRole.TEACHER) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        return principal.id();
    }

    private AssistantAssignmentStatusFilter parseAssignmentStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return AssistantAssignmentStatusFilter.ACTIVE;
        }
        try {
            return AssistantAssignmentStatusFilter.valueOf(rawStatus.toUpperCase(Locale.US));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
    }

    private InvitationStatus parseInvitationStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank() || rawStatus.equalsIgnoreCase("ALL")) {
            return null;
        }
        try {
            return InvitationStatus.valueOf(rawStatus.toUpperCase(Locale.US));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
    }
}
