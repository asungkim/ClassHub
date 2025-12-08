package com.classhub.domain.invitation.application;

import com.classhub.domain.invitation.dto.request.AssistantInvitationCreateRequest;
import com.classhub.domain.invitation.dto.request.StudentInvitationCreateRequest;
import com.classhub.domain.invitation.dto.response.InvitationResponse;
import com.classhub.domain.invitation.dto.response.StudentCandidateResponse;
import com.classhub.domain.invitation.model.Invitation;
import com.classhub.domain.invitation.model.InvitationRole;
import com.classhub.domain.invitation.model.InvitationStatus;
import com.classhub.domain.invitation.repository.InvitationRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.studentprofile.model.StudentProfile;
import com.classhub.domain.studentprofile.repository.StudentProfileRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private static final Duration DEFAULT_EXPIRATION = Duration.ofDays(7);
    private static final List<InvitationStatus> PENDING_STATUSES = List.of(InvitationStatus.PENDING);

    private final InvitationRepository invitationRepository;
    private final MemberRepository memberRepository;
    private final StudentProfileRepository studentProfileRepository;

    @Transactional
    public InvitationResponse createAssistantInvitation(UUID senderId, AssistantInvitationCreateRequest request) {
        Member sender = getMember(senderId);
        if (sender.getRole() != MemberRole.TEACHER) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        String email = request.normalizedEmail();
        validateDuplicate(email, InvitationRole.ASSISTANT);

        Invitation invitation = Invitation.builder()
                .senderId(sender.getId())
                .targetEmail(email)
                .inviteeRole(InvitationRole.ASSISTANT)
                .code(UUID.randomUUID().toString())
                .expiredAt(defaultExpiry())
                .build();

        return InvitationResponse.from(invitationRepository.save(invitation));
    }

    @Transactional
    public InvitationResponse createStudentInvitation(UUID senderId, StudentInvitationCreateRequest request) {
        Member sender = getMember(senderId);
        if (!canInviteStudent(sender)) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        String email = request.normalizedEmail();
        validateDuplicate(email, InvitationRole.STUDENT);

        Invitation invitation = Invitation.builder()
                .senderId(sender.getId())
                .targetEmail(email)
                .inviteeRole(InvitationRole.STUDENT)
                .studentProfileId(request.studentProfileId())
                .code(UUID.randomUUID().toString())
                .expiredAt(defaultExpiry())
                .build();

        return InvitationResponse.from(invitationRepository.save(invitation));
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> listInvitations(UUID senderId, InvitationRole role, InvitationStatus status) {
        Member sender = getMember(senderId);
        if (!isAllowedRole(sender, role)) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }

        List<InvitationStatus> statuses = status != null
                ? List.of(status)
                : List.of(InvitationStatus.values());

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<Invitation> invitations = invitationRepository.findAllBySenderIdAndInviteeRoleAndStatusIn(
                senderId,
                role,
                statuses
        );

        invitations.stream()
                .filter(invite -> invite.expireIfPast(now))
                .forEach(invitationRepository::save);

        return invitations.stream()
                .map(InvitationResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeInvitation(UUID senderId, String code) {
        Invitation invitation = invitationRepository.findByCodeAndSenderId(code, senderId)
                .orElseThrow(() -> new BusinessException(RsCode.INVALID_INVITATION));
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessException(RsCode.INVALID_INVITATION);
        }
        invitation.revoke();
        invitationRepository.save(invitation);
    }

    @Transactional(readOnly = true)
    public List<StudentCandidateResponse> findStudentCandidates(UUID memberId, String name) {
        Member member = getMember(memberId);
        if (!canInviteStudent(member)) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }

        // 1. 역할에 따라 StudentProfile 조회 (memberId=null, active=true)
        List<StudentProfile> profiles;
        if (member.getRole() == MemberRole.TEACHER) {
            profiles = (name == null || name.isBlank())
                    ? studentProfileRepository.findAllByTeacherIdAndMemberIdIsNullAndActiveTrue(memberId)
                    : studentProfileRepository.findAllByTeacherIdAndMemberIdIsNullAndActiveTrueAndNameContainingIgnoreCase(
                            memberId,
                            name
                    );
        } else {
            // ASSISTANT
            profiles = (name == null || name.isBlank())
                    ? studentProfileRepository.findAllByAssistantIdAndMemberIdIsNullAndActiveTrue(memberId)
                    : studentProfileRepository.findAllByAssistantIdAndMemberIdIsNullAndActiveTrueAndNameContainingIgnoreCase(
                            memberId,
                            name
                    );
        }

        // 2. PENDING 초대가 있는 StudentProfile 제외
        return profiles.stream()
                .filter(profile -> !invitationRepository.existsByStudentProfileIdAndStatusIn(
                        profile.getId(),
                        PENDING_STATUSES
                ))
                .map(StudentCandidateResponse::from)
                .collect(Collectors.toList());
    }

    private void validateDuplicate(String email, InvitationRole role) {
        if (invitationRepository.existsByTargetEmailIgnoreCaseAndInviteeRoleAndStatusIn(
                email,
                role,
                PENDING_STATUSES
        )) {
            throw new BusinessException(RsCode.INVITATION_ALREADY_EXISTS);
        }
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(RsCode.DUPLICATE_EMAIL);
        }
    }

    private Member getMember(UUID id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new BusinessException(RsCode.NOT_FOUND));
    }

    private boolean isAllowedRole(Member sender, InvitationRole role) {
        return switch (role) {
            case ASSISTANT -> sender.getRole() == MemberRole.TEACHER;
            case STUDENT -> sender.getRole() == MemberRole.ASSISTANT
                    || sender.getRole() == MemberRole.TEACHER;
        };
    }

    private boolean canInviteStudent(Member sender) {
        if (sender.getRole() == MemberRole.TEACHER) {
            return true;
        }
        return sender.getRole() == MemberRole.ASSISTANT && sender.getTeacherId() != null;
    }

    private LocalDateTime defaultExpiry() {
        return LocalDateTime.now(ZoneOffset.UTC).plus(DEFAULT_EXPIRATION);
    }
}
