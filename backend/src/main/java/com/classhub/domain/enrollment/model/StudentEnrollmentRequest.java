package com.classhub.domain.enrollment.model;

import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "student_enrollment_request",
        indexes = {
                @Index(name = "idx_enroll_req_student", columnList = "student_member_id"),
                @Index(name = "idx_enroll_req_course", columnList = "course_id"),
                @Index(name = "idx_enroll_req_status", columnList = "status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentEnrollmentRequest extends BaseEntity {

    @Column(name = "student_member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID studentMemberId;

    @Column(name = "course_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID courseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "processed_by_member_id", columnDefinition = "BINARY(16)")
    private UUID processedByMemberId;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Builder
    private StudentEnrollmentRequest(UUID studentMemberId,
                                     UUID courseId,
                                     EnrollmentStatus status,
                                     String message,
                                     UUID processedByMemberId,
                                     LocalDateTime processedAt) {
        this.studentMemberId = Objects.requireNonNull(studentMemberId, "studentMemberId must not be null");
        this.courseId = Objects.requireNonNull(courseId, "courseId must not be null");
        this.status = status == null ? EnrollmentStatus.PENDING : status;
        this.message = message;
        this.processedByMemberId = processedByMemberId;
        this.processedAt = processedAt;
    }

    public void approve(UUID processorId, LocalDateTime processedAt) {
        this.status = EnrollmentStatus.APPROVED;
        this.processedByMemberId = processorId;
        this.processedAt = processedAt == null ? LocalDateTime.now() : processedAt;
    }

    public void reject(UUID processorId, LocalDateTime processedAt) {
        this.status = EnrollmentStatus.REJECTED;
        this.processedByMemberId = processorId;
        this.processedAt = processedAt == null ? LocalDateTime.now() : processedAt;
    }

    public void cancel(UUID studentId, LocalDateTime canceledAt) {
        this.status = EnrollmentStatus.CANCELED;
        this.processedByMemberId = studentId;
        this.processedAt = canceledAt == null ? LocalDateTime.now() : canceledAt;
    }
}
