package com.classhub.domain.studentprofile.repository;

import com.classhub.domain.studentprofile.model.StudentProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {

    Optional<StudentProfile> findByIdAndTeacherId(UUID id, UUID teacherId);

    Page<StudentProfile> findAllByTeacherIdAndActive(UUID teacherId, boolean active, Pageable pageable);

    Page<StudentProfile> findAllByTeacherId(UUID teacherId, Pageable pageable);

    Page<StudentProfile> findAllByTeacherIdAndActiveAndNameContainingIgnoreCase(
            UUID teacherId,
            boolean active,
            String name,
            Pageable pageable
    );

    Page<StudentProfile> findAllByTeacherIdAndNameContainingIgnoreCase(
            UUID teacherId,
            String name,
            Pageable pageable
    );

    Page<StudentProfile> findAllByTeacherIdAndIdIn(
            UUID teacherId,
            List<UUID> profileIds,
            Pageable pageable
    );

    Page<StudentProfile> findAllByTeacherIdAndActiveAndIdIn(
            UUID teacherId,
            boolean active,
            List<UUID> profileIds,
            Pageable pageable
    );

    Page<StudentProfile> findAllByTeacherIdAndIdInAndNameContainingIgnoreCase(
            UUID teacherId,
            List<UUID> profileIds,
            String name,
            Pageable pageable
    );

    Page<StudentProfile> findAllByTeacherIdAndActiveAndIdInAndNameContainingIgnoreCase(
            UUID teacherId,
            boolean active,
            List<UUID> profileIds,
            String name,
            Pageable pageable
    );

    Optional<StudentProfile> findByTeacherIdAndPhoneNumberIgnoreCase(UUID teacherId, String phoneNumber);

    boolean existsByTeacherIdAndPhoneNumberIgnoreCase(UUID teacherId, String phoneNumber);

    boolean existsByMemberId(UUID memberId);

    // 학생 초대 후보 조회용 메서드 (memberId=null, active=true)
    List<StudentProfile> findAllByTeacherIdAndMemberIdIsNullAndActiveTrue(UUID teacherId);

    List<StudentProfile> findAllByAssistantIdAndMemberIdIsNullAndActiveTrue(UUID assistantId);

    List<StudentProfile> findAllByTeacherIdAndMemberIdIsNullAndActiveTrueAndNameContainingIgnoreCase(
            UUID teacherId,
            String name
    );

    List<StudentProfile> findAllByAssistantIdAndMemberIdIsNullAndActiveTrueAndNameContainingIgnoreCase(
            UUID assistantId,
            String name
    );
}
