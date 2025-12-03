package com.classhub.domain.course.model;

import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "course", indexes = {
        @Index(name = "idx_course_teacher", columnList = "teacher_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Course extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String company;

    @Column(name = "teacher_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID teacherId;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    public boolean isOwnedBy(UUID teacherId) {
        return teacherId != null && teacherId.equals(this.teacherId);
    }

    public void update(String name, String company) {
        if (name != null) {
            this.name = name;
        }
        if (company != null) {
            this.company = company;
        }
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}
