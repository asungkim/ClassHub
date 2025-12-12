package com.classhub.domain.course.model;

import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
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
    @CollectionTable(name = "course_days", joinColumns = @JoinColumn(name = "course_id"))
    @Column(name = "day_of_week", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<DayOfWeek> daysOfWeek = new HashSet<>();

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    public boolean isOwnedBy(UUID teacherId) {
        return teacherId != null && teacherId.equals(this.teacherId);
    }

    public void update(String name, String company, Set<DayOfWeek> daysOfWeek, LocalTime startTime, LocalTime endTime) {
        if (name != null) {
            this.name = name;
        }
        if (company != null) {
            this.company = company;
        }
        if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
            this.daysOfWeek = daysOfWeek;
        }
        if (startTime != null) {
            this.startTime = startTime;
        }
        if (endTime != null) {
            this.endTime = endTime;
        }
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}
