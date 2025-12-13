package com.classhub.global.init.data;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.sharedlesson.model.SharedLesson;
import com.classhub.domain.sharedlesson.repository.SharedLessonRepository;
import com.classhub.global.init.BootstrapSeedContext;
import com.classhub.global.init.SeedKeys;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"local", "dev"})
public class SharedLessonInitData extends BaseInitData {

    private final SharedLessonRepository sharedLessonRepository;
    private final BootstrapSeedContext seedContext;

    public SharedLessonInitData(
            SharedLessonRepository sharedLessonRepository,
            BootstrapSeedContext seedContext
    ) {
        super("shared-lesson-seed", 520);
        this.sharedLessonRepository = sharedLessonRepository;
        this.seedContext = seedContext;
    }

    @Override
    @Transactional
    protected void doInitialize(boolean force) {
        for (SharedLessonSeed seed : buildSeeds()) {
            upsertSharedLesson(seed);
        }
    }

    private void upsertSharedLesson(SharedLessonSeed seed) {
        Course course = seedContext.getRequiredCourse(seed.courseKey());
        sharedLessonRepository.findByCourse_IdAndDateAndTitleIgnoreCase(course.getId(), seed.date(), seed.title())
                .map(existing -> updateLesson(existing, seed))
                .orElseGet(() -> createLesson(course, seed));
    }

    private SharedLesson updateLesson(SharedLesson lesson, SharedLessonSeed seed) {
        lesson.update(seed.date(), seed.title(), seed.content());
        return lesson;
    }

    private SharedLesson createLesson(Course course, SharedLessonSeed seed) {
        SharedLesson sharedLesson = SharedLesson.builder()
                .course(course)
                .writerId(course.getTeacherId())
                .date(seed.date())
                .title(seed.title())
                .content(seed.content())
                .build();
        return sharedLessonRepository.save(sharedLesson);
    }

    private List<SharedLessonSeed> buildSeeds() {
        List<SharedLessonSeed> seeds = new ArrayList<>();
        seeds.addAll(buildSeedsForTeacher(SeedKeys.TEACHER_ALPHA, "Alpha"));
        seeds.addAll(buildSeedsForTeacher(SeedKeys.TEACHER_BETA, "Beta"));
        return seeds;
    }

    private List<SharedLessonSeed> buildSeedsForTeacher(String teacherKey, String label) {
        List<SharedLessonSeed> seeds = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int courseIndex = 1; courseIndex <= 3; courseIndex++) {
            String courseKey = SeedKeys.courseKey(teacherKey, courseIndex);
            for (int weekOffset = 0; weekOffset < 3; weekOffset++) {
                LocalDate date = today.minusWeeks(weekOffset).minusDays(courseIndex);
                String title = String.format("%s Course %d 주간 진도 #%d", label, courseIndex, weekOffset + 1);
                String content = String.format(
                        "%s 반 %d차 수업 정리 - 핵심 개념 복습과 퀴즈 피드백을 정리했습니다.",
                        label,
                        weekOffset + 1
                );
                seeds.add(new SharedLessonSeed(courseKey, date, title, content));
            }
        }
        return seeds;
    }

    private record SharedLessonSeed(String courseKey, LocalDate date, String title, String content) {
    }
}
