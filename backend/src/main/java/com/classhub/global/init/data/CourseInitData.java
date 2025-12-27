package com.classhub.global.init.data;

import com.classhub.domain.assignment.model.TeacherBranchAssignment;
import com.classhub.domain.assignment.repository.TeacherBranchAssignmentRepository;
import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.branch.repository.BranchRepository;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.repository.MemberRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile({"local", "test"})
public class CourseInitData extends BaseInitData {

    private static final LocalDate START_DATE = LocalDate.of(2025, 1, 1);
    private static final LocalDate END_DATE = LocalDate.of(2025, 12, 31);
    private static final List<TimeRange> COURSE_TIME_SLOTS = List.of(
            new TimeRange(LocalTime.of(14, 0), LocalTime.of(16, 0)),
            new TimeRange(LocalTime.of(16, 0), LocalTime.of(18, 0)),
            new TimeRange(LocalTime.of(18, 0), LocalTime.of(20, 0)),
            new TimeRange(LocalTime.of(20, 0), LocalTime.of(22, 0))
    );

    private static final Map<String, List<DayOfWeek>> TEACHER_DAYS = Map.of(
            "te1@n.com", List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
            "te2@n.com", List.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)
    );

    private final CourseRepository courseRepository;
    private final TeacherBranchAssignmentRepository assignmentRepository;
    private final MemberRepository memberRepository;
    private final BranchRepository branchRepository;

    public CourseInitData(CourseRepository courseRepository,
                          TeacherBranchAssignmentRepository assignmentRepository,
                          MemberRepository memberRepository,
                          BranchRepository branchRepository) {
        super("season2-course-seed", 70);
        this.courseRepository = courseRepository;
        this.assignmentRepository = assignmentRepository;
        this.memberRepository = memberRepository;
        this.branchRepository = branchRepository;
    }

    @Override
    @Transactional
    protected void doInitialize(boolean force) {
        for (Map.Entry<String, List<DayOfWeek>> entry : TEACHER_DAYS.entrySet()) {
            String email = entry.getKey();
            Optional<Member> teacher = memberRepository.findByEmail(email);
            if (teacher.isEmpty()) {
                log.warn("Skipping course seed. Teacher not found for email={}", email);
                continue;
            }
            List<TeacherBranchAssignment> assignments = assignmentRepository
                    .findByTeacherMemberIdAndDeletedAtIsNull(teacher.get().getId(), org.springframework.data.domain.Pageable.unpaged())
                    .getContent();
            int courseIndex = 0;
            for (TeacherBranchAssignment assignment : assignments) {
                Optional<Branch> branch = branchRepository.findById(assignment.getBranchId());
                String branchName = branch.map(Branch::getName).orElse("반");
                TimeRange firstSlot = resolveTimeRange(courseIndex++);
                TimeRange secondSlot = resolveTimeRange(courseIndex++);
                createOrUpdateCourse(teacher.get().getId(), assignment.getBranchId(), branchName + " A반", entry.getValue(), firstSlot, force);
                createOrUpdateCourse(teacher.get().getId(), assignment.getBranchId(), branchName + " B반", entry.getValue(), secondSlot, force);
            }
        }
    }

    private void createOrUpdateCourse(UUID teacherId,
                                      UUID branchId,
                                      String name,
                                      List<DayOfWeek> days,
                                      TimeRange timeRange,
                                      boolean force) {
        Optional<Course> existing = courseRepository.findByTeacherMemberIdAndBranchIdAndName(teacherId, branchId, name);
        Set<Course.CourseSchedule> schedules = buildSchedules(days, timeRange);
        if (existing.isPresent()) {
            if (force) {
                Course course = existing.get();
                course.updateInfo(name, START_DATE, END_DATE);
                course.replaceSchedules(schedules);
                course.activate();
            }
            return;
        }

        Course course = Course.create(
                branchId,
                teacherId,
                name,
                "테스트 반 커리큘럼",
                START_DATE,
                END_DATE,
                schedules
        );
        courseRepository.save(course);
    }

    private Set<Course.CourseSchedule> buildSchedules(List<DayOfWeek> days, TimeRange timeRange) {
        return Set.of(
                new Course.CourseSchedule(days.get(0), timeRange.start, timeRange.end),
                new Course.CourseSchedule(days.get(1), timeRange.start, timeRange.end)
        );
    }

    private TimeRange resolveTimeRange(int index) {
        if (COURSE_TIME_SLOTS.isEmpty()) {
            return new TimeRange(LocalTime.of(19, 0), LocalTime.of(21, 0));
        }
        return COURSE_TIME_SLOTS.get(index % COURSE_TIME_SLOTS.size());
    }

    private record TimeRange(LocalTime start, LocalTime end) {
    }
}
