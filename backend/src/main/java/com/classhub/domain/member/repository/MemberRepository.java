package com.classhub.domain.member.repository;

import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, UUID> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<Member> findAllByRoleAndTeacherId(
            MemberRole role,
            UUID teacherId,
            Pageable pageable
    );

    Page<Member> findAllByRoleAndTeacherIdAndIsActive(
            MemberRole role,
            UUID teacherId,
            boolean isActive,
            Pageable pageable
    );

    Page<Member> findAllByRoleAndTeacherIdAndNameContainingIgnoreCase(
            MemberRole role,
            UUID teacherId,
            String name,
            Pageable pageable
    );

    Page<Member> findAllByRoleAndTeacherIdAndIsActiveAndNameContainingIgnoreCase(
            MemberRole role,
            UUID teacherId,
            boolean isActive,
            String name,
            Pageable pageable
    );
}
