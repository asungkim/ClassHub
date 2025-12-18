package com.classhub.domain.enrollment.repository;

import com.classhub.domain.enrollment.model.StudentEnrollmentRequest;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentEnrollmentRequestRepository extends JpaRepository<StudentEnrollmentRequest, UUID> {
}
