package com.classhub.global.init.data;

import com.classhub.domain.invitation.model.Invitation;
import com.classhub.domain.invitation.model.InvitationRole;
import com.classhub.domain.invitation.model.InvitationStatus;
import com.classhub.domain.invitation.repository.InvitationRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.studentprofile.model.StudentProfile;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import com.classhub.global.init.BootstrapSeedContext;
import com.classhub.global.init.SeedKeys;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"local", "dev"})
public class InvitationInitData extends BaseInitData {

    private final InvitationRepository invitationRepository;
    private final BootstrapSeedContext seedContext;

    public InvitationInitData(InvitationRepository invitationRepository, BootstrapSeedContext seedContext) {
        super("invitation-seed", 400);
        this.invitationRepository = invitationRepository;
        this.seedContext = seedContext;
    }

    @Override
    @Transactional
    protected void doInitialize(boolean force) {
        for (InvitationSeed seed : buildSeeds()) {
            Member sender = seedContext.getRequiredMember(seed.senderKey());
            UUIDHolder holder = resolveStudentProfile(seed.studentProfileKey());
            invitationRepository.findByCode(seed.code())
                    .map(existing -> updateInvitation(existing, sender.getId(), holder.profileId(), seed))
                    .orElseGet(() -> createInvitation(sender.getId(), holder.profileId(), seed));
        }
    }

    private Invitation updateInvitation(
            Invitation invitation,
            java.util.UUID senderId,
            java.util.UUID studentProfileId,
            InvitationSeed seed
    ) {
        invitation.refresh(
                senderId,
                studentProfileId,
                seed.targetEmail(),
                seed.inviteeRole(),
                seed.status(),
                seed.expiredAt()
        );
        return invitation;
    }

    private Invitation createInvitation(
            java.util.UUID senderId,
            java.util.UUID studentProfileId,
            InvitationSeed seed
    ) {
        Invitation invitation = Invitation.builder()
                .code(seed.code())
                .senderId(senderId)
                .studentProfileId(studentProfileId)
                .targetEmail(seed.targetEmail())
                .inviteeRole(seed.inviteeRole())
                .status(seed.status())
                .expiredAt(seed.expiredAt())
                .build();
        return invitationRepository.save(invitation);
    }

    private UUIDHolder resolveStudentProfile(String profileKey) {
        if (profileKey == null) {
            return new UUIDHolder(null);
        }
        StudentProfile profile = seedContext.getRequiredStudentProfile(profileKey);
        return new UUIDHolder(profile.getId());
    }

    private List<InvitationSeed> buildSeeds() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        return List.of(
                new InvitationSeed(
                        "AST-ALPHA-001",
                        SeedKeys.TEACHER_ALPHA,
                        "new_assistant1@classhub.dev",
                        InvitationRole.ASSISTANT,
                        InvitationStatus.PENDING,
                        now.plusDays(14),
                        null
                ),
                new InvitationSeed(
                        "AST-ALPHA-002",
                        SeedKeys.TEACHER_ALPHA,
                        "new_assistant2@classhub.dev",
                        InvitationRole.ASSISTANT,
                        InvitationStatus.ACCEPTED,
                        now.minusDays(2),
                        null
                ),
                new InvitationSeed(
                        "AST-BETA-001",
                        SeedKeys.TEACHER_BETA,
                        "new_beta_assistant@classhub.dev",
                        InvitationRole.ASSISTANT,
                        InvitationStatus.PENDING,
                        now.plusDays(10),
                        null
                ),
                new InvitationSeed(
                        "STD-ALPHA-001",
                        SeedKeys.assistantKey(SeedKeys.TEACHER_ALPHA, 1),
                        "alpha_student_candidate@classhub.dev",
                        InvitationRole.STUDENT,
                        InvitationStatus.PENDING,
                        now.plusDays(7),
                        SeedKeys.studentProfileKey(SeedKeys.TEACHER_ALPHA, 1)
                ),
                new InvitationSeed(
                        "STD-ALPHA-002",
                        SeedKeys.assistantKey(SeedKeys.TEACHER_ALPHA, 2),
                        "alpha_student_enrolled@classhub.dev",
                        InvitationRole.STUDENT,
                        InvitationStatus.ACCEPTED,
                        now.minusDays(1),
                        SeedKeys.studentProfileKey(SeedKeys.TEACHER_ALPHA, 2)
                ),
                new InvitationSeed(
                        "STD-BETA-001",
                        SeedKeys.assistantKey(SeedKeys.TEACHER_BETA, 1),
                        "beta_student_candidate@classhub.dev",
                        InvitationRole.STUDENT,
                        InvitationStatus.PENDING,
                        now.plusDays(5),
                        SeedKeys.studentProfileKey(SeedKeys.TEACHER_BETA, 1)
                )
        );
    }

    private record InvitationSeed(
            String code,
            String senderKey,
            String targetEmail,
            InvitationRole inviteeRole,
            InvitationStatus status,
            LocalDateTime expiredAt,
            String studentProfileKey
    ) {
    }

    private record UUIDHolder(java.util.UUID profileId) {
    }
}
