package com.classhub.domain.sharedlesson.repository;

import com.classhub.domain.sharedlesson.model.SharedLesson;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedLessonRepository extends JpaRepository<SharedLesson, UUID> {

    Page<SharedLesson> findAllByCourse_TeacherIdAndCourse_IdAndDateBetween(
            UUID teacherId,
            UUID courseId,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    );

    Page<SharedLesson> findAllByCourse_TeacherIdAndCourse_Id(
            UUID teacherId,
            UUID courseId,
            Pageable pageable
    );

    Optional<SharedLesson> findByIdAndCourse_TeacherId(UUID sharedLessonId, UUID teacherId);

    Optional<SharedLesson> findByCourse_IdAndDateAndTitleIgnoreCase(UUID courseId, LocalDate date, String title);

    @EntityGraph(attributePaths = "course")
    List<SharedLesson> findAllByCourse_IdInAndDateBetweenOrderByDateAsc(
            Iterable<UUID> courseIds,
            LocalDate start,
            LocalDate end
    );
}
