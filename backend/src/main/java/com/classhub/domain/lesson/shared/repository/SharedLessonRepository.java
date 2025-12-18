package com.classhub.domain.lesson.shared.repository;

import com.classhub.domain.lesson.shared.model.SharedLesson;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedLessonRepository extends JpaRepository<SharedLesson, UUID> {
}
