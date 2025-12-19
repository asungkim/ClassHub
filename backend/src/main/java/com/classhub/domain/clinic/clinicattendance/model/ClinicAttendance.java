package com.classhub.domain.clinic.clinicattendance.model;

import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "clinic_attendance",
        indexes = {
                @Index(name = "idx_clinic_attendance_session", columnList = "clinic_session_id"),
                @Index(name = "idx_clinic_attendance_student", columnList = "student_course_record_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClinicAttendance extends BaseEntity {

    @Column(name = "clinic_session_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID clinicSessionId;

    @Column(name = "student_course_record_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID studentCourseRecordId;

    @Builder
    private ClinicAttendance(UUID clinicSessionId,
                             UUID studentCourseRecordId) {
        this.clinicSessionId = Objects.requireNonNull(clinicSessionId, "clinicSessionId must not be null");
        this.studentCourseRecordId = Objects.requireNonNull(studentCourseRecordId, "studentCourseRecordId must not be null");
    }
}
