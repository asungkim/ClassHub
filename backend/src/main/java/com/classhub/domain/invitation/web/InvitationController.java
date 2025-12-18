package com.classhub.domain.invitation.web;

import com.classhub.domain.invitation.application.InvitationService;
import com.classhub.domain.invitation.dto.request.AssistantInvitationCreateRequest;
import com.classhub.domain.invitation.dto.response.InvitationResponse;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
@Tag(name = "Invitation API", description = "조교 초대 생성/취소 API")
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "조교 초대 생성", description = "Teacher가 조교를 초대할 수 있는 초대 코드를 발급한다.")
    public RsData<InvitationResponse> createAssistantInvitation(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody AssistantInvitationCreateRequest request
    ) {
        UUID teacherId = validateTeacher(principal);
        InvitationResponse response = invitationService.createAssistantInvitation(teacherId, request);
        return RsData.from(RsCode.CREATED, response);
    }

    @PatchMapping("/{code}/revoke")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "조교 초대 취소", description = "Teacher가 아직 사용되지 않은 초대를 취소한다.")
    public RsData<Void> revokeInvitation(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable String code
    ) {
        UUID teacherId = validateTeacher(principal);
        invitationService.revokeInvitation(teacherId, code);
        return RsData.from(RsCode.SUCCESS, null);
    }

    private UUID validateTeacher(MemberPrincipal principal) {
        if (principal == null || principal.role() != MemberRole.TEACHER) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        return principal.id();
    }
}
