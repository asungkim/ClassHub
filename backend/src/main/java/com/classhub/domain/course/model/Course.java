package com.classhub.domain.course.model;

import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "course",
        indexes = {
                @Index(name = "idx_course_branch", columnList = "branch_id"),
                @Index(name = "idx_course_teacher", columnList = "teacher_member_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course extends BaseEntity {

    @Column(name = "branch_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID branchId;

    @Column(name = "teacher_member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID teacherMemberId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "course_schedule",
            joinColumns = @JoinColumn(name = "course_id", nullable = false, columnDefinition = "BINARY(16)")
    )
    private Set<CourseSchedule> schedules = new HashSet<>();

    @Builder
    private Course(UUID branchId,
                   UUID teacherMemberId,
                   String name,
                   String description,
                   Set<CourseSchedule> schedules) {
        this.branchId = Objects.requireNonNull(branchId, "branchId must not be null");
        this.teacherMemberId = Objects.requireNonNull(teacherMemberId, "teacherMemberId must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null").trim();
        this.description = description;
        if (schedules != null && !schedules.isEmpty()) {
            this.schedules = new HashSet<>(schedules);
        }
    }

    public static Course create(UUID branchId,
                                UUID teacherMemberId,
                                String name,
                                String description,
                                Set<CourseSchedule> schedules) {
        return Course.builder()
                .branchId(branchId)
                .teacherMemberId(teacherMemberId)
                .name(name)
                .description(description)
                .schedules(schedules)
                .build();
    }

    public Set<CourseSchedule> getSchedules() {
        return Collections.unmodifiableSet(schedules);
    }

    public void updateSchedules(Set<CourseSchedule> newSchedules) {
        this.schedules.clear();
        if (newSchedules != null) {
            this.schedules.addAll(newSchedules);
        }
    }

    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class CourseSchedule {

        @Column(name = "day_of_week", nullable = false, length = 10)
        private DayOfWeek dayOfWeek;

        @Column(name = "start_time", nullable = false)
        private LocalTime startTime;

        @Column(name = "end_time", nullable = false)
        private LocalTime endTime;

        public CourseSchedule(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
            this.dayOfWeek = Objects.requireNonNull(dayOfWeek, "dayOfWeek must not be null");
            this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
            this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        }
    }
}
