package com.classhub.global.init.data;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.sharedlesson.model.SharedLesson;
import com.classhub.domain.sharedlesson.repository.SharedLessonRepository;
import com.classhub.global.init.BootstrapSeedContext;
import com.classhub.global.init.SeedKeys;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"local", "dev"})
public class SharedLessonInitData extends BaseInitData {

    private static final int BASE_YEAR = 2025;
    private static final int[] LESSON_MONTHS = {12, 11, 10};
    private static final String[] SHARED_CONTENT_TEMPLATES = {
            "핵심 개념 점검과 서술형 첨삭",
            "모의고사 분석 및 오답 클리닉",
            "심화 문제 풀이와 개별 질의 응답",
            "실전 대비 프로젝트 피드백"
    };

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
        DayOfWeek[][] courseDays = resolveCourseDays(teacherKey);
        String regionLabel = resolveRegionLabel(label);
        for (int courseIndex = 1; courseIndex <= courseDays.length; courseIndex++) {
            String courseKey = SeedKeys.courseKey(teacherKey, courseIndex);
            DayOfWeek[] targetDays = courseDays[courseIndex - 1];
            for (int lessonMonth : LESSON_MONTHS) {
                for (int occurrence = 0; occurrence < targetDays.length; occurrence++) {
                    LocalDate date = alignToMonth(lessonMonth, targetDays[occurrence], courseIndex, occurrence);
                    String title = String.format(
                            "%s %d반 %d월 %d주차 진도",
                            regionLabel,
                            courseIndex,
                            lessonMonth,
                            occurrence + 1
                    );
                    String content = String.format(
                            "%d월 %d주차 %s 수업 - %s",
                            lessonMonth,
                            occurrence + 1,
                            dayToKorean(targetDays[occurrence]),
                            SHARED_CONTENT_TEMPLATES[(courseIndex + occurrence) % SHARED_CONTENT_TEMPLATES.length]
                    );
                    seeds.add(new SharedLessonSeed(courseKey, date, title, content));
                }
            }
        }
        return seeds;
    }

    private DayOfWeek[][] resolveCourseDays(String teacherKey) {
        if (SeedKeys.TEACHER_ALPHA.equals(teacherKey)) {
            return new DayOfWeek[][]{
                    {DayOfWeek.TUESDAY, DayOfWeek.THURSDAY},
                    {DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY},
                    {DayOfWeek.TUESDAY, DayOfWeek.SUNDAY}
            };
        }
        return new DayOfWeek[][]{
                {DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY},
                {DayOfWeek.TUESDAY, DayOfWeek.THURSDAY},
                {DayOfWeek.SATURDAY, DayOfWeek.SUNDAY}
        };
    }

    private String resolveRegionLabel(String label) {
        return "Alpha".equals(label) ? "대치" : "분당";
    }

    private LocalDate alignToMonth(int month, DayOfWeek target, int courseIndex, int occurrence) {
        LocalDate firstDay = LocalDate.of(BASE_YEAR, month, 1);
        LocalDate firstTarget = firstDay.with(TemporalAdjusters.nextOrSame(target));
        return firstTarget.plusWeeks(courseIndex - 1L + occurrence);
    }

    private String dayToKorean(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "월요일";
            case TUESDAY -> "화요일";
            case WEDNESDAY -> "수요일";
            case THURSDAY -> "목요일";
            case FRIDAY -> "금요일";
            case SATURDAY -> "토요일";
            case SUNDAY -> "일요일";
        };
    }

    private record SharedLessonSeed(String courseKey, LocalDate date, String title, String content) {
    }
}
