package com.classhub.domain.notice.model;

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
        name = "notice",
        indexes = {
                @Index(name = "idx_notice_writer", columnList = "writer_id"),
                @Index(name = "idx_notice_created", columnList = "created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseEntity {

    @Column(name = "writer_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID writerId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    private Notice(UUID writerId,
                   String title,
                   String content) {
        this.writerId = Objects.requireNonNull(writerId, "writerId must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null").trim();
        this.content = Objects.requireNonNull(content, "content must not be null");
    }
}
