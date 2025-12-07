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

    Page<StudentProfile> findAllByTeacherIdAndActiveAndNameContainingIgnoreCase(
            UUID teacherId,
            boolean active,
            String name,
            Pageable pageable
    );

    Page<StudentProfile> findAllByTeacherIdAndCourseIdAndActive(
            UUID teacherId,
            UUID courseId,
            boolean active,
            Pageable pageable
    );

    Page<StudentProfile> findAllByTeacherIdAndCourseIdAndActiveAndNameContainingIgnoreCase(
            UUID teacherId,
            UUID courseId,
            boolean active,
            String name,
            Pageable pageable
    );

    List<StudentProfile> findAllByTeacherIdAndCourseIdAndActive(UUID teacherId, UUID courseId, boolean active);

    boolean existsByTeacherIdAndCourseIdAndPhoneNumberIgnoreCase(UUID teacherId, UUID courseId, String phoneNumber);

    boolean existsByMemberId(UUID memberId);

    Optional<StudentProfile> findByCourseIdAndPhoneNumberIgnoreCase(UUID courseId, String phoneNumber);
}
