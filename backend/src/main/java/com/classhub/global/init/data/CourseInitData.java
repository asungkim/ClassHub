package com.classhub.global.init.data;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.model.CourseSchedule;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.model.Member;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
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
        course.update(seed.name(), seed.company(), seed.schedules());
        course.activate();
        return course;
    }

    private Course createCourse(CourseSeed seed, java.util.UUID teacherId) {
        Course course = Course.builder()
                .name(seed.name())
                .company(seed.company())
                .teacherId(teacherId)
                .schedules(seed.schedules())
                .build();
        return courseRepository.save(course);
    }

    private List<CourseSeed> buildSeeds() {
        List<CourseSeed> seeds = new ArrayList<>();
        seeds.add(new CourseSeed(
                SeedKeys.courseKey(SeedKeys.TEACHER_ALPHA, 1),
                "Alpha Course A",
                "Alpha Academy",
                scheduleSet(
                        schedule(DayOfWeek.MONDAY, 14, 0, 16, 0),
                        schedule(DayOfWeek.FRIDAY, 14, 0, 16, 0)
                ),
                SeedKeys.TEACHER_ALPHA
        ));
        seeds.add(new CourseSeed(
                SeedKeys.courseKey(SeedKeys.TEACHER_ALPHA, 2),
                "Alpha Course B",
                "Alpha Academy",
                scheduleSet(
                        schedule(DayOfWeek.MONDAY, 16, 0, 18, 0),
                        schedule(DayOfWeek.FRIDAY, 16, 0, 18, 0)
                ),
                SeedKeys.TEACHER_ALPHA
        ));
        seeds.add(new CourseSeed(
                SeedKeys.courseKey(SeedKeys.TEACHER_ALPHA, 3),
                "Alpha Course C",
                "Alpha Academy",
                scheduleSet(
                        schedule(DayOfWeek.WEDNESDAY, 10, 0, 12, 0),
                        schedule(DayOfWeek.SATURDAY, 16, 0, 18, 0)
                ),
                SeedKeys.TEACHER_ALPHA
        ));

        seeds.add(new CourseSeed(
                SeedKeys.courseKey(SeedKeys.TEACHER_BETA, 1),
                "Beta Course A",
                "Beta Institute",
                scheduleSet(
                        schedule(DayOfWeek.MONDAY, 15, 0, 17, 0),
                        schedule(DayOfWeek.FRIDAY, 15, 0, 17, 0)
                ),
                SeedKeys.TEACHER_BETA
        ));
        seeds.add(new CourseSeed(
                SeedKeys.courseKey(SeedKeys.TEACHER_BETA, 2),
                "Beta Course B",
                "Beta Institute",
                scheduleSet(
                        schedule(DayOfWeek.MONDAY, 17, 0, 19, 0),
                        schedule(DayOfWeek.FRIDAY, 17, 0, 19, 0)
                ),
                SeedKeys.TEACHER_BETA
        ));
        seeds.add(new CourseSeed(
                SeedKeys.courseKey(SeedKeys.TEACHER_BETA, 3),
                "Beta Course C",
                "Beta Institute",
                scheduleSet(
                        schedule(DayOfWeek.MONDAY, 10, 0, 12, 0),
                        schedule(DayOfWeek.SATURDAY, 10, 0, 12, 0)
                ),
                SeedKeys.TEACHER_BETA
        ));
        return seeds;
    }

    private Set<CourseSchedule> scheduleSet(CourseSchedule... schedules) {
        return new HashSet<>(Arrays.asList(schedules));
    }

    private CourseSchedule schedule(DayOfWeek day, int startHour, int startMinute, int endHour, int endMinute) {
        return new CourseSchedule(day, LocalTime.of(startHour, startMinute), LocalTime.of(endHour, endMinute));
    }

    private record CourseSeed(String key, String name, String company, Set<CourseSchedule> schedules, String teacherKey) {
    }
}
