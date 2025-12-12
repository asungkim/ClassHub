package com.classhub.global.init.data;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.model.Member;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.classhub.global.init.BootstrapSeedContext;
import com.classhub.global.init.SeedKeys;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"local", "dev"})
public class CourseInitData extends BaseInitData {

    private final CourseRepository courseRepository;
    private final BootstrapSeedContext seedContext;

    public CourseInitData(CourseRepository courseRepository, BootstrapSeedContext seedContext) {
        super("course-seed", 200);
        this.courseRepository = courseRepository;
        this.seedContext = seedContext;
    }

    @Override
    @Transactional
    protected void doInitialize(boolean force) {
        for (CourseSeed seed : buildSeeds()) {
            Member teacher = seedContext.getRequiredMember(seed.teacherKey());
            Course course = courseRepository.findByNameIgnoreCaseAndTeacherId(seed.name(), teacher.getId())
                    .map(existing -> updateCourse(existing, seed))
                    .orElseGet(() -> createCourse(seed, teacher.getId()));
            seedContext.storeCourse(seed.key(), course);
        }
    }

    private Course updateCourse(Course course, CourseSeed seed) {
        course.update(seed.name(), seed.company(), seed.daysOfWeek(), seed.startTime(), seed.endTime());
        course.activate();
        return course;
    }

    private Course createCourse(CourseSeed seed, java.util.UUID teacherId) {
        Course course = Course.builder()
                .name(seed.name())
                .company(seed.company())
                .teacherId(teacherId)
                .daysOfWeek(seed.daysOfWeek())
                .startTime(seed.startTime())
                .endTime(seed.endTime())
                .build();
        return courseRepository.save(course);
    }

    private List<CourseSeed> buildSeeds() {
        List<CourseSeed> seeds = new ArrayList<>();
        seeds.add(new CourseSeed(SeedKeys.courseKey(SeedKeys.TEACHER_ALPHA, 1), "Alpha Course A", "Alpha Academy", Set.of(DayOfWeek.MONDAY,DayOfWeek.FRIDAY), LocalTime.of(14, 0), LocalTime.of(16, 0), SeedKeys.TEACHER_ALPHA));
        seeds.add(new CourseSeed(SeedKeys.courseKey(SeedKeys.TEACHER_ALPHA, 2), "Alpha Course B", "Alpha Academy", Set.of(DayOfWeek.MONDAY,DayOfWeek.FRIDAY), LocalTime.of(16, 0), LocalTime.of(18, 0), SeedKeys.TEACHER_ALPHA));
        seeds.add(new CourseSeed(SeedKeys.courseKey(SeedKeys.TEACHER_ALPHA, 3), "Alpha Course C", "Alpha Academy", Set.of(DayOfWeek.MONDAY,DayOfWeek.FRIDAY), LocalTime.of(14, 0), LocalTime.of(16, 0), SeedKeys.TEACHER_ALPHA));

        seeds.add(new CourseSeed(SeedKeys.courseKey(SeedKeys.TEACHER_BETA, 1), "Beta Course A", "Beta Institute", Set.of(DayOfWeek.MONDAY,DayOfWeek.FRIDAY), LocalTime.of(15, 0), LocalTime.of(17, 0), SeedKeys.TEACHER_BETA));
        seeds.add(new CourseSeed(SeedKeys.courseKey(SeedKeys.TEACHER_BETA, 2), "Beta Course B", "Beta Institute", Set.of(DayOfWeek.MONDAY,DayOfWeek.FRIDAY), LocalTime.of(17, 0), LocalTime.of(19, 0), SeedKeys.TEACHER_BETA));
        seeds.add(new CourseSeed(SeedKeys.courseKey(SeedKeys.TEACHER_BETA, 3), "Beta Course C", "Beta Institute", Set.of(DayOfWeek.MONDAY,DayOfWeek.FRIDAY), LocalTime.of(10, 0), LocalTime.of(12, 0), SeedKeys.TEACHER_BETA));
        return seeds;
    }

    private record CourseSeed(String key, String name, String company, Set<DayOfWeek> daysOfWeek, LocalTime startTime, LocalTime endTime, String teacherKey) {
    }
}
