package com.classhub.domain.personallesson.repository;

import com.classhub.domain.personallesson.model.PersonalLesson;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalLessonRepository extends JpaRepository<PersonalLesson, UUID> {

    Page<PersonalLesson> findAllByTeacherIdAndStudentProfile_IdOrderByDateDesc(
            UUID teacherId,
            UUID studentProfileId,
            Pageable pageable
    );

    Page<PersonalLesson> findAllByTeacherIdAndStudentProfile_IdAndDateBetweenOrderByDateDesc(
            UUID teacherId,
            UUID studentProfileId,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    );

    Optional<PersonalLesson> findByIdAndTeacherId(UUID id, UUID teacherId);

    Optional<PersonalLesson> findByStudentProfile_IdAndDate(UUID studentProfileId, LocalDate date);
}
