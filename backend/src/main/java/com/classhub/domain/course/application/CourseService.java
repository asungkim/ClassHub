package com.classhub.domain.course.application;

import com.classhub.domain.course.dto.request.CourseCreateRequest;
import com.classhub.domain.course.dto.request.CourseScheduleRequest;
import com.classhub.domain.course.dto.request.CourseUpdateRequest;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.model.CourseSchedule;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public CourseResponse createCourse(UUID teacherId, CourseCreateRequest request) {

        validateTeacher(teacherId);
        Set<CourseSchedule> schedules = toSchedulesWithValidation(request.schedules());

        Course course = Course.builder()
                .name(request.name())
                .company(request.company())
                .teacherId(teacherId)
                .schedules(schedules)
                .build();

        Course saved = courseRepository.save(course);
        return CourseResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getCoursesByTeacher(UUID teacherId, Boolean isActive) {

        validateTeacher(teacherId);

        List<Course> courses = isActive != null
                ? courseRepository.findByTeacherIdAndActive(teacherId, isActive)
                : courseRepository.findByTeacherId(teacherId);

        return courses.stream()
                .map(CourseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourseById(UUID courseId, UUID teacherId) {

        validateTeacher(teacherId);

        Course course = getCourseOwnedByTeacher(courseId, teacherId);

        return CourseResponse.from(course);
    }

    @Transactional
    public CourseResponse updateCourse(UUID courseId, UUID teacherId, CourseUpdateRequest request) {

        validateTeacher(teacherId);

        Course course = getCourseOwnedByTeacher(courseId, teacherId);
        Set<CourseSchedule> schedules = request.schedules() != null
                ? toSchedulesWithValidation(request.schedules())
                : course.getSchedules();

        course.update(
                request.name(),
                request.company(),
                schedules
        );

        return CourseResponse.from(course);
    }

    @Transactional
    public void deactivateCourse(UUID courseId, UUID teacherId) {

        validateTeacher(teacherId);

        Course course = getCourseOwnedByTeacher(courseId, teacherId);

        course.deactivate();
    }

    @Transactional
    public void activateCourse(UUID courseId, UUID teacherId) {

        validateTeacher(teacherId);

        Course course = getCourseOwnedByTeacher(courseId, teacherId);

        course.activate();
    }

    private void validateTeacher(UUID teacherId) {
        Member member = memberRepository.findById(teacherId)
                .orElseThrow(() -> new BusinessException(RsCode.MEMBER_NOT_FOUND));

        if (member.getRole() != MemberRole.TEACHER) {
            throw new BusinessException(RsCode.COURSE_FORBIDDEN);
        }
    }

    private Course getCourseOwnedByTeacher(UUID courseId, UUID teacherId) {
        return courseRepository.findByIdAndTeacherId(courseId, teacherId)
                .orElseThrow(() -> new BusinessException(RsCode.COURSE_NOT_FOUND));
    }

    private Set<CourseSchedule> toSchedulesWithValidation(Set<CourseScheduleRequest> scheduleRequests) {
        if (scheduleRequests == null || scheduleRequests.isEmpty()) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        Set<DayOfWeek> seenDays = new java.util.HashSet<>();
        for (CourseScheduleRequest schedule : scheduleRequests) {
            if (schedule.dayOfWeek() == null
                    || schedule.startTime() == null
                    || schedule.endTime() == null
                    || !schedule.startTime().isBefore(schedule.endTime())) {
                throw new BusinessException(RsCode.BAD_REQUEST);
            }
            if (!seenDays.add(schedule.dayOfWeek())) {
                throw new BusinessException(RsCode.BAD_REQUEST);
            }
        }
        return scheduleRequests.stream()
                .map(req -> new CourseSchedule(req.dayOfWeek(), req.startTime(), req.endTime()))
                .collect(Collectors.toSet());
    }
}
