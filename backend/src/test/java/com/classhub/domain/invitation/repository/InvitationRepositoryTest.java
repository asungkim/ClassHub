package com.classhub.domain.invitation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.invitation.model.Invitation;
import com.classhub.domain.invitation.model.InvitationRole;
import com.classhub.domain.invitation.model.InvitationStatus;
import com.classhub.global.config.JpaConfig;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class InvitationRepositoryTest {

    @Autowired
    private InvitationRepository invitationRepository;

    @Test
    void findByCode_shouldReturnInvitation_whenExists() {
        Invitation invitation = invitationRepository.save(buildInvitation("CODE-123"));

        Optional<Invitation> result = invitationRepository.findByCode("CODE-123");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(invitation.getId());
        assertThat(result.get().getTargetEmail()).isEqualTo("assistant@classhub.com");
        assertThat(result.get().getStatus()).isEqualTo(InvitationStatus.PENDING);
    }

    @Test
    void findByCode_shouldReturnEmpty_whenInvitationDoesNotExist() {
        assertThat(invitationRepository.findByCode("UNKNOWN")).isEmpty();
    }

    @Test
    void existsByTargetEmailAndStatus_shouldReturnTrue_forPendingInvitation() {
        invitationRepository.save(buildInvitation("CODE-200"));

        boolean exists = invitationRepository.existsByTargetEmailAndStatus(
                "assistant@classhub.com", InvitationStatus.PENDING
        );

        assertThat(exists).isTrue();
    }

    @Test
    void existsByTargetEmailAndStatus_shouldReturnFalse_whenStatusMismatch() {
        Invitation invitation = invitationRepository.save(buildInvitation("CODE-201"));
        invitation.markAccepted();
        invitationRepository.save(invitation);

        boolean exists = invitationRepository.existsByTargetEmailAndStatus(
                "assistant@classhub.com", InvitationStatus.PENDING
        );

        assertThat(exists).isFalse();
    }

    @Test
    void save_shouldNormalizeTargetEmail() {
        Invitation invitation = invitationRepository.save(
                Invitation.builder()
                        .senderId(UUID.randomUUID())
                        .targetEmail(" Assistant@ClassHub.Com ")
                        .inviteeRole(InvitationRole.ASSISTANT)
                        .code("CODE-300")
                        .expiredAt(LocalDateTime.now().plusDays(5))
                        .build()
        );

        assertThat(invitation.getTargetEmail()).isEqualTo("assistant@classhub.com");
    }

    @Test
    void markAccepted_shouldUpdateStatusAndSoftDelete() {
        Invitation invitation = invitationRepository.save(buildInvitation("CODE-400"));

        invitation.markAccepted();
        invitationRepository.save(invitation);

        Invitation persisted = invitationRepository.findById(invitation.getId()).orElseThrow();

        assertThat(persisted.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(persisted.getDeletedAt()).isNotNull();
    }

    @Test
    void markExpired_shouldUpdateStatusAndSoftDelete() {
        Invitation invitation = invitationRepository.save(buildInvitation("CODE-401"));

        invitation.markExpired();
        invitationRepository.save(invitation);

        Invitation persisted = invitationRepository.findById(invitation.getId()).orElseThrow();

        assertThat(persisted.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
        assertThat(persisted.getDeletedAt()).isNotNull();
    }

    @Test
    void revoke_shouldUpdateStatusAndSoftDelete() {
        Invitation invitation = invitationRepository.save(buildInvitation("CODE-402"));

        invitation.revoke();
        invitationRepository.save(invitation);

        Invitation persisted = invitationRepository.findById(invitation.getId()).orElseThrow();

        assertThat(persisted.getStatus()).isEqualTo(InvitationStatus.REVOKED);
        assertThat(persisted.getDeletedAt()).isNotNull();
    }

    @Test
    void canUse_shouldReturnTrue_onlyWhenPendingAndNotExpiredAndActive() {
        LocalDateTime now = LocalDateTime.now();
        Invitation invitation = buildInvitation("CODE-500");

        assertThat(invitation.canUse(now)).isTrue();

        invitation.revoke();
        assertThat(invitation.canUse(now)).isFalse();

        Invitation expired = Invitation.builder()
                .senderId(UUID.randomUUID())
                .targetEmail("assistant@classhub.com")
                .inviteeRole(InvitationRole.ASSISTANT)
                .code("CODE-501")
                .expiredAt(now.minusMinutes(1))
                .build();
        assertThat(expired.canUse(now)).isFalse();
    }

    private Invitation buildInvitation(String code) {
        return Invitation.builder()
                .senderId(UUID.randomUUID())
                .targetEmail("assistant@classhub.com")
                .inviteeRole(InvitationRole.ASSISTANT)
                .code(code)
                .expiredAt(LocalDateTime.now().plusDays(3))
                .build();
    }
}
