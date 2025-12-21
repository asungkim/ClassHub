package com.classhub.domain.progress.personal.model;

import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "personal_lesson",
        indexes = {
                @Index(name = "idx_personal_lesson_record", columnList = "student_course_record_id"),
                @Index(name = "idx_personal_lesson_date", columnList = "lesson_date")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalProgress extends BaseEntity {

    @Column(name = "student_course_record_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID studentCourseRecordId;

    @Column(name = "writer_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID writerId;

    @Column(name = "lesson_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    private PersonalProgress(UUID studentCourseRecordId,
                             UUID writerId,
                             LocalDate date,
                             String title,
                             String content) {
        this.studentCourseRecordId = Objects.requireNonNull(studentCourseRecordId, "studentCourseRecordId must not be null");
        this.writerId = Objects.requireNonNull(writerId, "writerId must not be null");
        this.date = Objects.requireNonNull(date, "date must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null").trim();
        this.content = Objects.requireNonNull(content, "content must not be null");
    }

    public void update(LocalDate date, String title, String content) {
        if (date != null) {
            this.date = date;
        }
        if (title != null) {
            this.title = title.trim();
        }
        if (content != null) {
            this.content = content;
        }
    }
}
