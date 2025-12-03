package com.classhub.domain.personallesson.model;

import com.classhub.domain.studentprofile.model.StudentProfile;
import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "personal_lesson", indexes = {
        @Index(name = "idx_personal_lesson_teacher", columnList = "teacher_id"),
        @Index(name = "idx_personal_lesson_student_profile", columnList = "student_profile_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class PersonalLesson extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false, columnDefinition = "BINARY(16)")
    private StudentProfile studentProfile;

    @Column(name = "course_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID courseId;

    @Column(name = "teacher_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID teacherId;

    @Column(name = "writer_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID writerId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public void update(LocalDate date, String content) {
        if (date != null) {
            this.date = date;
        }
        if (content != null) {
            this.content = content;
        }
    }
}
