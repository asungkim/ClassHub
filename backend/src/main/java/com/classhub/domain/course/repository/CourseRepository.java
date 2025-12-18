package com.classhub.domain.course.repository;

import com.classhub.domain.course.model.Course;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, UUID> {
}
