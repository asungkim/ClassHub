package com.classhub.domain.invitation.repository;

import com.classhub.domain.invitation.model.Invitation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByCode(String code);
}
