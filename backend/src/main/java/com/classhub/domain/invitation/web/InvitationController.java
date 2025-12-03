package com.classhub.domain.invitation.web;

import com.classhub.domain.invitation.application.InvitationService;
import com.classhub.domain.invitation.dto.AssistantInvitationCreateRequest;
import com.classhub.domain.invitation.dto.InvitationResponse;
import com.classhub.domain.invitation.dto.StudentInvitationCreateRequest;
import com.classhub.domain.invitation.model.InvitationRole;
import com.classhub.domain.invitation.model.InvitationStatus;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping("/assistant")
    public RsData<InvitationResponse> createAssistantInvitation(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody AssistantInvitationCreateRequest request
    ) {
        InvitationResponse response = invitationService.createAssistantInvitation(principal.id(), request);
        return RsData.from(RsCode.CREATED, response);
    }

    @PostMapping("/student")
    public RsData<InvitationResponse> createStudentInvitation(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody StudentInvitationCreateRequest request
    ) {
        InvitationResponse response = invitationService.createStudentInvitation(principal.id(), request);
        return RsData.from(RsCode.CREATED, response);
    }

    @GetMapping("/assistant")
    public RsData<List<InvitationResponse>> listAssistantInvitations(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(value = "status", required = false) InvitationStatus status
    ) {
        List<InvitationResponse> responses =
                invitationService.listInvitations(principal.id(), InvitationRole.ASSISTANT, status);
        return RsData.from(RsCode.SUCCESS, responses);
    }

    @GetMapping("/student")
    public RsData<List<InvitationResponse>> listStudentInvitations(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(value = "status", required = false) InvitationStatus status
    ) {
        List<InvitationResponse> responses =
                invitationService.listInvitations(principal.id(), InvitationRole.STUDENT, status);
        return RsData.from(RsCode.SUCCESS, responses);
    }

    @DeleteMapping("/{code}")
    public RsData<Void> revokeInvitation(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable String code
    ) {
        invitationService.revokeInvitation(principal.id(), code);
        return RsData.from(RsCode.SUCCESS, null);
    }
}
