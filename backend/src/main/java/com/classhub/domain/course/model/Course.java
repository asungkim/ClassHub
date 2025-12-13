package com.classhub.domain.course.model;

import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "course_schedule", joinColumns = @JoinColumn(name = "course_id"))
    @Builder.Default
    private Set<CourseSchedule> schedules = new HashSet<>();

    @Builder.Default
    @Column(name = "start_time", nullable = false)
    private LocalTime aggregatedStartTime = LocalTime.of(0, 0);

    @Builder.Default
    @Column(name = "end_time", nullable = false)
    private LocalTime aggregatedEndTime = LocalTime.of(0, 0);

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    public boolean isOwnedBy(UUID teacherId) {
        return teacherId != null && teacherId.equals(this.teacherId);
    }

    public void update(String name, String company, Set<CourseSchedule> schedules) {
        if (name != null) {
            this.name = name;
        }
        if (company != null) {
            this.company = company;
        }
        if (schedules != null && !schedules.isEmpty()) {
            this.schedules = schedules;
        }
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    @PrePersist
    @PreUpdate
    private void updateAggregateTimes() {
        if (schedules == null || schedules.isEmpty()) {
            throw new IllegalStateException("Course schedules must not be empty");
        }
        this.aggregatedStartTime = schedules.stream()
                .min(Comparator.comparing(CourseSchedule::getStartTime))
                .orElseThrow()
                .getStartTime();
        this.aggregatedEndTime = schedules.stream()
                .max(Comparator.comparing(CourseSchedule::getEndTime))
                .orElseThrow()
                .getEndTime();
    }
}
