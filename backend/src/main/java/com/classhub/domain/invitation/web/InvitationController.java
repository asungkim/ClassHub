package com.classhub.domain.invitation.web;

import com.classhub.domain.invitation.application.InvitationService;
import com.classhub.domain.invitation.dto.request.AssistantInvitationCreateRequest;
import com.classhub.domain.invitation.dto.request.StudentInvitationCreateRequest;
import com.classhub.domain.invitation.dto.response.InvitationResponse;
import com.classhub.domain.invitation.dto.response.StudentCandidateResponse;
import com.classhub.domain.invitation.model.InvitationRole;
import com.classhub.domain.invitation.model.InvitationStatus;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
@Tag(name = "Invitation API", description = "초대 생성/목록/취소 API")
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping("/assistant")
    @PreAuthorize("hasAnyAuthority('TEACHER')")
    @Operation(summary = "조교 초대 생성", description = "Teacher가 Assistant 초대를 생성한다.")
    public RsData<InvitationResponse> createAssistantInvitation(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody AssistantInvitationCreateRequest request
    ) {
        InvitationResponse response = invitationService.createAssistantInvitation(principal.id(), request);
        return RsData.from(RsCode.CREATED, response);
    }

    @PostMapping("/student")
    @PreAuthorize("hasAnyAuthority('TEACHER','ASSISTANT')")
    @Operation(summary = "학생 일괄 초대 생성", description = "Teacher/Assistant가 여러 StudentProfile에 대해 일괄로 Student 초대를 생성한다.")
    public RsData<List<InvitationResponse>> createStudentInvitation(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody StudentInvitationCreateRequest request
    ) {
        List<InvitationResponse> responses = invitationService.createStudentInvitations(principal.id(), request);
        return RsData.from(RsCode.CREATED, responses);
    }

    @GetMapping("/assistant")
    @PreAuthorize("hasAnyAuthority('TEACHER')")
    @Operation(summary = "조교 초대 목록", description = "Teacher가 생성한 Assistant 초대 목록을 조회한다.")
    public RsData<List<InvitationResponse>> listAssistantInvitations(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(value = "status", required = false) InvitationStatus status
    ) {
        List<InvitationResponse> responses =
                invitationService.listInvitations(principal.id(), InvitationRole.ASSISTANT, status);
        return RsData.from(RsCode.SUCCESS, responses);
    }

    @GetMapping("/student")
    @PreAuthorize("hasAnyAuthority('TEACHER','ASSISTANT')")
    @Operation(summary = "학생 초대 목록", description = "Teacher/Assistant가 생성한 Student 초대 목록을 조회한다.")
    public RsData<List<InvitationResponse>> listStudentInvitations(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(value = "status", required = false) InvitationStatus status
    ) {
        List<InvitationResponse> responses =
                invitationService.listInvitations(principal.id(), InvitationRole.STUDENT, status);
        return RsData.from(RsCode.SUCCESS, responses);
    }

    @GetMapping("/student/candidates")
    @PreAuthorize("hasAnyAuthority('TEACHER','ASSISTANT')")
    @Operation(summary = "학생 초대 후보 조회", description = "초대되지 않은 StudentProfile 목록을 조회한다 (memberId=null, active=true, PENDING 초대 없음)")
    public RsData<List<StudentCandidateResponse>> findStudentCandidates(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(value = "name", required = false) String name
    ) {
        List<StudentCandidateResponse> candidates = invitationService.findStudentCandidates(principal.id(), name);
        return RsData.from(RsCode.SUCCESS, candidates);
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("hasAnyAuthority('TEACHER','ASSISTANT')")
    @Operation(summary = "초대 취소", description = "초대 코드 기준으로 초대를 취소한다.")
    public RsData<Void> revokeInvitation(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable String code
    ) {
        invitationService.revokeInvitation(principal.id(), code);
        return RsData.from(RsCode.SUCCESS, null);
    }
}
