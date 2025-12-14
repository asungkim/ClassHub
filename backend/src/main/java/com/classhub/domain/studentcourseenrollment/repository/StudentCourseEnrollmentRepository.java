package com.classhub.domain.studentcourseenrollment.repository;

import com.classhub.domain.studentcourseenrollment.model.StudentCourseEnrollment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StudentCourseEnrollmentRepository extends JpaRepository<StudentCourseEnrollment, UUID> {

    List<StudentCourseEnrollment> findAllByStudentProfileId(UUID studentProfileId);

    List<StudentCourseEnrollment> findAllByStudentProfileIdIn(Collection<UUID> studentProfileIds);

    List<StudentCourseEnrollment> findAllByCourseId(UUID courseId);

    Optional<StudentCourseEnrollment> findByStudentProfileIdAndCourseId(UUID studentProfileId, UUID courseId);

    boolean existsByStudentProfileIdAndCourseId(UUID studentProfileId, UUID courseId);

    void deleteByStudentProfileIdAndCourseId(UUID studentProfileId, UUID courseId);

    boolean existsByTeacherIdAndStudentProfileId(UUID teacherId, UUID studentProfileId);

    @Query("select enrollment.courseId from StudentCourseEnrollment enrollment "
            + "where enrollment.studentProfileId = :studentProfileId")
    List<UUID> findAllCourseIdsByStudentProfileId(UUID studentProfileId);
}
