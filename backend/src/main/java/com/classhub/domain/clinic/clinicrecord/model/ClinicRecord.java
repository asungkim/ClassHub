package com.classhub.domain.clinic.clinicrecord.model;

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
        name = "clinic_record",
        indexes = {
                @Index(name = "idx_clinic_record_writer", columnList = "writer_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClinicRecord extends BaseEntity {

    @Column(name = "clinic_attendance_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID clinicAttendanceId;

    @Column(name = "writer_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID writerId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "homework_progress", columnDefinition = "TEXT")
    private String homeworkProgress;

    @Builder
    private ClinicRecord(UUID clinicAttendanceId,
                         UUID writerId,
                         String title,
                         String content,
                         String homeworkProgress) {
        this.clinicAttendanceId = Objects.requireNonNull(clinicAttendanceId, "clinicAttendanceId must not be null");
        this.writerId = Objects.requireNonNull(writerId, "writerId must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null").trim();
        this.content = Objects.requireNonNull(content, "content must not be null");
        this.homeworkProgress = homeworkProgress;
    }
}
