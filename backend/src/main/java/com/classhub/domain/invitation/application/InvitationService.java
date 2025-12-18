package com.classhub.domain.invitation.application;

import com.classhub.domain.assignment.model.TeacherAssistantAssignment;
import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.auth.dto.response.AuthTokens;
import com.classhub.domain.invitation.dto.request.AssistantInvitationCreateRequest;
import com.classhub.domain.invitation.dto.request.InvitationVerifyRequest;
import com.classhub.domain.invitation.dto.response.InvitationResponse;
import com.classhub.domain.invitation.dto.response.InvitationVerifyResponse;
import com.classhub.domain.invitation.model.Invitation;
import com.classhub.domain.invitation.model.InvitationRole;
import com.classhub.domain.invitation.model.InvitationStatus;
import com.classhub.domain.invitation.repository.InvitationRepository;
import com.classhub.domain.invitation.support.InvitationCodeGenerator;
import com.classhub.domain.member.application.RegisterService;
import com.classhub.domain.member.dto.request.RegisterAssistantByInvitationRequest;
import com.classhub.domain.member.dto.request.RegisterMemberRequest;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private static final long DEFAULT_EXPIRATION_DAYS = 7;

    private final InvitationRepository invitationRepository;
    private final MemberRepository memberRepository;
    private final TeacherAssistantAssignmentRepository teacherAssistantAssignmentRepository;
    private final RegisterService registerService;
    private final InvitationCodeGenerator invitationCodeGenerator;
    private final Clock clock;

    @Transactional
    public InvitationResponse createAssistantInvitation(
            UUID senderId,
            AssistantInvitationCreateRequest request
    ) {
        String normalizedTargetEmail = request.normalizedTargetEmail();
        ensureNoPendingInvitation(normalizedTargetEmail);

        LocalDateTime expiredAt = request.expiredAt() != null
                ? request.expiredAt()
                : LocalDateTime.now(clock).plusDays(DEFAULT_EXPIRATION_DAYS);

        Invitation invitation = Invitation.builder()
                .senderId(senderId)
                .targetEmail(normalizedTargetEmail)
                .inviteeRole(InvitationRole.ASSISTANT)
                .status(InvitationStatus.PENDING)
                .code(invitationCodeGenerator.generate())
                .expiredAt(expiredAt)
                .build();

        invitationRepository.save(invitation);
        return InvitationResponse.from(invitation);
    }

    @Transactional(readOnly = true)
    public InvitationVerifyResponse verifyCode(String rawCode) {
        Invitation invitation = getUsableInvitation(rawCode);
        Member sender = memberRepository.findById(invitation.getSenderId())
                .orElseThrow(() -> new BusinessException(RsCode.MEMBER_NOT_FOUND));
        return InvitationVerifyResponse.of(invitation, sender.getId(), sender.getName());
    }

    @Transactional
    public void revokeInvitation(UUID senderId, String rawCode) {
        Invitation invitation = getExistingInvitation(rawCode);

        if (!invitation.getSenderId().equals(senderId)) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        if (!invitation.isPending()) {
            throw new BusinessException(RsCode.INVALID_INVITATION);
        }

        invitation.revoke();
        invitationRepository.save(invitation);
    }

    @Transactional(readOnly = true)
    public InvitationVerifyResponse verifyCode(InvitationVerifyRequest request) {
        return verifyCode(request.trimmedCode());
    }

    @Transactional
    public AuthTokens registerAssistantViaInvitation(
            RegisterAssistantByInvitationRequest request
    ) {
        Invitation invitation = getUsableInvitation(request.trimmedCode());
        RegisterMemberRequest baseRequest = request.memberRequest();
        RegisterMemberRequest registerPayload = new RegisterMemberRequest(
                invitation.getTargetEmail(),
                baseRequest.password(),
                baseRequest.name(),
                baseRequest.phoneNumber()
        );

        AuthTokens tokens = registerService.registerAssistant(registerPayload);

        teacherAssistantAssignmentRepository.save(
                TeacherAssistantAssignment.create(invitation.getSenderId(), tokens.memberId())
        );

        invitation.markAccepted();
        invitationRepository.save(invitation);

        return tokens;
    }

    private void ensureNoPendingInvitation(String targetEmail) {
        if (invitationRepository.existsByTargetEmailAndStatus(
                targetEmail,
                InvitationStatus.PENDING
        )) {
            throw new BusinessException(RsCode.INVITATION_ALREADY_EXISTS);
        }
    }

    private Invitation getUsableInvitation(String rawCode) {
        Invitation invitation = getExistingInvitation(rawCode);
        if (!invitation.canUse(LocalDateTime.now(clock))) {
            throw new BusinessException(RsCode.INVITATION_EXPIRED);
        }
        return invitation;
    }

    private Invitation getExistingInvitation(String rawCode) {
        String code = rawCode.trim();
        return invitationRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(RsCode.INVALID_INVITATION));
    }
}
