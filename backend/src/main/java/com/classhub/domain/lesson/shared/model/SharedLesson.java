package com.classhub.domain.lesson.shared.model;

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
        name = "shared_lesson",
        indexes = {
                @Index(name = "idx_shared_lesson_course", columnList = "course_id"),
                @Index(name = "idx_shared_lesson_date", columnList = "lesson_date")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SharedLesson extends BaseEntity {

    @Column(name = "course_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID courseId;

    @Column(name = "writer_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID writerId;

    @Column(name = "lesson_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    private SharedLesson(UUID courseId,
                         UUID writerId,
                         LocalDate date,
                         String title,
                         String content) {
        this.courseId = Objects.requireNonNull(courseId, "courseId must not be null");
        this.writerId = Objects.requireNonNull(writerId, "writerId must not be null");
        this.date = Objects.requireNonNull(date, "date must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null").trim();
        this.content = Objects.requireNonNull(content, "content must not be null");
    }
}
