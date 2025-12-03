package com.classhub.domain.course.repository;

import com.classhub.domain.course.model.Course;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    Optional<Course> findByIdAndActiveTrue(UUID id);

    Optional<Course> findByIdAndTeacherId(UUID id, UUID teacherId);

    Optional<Course> findByNameIgnoreCaseAndTeacherId(String name, UUID teacherId);

    boolean existsByIdAndTeacherId(UUID id, UUID teacherId);
}
