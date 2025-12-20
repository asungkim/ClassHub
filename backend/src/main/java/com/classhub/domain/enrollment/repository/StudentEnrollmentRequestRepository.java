package com.classhub.domain.enrollment.repository;

import com.classhub.domain.enrollment.model.EnrollmentStatus;
import com.classhub.domain.enrollment.model.StudentEnrollmentRequest;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentEnrollmentRequestRepository extends JpaRepository<StudentEnrollmentRequest, UUID> {

    boolean existsByStudentMemberIdAndCourseIdAndStatusIn(UUID studentMemberId,
                                                          UUID courseId,
                                                          Collection<EnrollmentStatus> statuses);

    Page<StudentEnrollmentRequest> findByStudentMemberIdAndStatusInOrderByCreatedAtDesc(UUID studentMemberId,
                                                                                        Collection<EnrollmentStatus> statuses,
                                                                                        Pageable pageable);
}
