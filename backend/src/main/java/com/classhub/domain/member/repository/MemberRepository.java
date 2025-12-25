package com.classhub.domain.member.repository;

import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, UUID> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Member> findTop5ByRoleAndDeletedAtIsNullAndEmailContainingIgnoreCaseOrderByEmailAsc(
            MemberRole role,
            String emailFragment
    );

    List<Member> findTop5ByRoleAndDeletedAtIsNullAndNameContainingIgnoreCaseOrderByNameAsc(
            MemberRole role,
            String nameFragment
    );
}
