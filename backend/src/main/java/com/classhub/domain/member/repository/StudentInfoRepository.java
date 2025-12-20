package com.classhub.domain.member.repository;

import com.classhub.domain.member.model.StudentInfo;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentInfoRepository extends JpaRepository<StudentInfo, UUID> {

    Optional<StudentInfo> findByMemberId(UUID memberId);

    List<StudentInfo> findByMemberIdIn(Collection<UUID> memberIds);
}
