package com.classhub.domain.invitation.repository;

import com.classhub.domain.invitation.model.Invitation;
import com.classhub.domain.invitation.model.InvitationRole;
import com.classhub.domain.invitation.model.InvitationStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByCode(String code);

    boolean existsByTargetEmailAndStatus(String targetEmail, InvitationStatus status);

    Page<Invitation> findBySenderIdAndInviteeRole(
            UUID senderId,
            InvitationRole inviteeRole,
            Pageable pageable
    );

    Page<Invitation> findBySenderIdAndInviteeRoleAndStatus(
            UUID senderId,
            InvitationRole inviteeRole,
            InvitationStatus status,
            Pageable pageable
    );
}
