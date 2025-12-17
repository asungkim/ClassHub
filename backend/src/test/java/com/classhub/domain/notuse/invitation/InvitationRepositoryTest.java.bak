package com.classhub.domain.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classhub.domain.invitation.model.Invitation;
import com.classhub.domain.invitation.model.InvitationRole;
import com.classhub.domain.invitation.model.InvitationStatus;
import com.classhub.domain.invitation.repository.InvitationRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InvitationRepositoryTest {

    @Autowired
    private InvitationRepository invitationRepository;

    @Test
    void saveInvitation() {
        Invitation invitation = Invitation.builder()
                .senderId(UUID.randomUUID())
                .targetEmail("assistant@classhub.com")
                .inviteeRole(InvitationRole.ASSISTANT)
                .code("CODE-123")
                .expiredAt(LocalDateTime.now().plusDays(3))
                .build();

        Invitation saved = invitationRepository.save(invitation);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(saved.getInviteeRole()).isEqualTo(InvitationRole.ASSISTANT);
    }

    @Test
    void uniqueCodeConstraint() {
        Invitation first = Invitation.builder()
                .senderId(UUID.randomUUID())
                .targetEmail("assistant@classhub.com")
                .inviteeRole(InvitationRole.ASSISTANT)
                .code("DUP-CODE")
                .expiredAt(LocalDateTime.now().plusDays(3))
                .build();
        invitationRepository.saveAndFlush(first);

        Invitation duplicate = Invitation.builder()
                .senderId(UUID.randomUUID())
                .targetEmail("assistant2@classhub.com")
                .inviteeRole(InvitationRole.ASSISTANT)
                .code("DUP-CODE")
                .expiredAt(LocalDateTime.now().plusDays(5))
                .build();

        assertThatThrownBy(() -> invitationRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void expireIfPastChangesStatus() {
        Invitation invitation = Invitation.builder()
                .senderId(UUID.randomUUID())
                .targetEmail("student@classhub.com")
                .inviteeRole(InvitationRole.STUDENT)
                .code("EXP-123")
                .expiredAt(LocalDateTime.now().minusHours(1))
                .build();

        boolean expired = invitation.expireIfPast(LocalDateTime.now());

        assertThat(expired).isTrue();
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
    }
}
