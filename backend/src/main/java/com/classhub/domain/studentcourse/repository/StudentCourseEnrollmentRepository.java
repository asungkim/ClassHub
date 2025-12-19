package com.classhub.domain.studentcourse.repository;

import com.classhub.domain.studentcourse.model.StudentCourseEnrollment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentCourseEnrollmentRepository extends JpaRepository<StudentCourseEnrollment, UUID> {
}
