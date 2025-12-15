package com.classhub.global.init.data;

import com.classhub.domain.personallesson.model.PersonalLesson;
import com.classhub.domain.personallesson.repository.PersonalLessonRepository;
import com.classhub.global.init.BootstrapSeedContext;
import com.classhub.domain.studentprofile.model.StudentProfile;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"local", "dev"})
public class PersonalLessonInitData extends BaseInitData {

    private static final LocalDate[] PERSONAL_LESSON_BASE_DATES = {
            LocalDate.of(2025, 12, 6),
            LocalDate.of(2025, 12, 13),
            LocalDate.of(2025, 11, 23),
            LocalDate.of(2025, 11, 9),
            LocalDate.of(2025, 10, 26),
            LocalDate.of(2025, 10, 12)
    };
    private static final String[] PERSONAL_CONTENT_TEMPLATES = {
            "심화 문제 풀이 복습",
            "약점 보완 코칭",
            "첨삭 피드백 및 오답 정리",
            "모의고사 해설 및 목표 재설정"
    };
    private static final DateTimeFormatter LESSON_DATE_FORMAT = DateTimeFormatter.ofPattern("M월 d일");

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
        for (StudentProfile profile : profiles) {
            seedLessonsForProfile(profile);
        }
    }

    private void seedLessonsForProfile(StudentProfile profile) {
        List<LessonSeed> seeds = buildLessonSeeds(profile);
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
                .teacherId(profile.getTeacherId())
                .writerId(profile.getTeacherId())
                .date(seed.date())
                .content(seed.content())
                .build();
        return personalLessonRepository.save(lesson);
    }

    private List<LessonSeed> buildLessonSeeds(StudentProfile profile) {
        List<LessonSeed> seeds = new ArrayList<>();
        int offset = Math.abs(profile.getName().hashCode()) % 3;
        for (int i = 0; i < PERSONAL_LESSON_BASE_DATES.length; i++) {
            LocalDate date = PERSONAL_LESSON_BASE_DATES[i].plusDays(offset);
            String template = PERSONAL_CONTENT_TEMPLATES[(i + offset) % PERSONAL_CONTENT_TEMPLATES.length];
            String content = String.format(
                    "%s %s 학습 코칭 - %s",
                    profile.getName(),
                    date.format(LESSON_DATE_FORMAT),
                    template
            );
            seeds.add(new LessonSeed(date, content));
        }
        return seeds;
    }

    private record LessonSeed(LocalDate date, String content) {
    }
}
