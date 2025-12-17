package com.classhub.domain.personallesson.application;

import com.classhub.domain.personallesson.dto.request.PersonalLessonCreateRequest;
import com.classhub.domain.personallesson.dto.request.PersonalLessonUpdateRequest;
import com.classhub.domain.personallesson.dto.response.PersonalLessonResponse;
import com.classhub.domain.personallesson.dto.response.PersonalLessonSummary;
import com.classhub.domain.personallesson.model.PersonalLesson;
import com.classhub.domain.personallesson.repository.PersonalLessonRepository;
import com.classhub.domain.studentprofile.model.StudentProfile;
import com.classhub.domain.studentprofile.repository.StudentProfileRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonalLessonService {

    private final PersonalLessonRepository personalLessonRepository;
    private final StudentProfileRepository studentProfileRepository;

    @Transactional
    public PersonalLessonResponse createLesson(UUID teacherId, PersonalLessonCreateRequest request) {
        StudentProfile profile = getStudentProfile(teacherId, request.studentProfileId());

        PersonalLesson lesson = PersonalLesson.builder()
                .studentProfile(profile)
                .teacherId(teacherId)
                .writerId(teacherId)
                .date(request.date())
                .title(request.title())
                .content(request.content())
                .build();

        return PersonalLessonResponse.from(personalLessonRepository.save(lesson));
    }

    @Transactional(readOnly = true)
    public Page<PersonalLessonSummary> getLessons(
            UUID teacherId,
            UUID studentProfileId,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    ) {
        getStudentProfile(teacherId, studentProfileId);
        if (from != null || to != null) {
            LocalDate start = from != null ? from : LocalDate.of(1970, 1, 1);
            LocalDate end = to != null ? to : LocalDate.of(9999, 12, 31);
            return personalLessonRepository
                    .findAllByTeacherIdAndStudentProfile_IdAndDateBetweenOrderByDateDesc(
                            teacherId,
                            studentProfileId,
                            start,
                            end,
                            pageable
                    )
                    .map(PersonalLessonSummary::from);
        }

        return personalLessonRepository
                .findAllByTeacherIdAndStudentProfile_IdOrderByDateDesc(teacherId, studentProfileId, pageable)
                .map(PersonalLessonSummary::from);
    }

    @Transactional(readOnly = true)
    public PersonalLessonResponse getLesson(UUID teacherId, UUID lessonId) {
        PersonalLesson lesson = personalLessonRepository.findByIdAndTeacherId(lessonId, teacherId)
                .orElseThrow(() -> new BusinessException(RsCode.PERSONAL_LESSON_NOT_FOUND));
        return PersonalLessonResponse.from(lesson);
    }

    @Transactional
    public PersonalLessonResponse updateLesson(
            UUID teacherId,
            UUID lessonId,
            PersonalLessonUpdateRequest request
    ) {
        PersonalLesson lesson = personalLessonRepository.findByIdAndTeacherId(lessonId, teacherId)
                .orElseThrow(() -> new BusinessException(RsCode.PERSONAL_LESSON_NOT_FOUND));
        lesson.update(request.date(), request.title(), request.content());
        return PersonalLessonResponse.from(personalLessonRepository.save(lesson));
    }

    @Transactional
    public void deleteLesson(UUID teacherId, UUID lessonId) {
        PersonalLesson lesson = personalLessonRepository.findByIdAndTeacherId(lessonId, teacherId)
                .orElseThrow(() -> new BusinessException(RsCode.PERSONAL_LESSON_NOT_FOUND));
        personalLessonRepository.delete(lesson);
    }

    private StudentProfile getStudentProfile(UUID teacherId, UUID studentProfileId) {
        StudentProfile profile = studentProfileRepository.findByIdAndTeacherId(studentProfileId, teacherId)
                .orElseThrow(() -> new BusinessException(RsCode.STUDENT_PROFILE_NOT_FOUND));
        if (!profile.isActive()) {
            throw new BusinessException(RsCode.STUDENT_PROFILE_NOT_FOUND);
        }
        return profile;
    }

}
