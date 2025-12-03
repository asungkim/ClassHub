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

    Page<StudentProfile> findAllByTeacherIdAndActiveTrue(UUID teacherId, Pageable pageable);

    Page<StudentProfile> findAllByTeacherIdAndActiveTrueAndNameContainingIgnoreCase(
            UUID teacherId,
            String name,
            Pageable pageable
    );

    Page<StudentProfile> findAllByTeacherIdAndCourseIdAndActiveTrue(
            UUID teacherId,
            UUID courseId,
            Pageable pageable
    );

    Page<StudentProfile> findAllByTeacherIdAndCourseIdAndActiveTrueAndNameContainingIgnoreCase(
            UUID teacherId,
            UUID courseId,
            String name,
            Pageable pageable
    );

    List<StudentProfile> findAllByTeacherIdAndCourseIdAndActiveTrue(UUID teacherId, UUID courseId);

    boolean existsByTeacherIdAndCourseIdAndPhoneNumberIgnoreCase(UUID teacherId, UUID courseId, String phoneNumber);

    boolean existsByMemberId(UUID memberId);
}
