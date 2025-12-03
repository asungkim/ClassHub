package com.classhub.global.init.data;

import com.classhub.domain.personallesson.model.PersonalLesson;
import com.classhub.domain.personallesson.repository.PersonalLessonRepository;
import com.classhub.domain.studentprofile.model.StudentProfile;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.classhub.global.init.BootstrapSeedContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"local", "dev"})
public class PersonalLessonInitData extends BaseInitData {

    private final PersonalLessonRepository personalLessonRepository;
    private final BootstrapSeedContext seedContext;

    public PersonalLessonInitData(
            PersonalLessonRepository personalLessonRepository,
            BootstrapSeedContext seedContext
    ) {
        super("personal-lesson-seed", 500);
        this.personalLessonRepository = personalLessonRepository;
        this.seedContext = seedContext;
    }

    @Override
    @Transactional
    protected void doInitialize(boolean force) {
        Collection<StudentProfile> profiles = seedContext.getAllStudentProfiles();
        LocalDate today = LocalDate.now();
        for (StudentProfile profile : profiles) {
            seedLessonsForProfile(profile, today);
        }
    }

    private void seedLessonsForProfile(StudentProfile profile, LocalDate today) {
        List<LessonSeed> seeds = buildLessonSeeds(profile, today);
        for (LessonSeed seed : seeds) {
            personalLessonRepository.findByStudentProfile_IdAndDate(profile.getId(), seed.date())
                    .map(existing -> updateLesson(existing, seed))
                    .orElseGet(() -> createLesson(profile, seed));
        }
    }

    private PersonalLesson updateLesson(PersonalLesson lesson, LessonSeed seed) {
        lesson.update(seed.date(), seed.content());
        return lesson;
    }

    private PersonalLesson createLesson(StudentProfile profile, LessonSeed seed) {
        PersonalLesson lesson = PersonalLesson.builder()
                .studentProfile(profile)
                .courseId(profile.getCourseId())
                .teacherId(profile.getTeacherId())
                .writerId(profile.getTeacherId())
                .date(seed.date())
                .content(seed.content())
                .build();
        return personalLessonRepository.save(lesson);
    }

    private List<LessonSeed> buildLessonSeeds(StudentProfile profile, LocalDate today) {
        List<LessonSeed> seeds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            LocalDate date = today.minusDays((long) i + 1);
            String content = String.format(
                    "%s 진도 기록 #%d - 단어 테스트 및 복습",
                    profile.getName(),
                    i + 1
            );
            seeds.add(new LessonSeed(date, content));
        }
        return seeds;
    }

    private record LessonSeed(LocalDate date, String content) {
    }
}
