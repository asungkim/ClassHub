package com.classhub.domain.invitation.repository;

import com.classhub.domain.invitation.model.Invitation;
import com.classhub.domain.invitation.model.InvitationRole;
import com.classhub.domain.invitation.model.InvitationStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByCode(String code);

    Optional<Invitation> findByCodeAndSenderId(String code, UUID senderId);

    boolean existsByTargetEmailIgnoreCaseAndInviteeRoleAndStatusIn(
            String targetEmail,
            InvitationRole inviteeRole,
            Collection<InvitationStatus> statuses
    );

    List<Invitation> findAllBySenderIdAndInviteeRoleAndStatusIn(
            UUID senderId,
            InvitationRole inviteeRole,
            Collection<InvitationStatus> statuses
    );
}
