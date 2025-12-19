package com.classhub.domain.lesson.personal.repository;

import com.classhub.domain.lesson.personal.model.PersonalLesson;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalLessonRepository extends JpaRepository<PersonalLesson, UUID> {
}
