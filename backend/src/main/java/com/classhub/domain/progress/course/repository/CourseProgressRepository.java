package com.classhub.domain.progress.course.repository;

import com.classhub.domain.progress.course.model.CourseProgress;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseProgressRepository extends JpaRepository<CourseProgress, UUID> {
}
