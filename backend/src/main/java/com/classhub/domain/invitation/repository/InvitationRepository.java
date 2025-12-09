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

    // 학생 초대 후보 조회용: StudentProfile에 PENDING 초대가 있는지 확인
    boolean existsByStudentProfileIdAndStatusIn(UUID studentProfileId, Collection<InvitationStatus> statuses);

    // 조교 초대 회전용: Teacher의 PENDING 조교 초대 찾기
    List<Invitation> findAllBySenderIdAndInviteeRoleAndStatus(
            UUID senderId,
            InvitationRole inviteeRole,
            InvitationStatus status
    );
}
