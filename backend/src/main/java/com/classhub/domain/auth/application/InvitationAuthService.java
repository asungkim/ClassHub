package com.classhub.domain.auth.application;

import com.classhub.domain.auth.dto.request.InvitationRegisterRequest;
import com.classhub.domain.auth.dto.request.InvitationVerifyRequest;
import com.classhub.domain.auth.dto.request.LoginRequest;
import com.classhub.domain.auth.dto.response.InvitationVerifyResponse;
import com.classhub.domain.auth.dto.response.LoginResponse;
import com.classhub.domain.invitation.model.Invitation;
import com.classhub.domain.invitation.model.InvitationRole;
import com.classhub.domain.invitation.model.InvitationStatus;
import com.classhub.domain.invitation.repository.InvitationRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvitationAuthService {

    private final InvitationRepository invitationRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public InvitationVerifyResponse verify(InvitationVerifyRequest request) {
        Invitation invitation = loadActiveInvitation(request.code());
        Member inviter = memberRepository.findById(invitation.getSenderId())
                .orElseThrow(() -> new BusinessException(RsCode.INVALID_INVITATION));

        return new InvitationVerifyResponse(
                inviter.getId(),
                inviter.getName(),
                invitation.getInviteeRole().name(),
                invitation.getExpiredAt()
        );
    }

    @Transactional
    public LoginResponse registerInvited(InvitationRegisterRequest request) {
        Invitation invitation = loadActiveInvitation(request.code());
        String normalizedEmail = request.normalizedEmail();

        if (!invitation.getTargetEmail().equalsIgnoreCase(normalizedEmail)) {
            throw new BusinessException(RsCode.INVALID_INVITATION);
        }
        if (memberRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException(RsCode.DUPLICATE_EMAIL);
        }

        MemberRole memberRole = mapRole(invitation.getInviteeRole());
        UUID teacherId = resolveTeacherId(invitation, memberRole);

        Member member = Member.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.password()))
                .name(request.name().trim())
                .role(memberRole)
                .teacherId(teacherId)
                .build();
        memberRepository.save(member);

        invitation.accept();
        invitationRepository.save(invitation);

        return authService.login(new LoginRequest(normalizedEmail, request.password()));
    }

    private Invitation loadActiveInvitation(String code) {
        Invitation invitation = invitationRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(RsCode.INVALID_INVITATION));

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (invitation.getStatus() != InvitationStatus.PENDING || invitation.getExpiredAt().isBefore(now)) {
            throw new BusinessException(RsCode.INVALID_INVITATION);
        }
        return invitation;
    }

    private MemberRole mapRole(InvitationRole inviteeRole) {
        return switch (inviteeRole) {
            case ASSISTANT -> MemberRole.ASSISTANT;
            case STUDENT -> MemberRole.STUDENT;
        };
    }

    private UUID resolveTeacherId(Invitation invitation, MemberRole role) {
        if (role == MemberRole.ASSISTANT) {
            return invitation.getSenderId();
        }
        if (role == MemberRole.STUDENT) {
            Member sender = memberRepository.findById(invitation.getSenderId())
                    .orElseThrow(() -> new BusinessException(RsCode.INVALID_INVITATION));
            if (sender.getRole() == MemberRole.TEACHER) {
                return sender.getId();
            }
            UUID teacherId = sender.getTeacherId();
            if (teacherId == null) {
                throw new BusinessException(RsCode.INVALID_INVITATION);
            }
            return teacherId;
        }
        return null;
    }
}
