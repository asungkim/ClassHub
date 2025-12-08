package com.classhub.domain.auth.application;

import com.classhub.domain.auth.dto.request.InvitationRegisterRequest;
import com.classhub.domain.auth.dto.request.InvitationVerifyRequest;
import com.classhub.domain.auth.dto.request.LoginRequest;
import com.classhub.domain.auth.dto.response.AuthTokens;
import com.classhub.domain.auth.dto.response.InvitationVerifyResponse;
import com.classhub.domain.invitation.dto.response.StudentCandidateResponse;
import com.classhub.domain.invitation.model.Invitation;
import com.classhub.domain.invitation.model.InvitationRole;
import com.classhub.domain.invitation.repository.InvitationRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.studentprofile.model.StudentProfile;
import com.classhub.domain.studentprofile.repository.StudentProfileRepository;
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
    private final StudentProfileRepository studentProfileRepository;

    @Transactional(readOnly = true)
    public InvitationVerifyResponse verify(InvitationVerifyRequest request) {
        Invitation invitation = loadActiveInvitation(request.code());
        Member inviter = memberRepository.findById(invitation.getSenderId())
                .orElseThrow(() -> new BusinessException(RsCode.INVALID_INVITATION));

        StudentCandidateResponse studentProfile = null;
        if (invitation.getInviteeRole() == InvitationRole.STUDENT && invitation.getStudentProfileId() != null) {
            StudentProfile profile = studentProfileRepository.findById(invitation.getStudentProfileId())
                    .orElseThrow(() -> new BusinessException(RsCode.INVALID_INVITATION));
            studentProfile = StudentCandidateResponse.from(profile);
        }

        return new InvitationVerifyResponse(
                inviter.getId(),
                inviter.getName(),
                invitation.getInviteeRole().name(),
                invitation.getExpiredAt(),
                studentProfile
        );
    }

    @Transactional
    public AuthTokens registerInvited(InvitationRegisterRequest request) {
        Invitation invitation = loadActiveInvitation(request.code());
        String normalizedEmail = request.normalizedEmail();

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

        // 학생 초대인 경우 StudentProfile.memberId 연결
        if (invitation.getInviteeRole() == InvitationRole.STUDENT && invitation.getStudentProfileId() != null) {
            StudentProfile profile = studentProfileRepository.findById(invitation.getStudentProfileId())
                    .orElseThrow(() -> new BusinessException(RsCode.INVALID_INVITATION));
            profile.assignMember(member.getId());
            studentProfileRepository.save(profile);
        }

        // useCount 증가
        invitation.increaseUseCount();
        // 단일 사용(학생 초대) 또는 제한 도달 시 ACCEPTED로 상태 변경
        invitation.acceptIfLimitReached();
        invitationRepository.save(invitation);

        return authService.login(new LoginRequest(normalizedEmail, request.password()));
    }

    private Invitation loadActiveInvitation(String code) {
        Invitation invitation = invitationRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(RsCode.INVALID_INVITATION));

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (!invitation.canUse(now)) {
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
