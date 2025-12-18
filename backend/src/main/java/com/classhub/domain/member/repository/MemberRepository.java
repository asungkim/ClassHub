package com.classhub.domain.member.repository;

import com.classhub.domain.member.model.Member;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, UUID> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);
}
