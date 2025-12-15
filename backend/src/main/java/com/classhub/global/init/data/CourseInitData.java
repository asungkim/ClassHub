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
                "대치 메가프렙 수학심화반",
                "대치 메가프렙",
                scheduleSet(
                        schedule(DayOfWeek.TUESDAY, 18, 0, 20, 0),
                        schedule(DayOfWeek.THURSDAY, 18, 0, 20, 0)
                ),
                SeedKeys.TEACHER_ALPHA
        ));
        seeds.add(new CourseSeed(
                SeedKeys.courseKey(SeedKeys.TEACHER_ALPHA, 2),
                "잠실 루멘 과학탐구반",
                "루멘프라임 아카데미",
                scheduleSet(
                        schedule(DayOfWeek.WEDNESDAY, 17, 30, 19, 30),
                        schedule(DayOfWeek.SATURDAY, 9, 0, 11, 0)
                ),
                SeedKeys.TEACHER_ALPHA
        ));
        seeds.add(new CourseSeed(
                SeedKeys.courseKey(SeedKeys.TEACHER_ALPHA, 3),
                "서초 로하스 영어토론반",
                "서초 로하스 어학원",
                scheduleSet(
                        schedule(DayOfWeek.TUESDAY, 10, 0, 12, 0),
                        schedule(DayOfWeek.SUNDAY, 13, 0, 15, 0)
                ),
                SeedKeys.TEACHER_ALPHA
        ));

        seeds.add(new CourseSeed(
                SeedKeys.courseKey(SeedKeys.TEACHER_BETA, 1),
                "분당 리더스 영어독해반",
                "분당 리더스 학원",
                scheduleSet(
                        schedule(DayOfWeek.MONDAY, 19, 0, 21, 0),
                        schedule(DayOfWeek.WEDNESDAY, 19, 0, 21, 0)
                ),
                SeedKeys.TEACHER_BETA
        ));
        seeds.add(new CourseSeed(
                SeedKeys.courseKey(SeedKeys.TEACHER_BETA, 2),
                "수원 프라임 물리연구반",
                "수원 프라임 과학관",
                scheduleSet(
                        schedule(DayOfWeek.TUESDAY, 16, 0, 18, 0),
                        schedule(DayOfWeek.THURSDAY, 16, 0, 18, 0)
                ),
                SeedKeys.TEACHER_BETA
        ));
        seeds.add(new CourseSeed(
                SeedKeys.courseKey(SeedKeys.TEACHER_BETA, 3),
                "평촌 비전 수학올림피아드",
                "평촌 비전 아카데미",
                scheduleSet(
                        schedule(DayOfWeek.SATURDAY, 14, 0, 17, 0),
                        schedule(DayOfWeek.SUNDAY, 9, 0, 12, 0)
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
