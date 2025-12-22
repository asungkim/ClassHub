package com.classhub.domain.calendar.application;

import com.classhub.domain.clinic.attendance.repository.ClinicAttendanceEventProjection;
import com.classhub.domain.clinic.attendance.repository.ClinicAttendanceRepository;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.progress.course.model.CourseProgress;
import com.classhub.domain.progress.course.repository.CourseProgressRepository;
import com.classhub.domain.calendar.dto.StudentCalendarResponse;
import com.classhub.domain.calendar.mapper.StudentCalendarMapper;
import com.classhub.domain.progress.personal.model.PersonalProgress;
import com.classhub.domain.progress.personal.repository.PersonalProgressRepository;
import com.classhub.domain.progress.support.ProgressPermissionValidator;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentCalendarService {

    private static final int MAX_EVENT_COUNT = 500;
    private static final int ALLOWED_MONTH_GAP = 3;

    private final ProgressPermissionValidator permissionValidator;
    private final CourseProgressRepository courseProgressRepository;
    private final PersonalProgressRepository personalProgressRepository;
    private final ClinicAttendanceRepository clinicAttendanceRepository;
    private final CourseRepository courseRepository;
    private final MemberRepository memberRepository;
    private final StudentCalendarMapper studentCalendarMapper;

    public StudentCalendarResponse getStudentCalendar(MemberPrincipal principal,
                                                      UUID studentId,
                                                      int year,
                                                      int month) {
        YearMonth targetMonth = resolveYearMonth(year, month);
        validateMonthRange(targetMonth);
        List<StudentCourseRecord> records = permissionValidator.ensureCalendarAccess(principal, studentId);
        Set<UUID> courseIds = records.stream()
                .map(StudentCourseRecord::getCourseId)
                .collect(Collectors.toSet());
        Map<UUID, UUID> recordCourseMap = records.stream()
                .collect(Collectors.toMap(StudentCourseRecord::getId, StudentCourseRecord::getCourseId));
        List<UUID> recordIds = records.stream()
                .map(StudentCourseRecord::getId)
                .toList();

        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();

        List<CourseProgress> courseProgresses = courseIds.isEmpty()
                ? List.of()
                : courseProgressRepository.findByCourseIdsAndDateRange(courseIds.stream().toList(), startDate, endDate);
        List<PersonalProgress> personalProgresses = recordIds.isEmpty()
                ? List.of()
                : personalProgressRepository.findByRecordIdsAndDateRange(recordIds, startDate, endDate);
        List<ClinicAttendanceEventProjection> clinicEvents = recordIds.isEmpty()
                ? List.of()
                : clinicAttendanceRepository.findEventsByRecordIdsAndDateRange(recordIds, startDate, endDate);

        enforceEventLimit(courseProgresses.size() + personalProgresses.size() + clinicEvents.size());

        Map<UUID, Course> courseMap = loadCourses(courseIds);
        Map<UUID, MemberRole> roleMap = loadWriterRoles(courseProgresses, personalProgresses, clinicEvents);

        List<StudentCalendarResponse.CourseProgressEvent> courseProgressEvents =
                studentCalendarMapper.toCourseProgressEvents(courseProgresses, courseMap, roleMap);
        List<StudentCalendarResponse.PersonalProgressEvent> personalProgressEvents =
                studentCalendarMapper.toPersonalProgressEvents(personalProgresses, recordCourseMap, courseMap, roleMap);
        List<StudentCalendarResponse.ClinicEvent> clinicEventResponses =
                studentCalendarMapper.toClinicEvents(clinicEvents, roleMap);

        return new StudentCalendarResponse(
                studentId,
                targetMonth.getYear(),
                targetMonth.getMonthValue(),
                courseProgressEvents,
                personalProgressEvents,
                clinicEventResponses
        );
    }

    private YearMonth resolveYearMonth(int year, int month) {
        try {
            return YearMonth.of(year, month);
        } catch (RuntimeException ex) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
    }

    private void validateMonthRange(YearMonth targetMonth) {
        YearMonth current = YearMonth.now();
        long diff = ChronoUnit.MONTHS.between(current, targetMonth);
        if (diff > ALLOWED_MONTH_GAP || diff < -ALLOWED_MONTH_GAP) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
    }

    private void enforceEventLimit(int totalCount) {
        if (totalCount > MAX_EVENT_COUNT) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
    }

    private Map<UUID, Course> loadCourses(Set<UUID> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        List<Course> courses = courseRepository.findAllById(courseIds);
        Map<UUID, Course> courseMap = courses.stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));
        if (courseMap.size() != courseIds.size()) {
            throw new BusinessException(RsCode.INTERNAL_SERVER);
        }
        return courseMap;
    }

    private Map<UUID, MemberRole> loadWriterRoles(Collection<CourseProgress> courseProgresses,
                                                  Collection<PersonalProgress> personalProgresses,
                                                  Collection<ClinicAttendanceEventProjection> clinicEvents) {
        Set<UUID> writerIds = courseProgresses.stream()
                .map(CourseProgress::getWriterId)
                .collect(Collectors.toSet());
        writerIds.addAll(personalProgresses.stream()
                .map(PersonalProgress::getWriterId)
                .toList());
        writerIds.addAll(clinicEvents.stream()
                .map(ClinicAttendanceEventProjection::getRecordWriterId)
                .filter(id -> id != null)
                .toList());
        if (writerIds.isEmpty()) {
            return Map.of();
        }
        List<Member> members = memberRepository.findAllById(writerIds);
        Map<UUID, MemberRole> roleMap = members.stream()
                .collect(Collectors.toMap(Member::getId, Member::getRole));
        if (roleMap.size() != writerIds.size()) {
            throw new BusinessException(RsCode.INTERNAL_SERVER);
        }
        return roleMap;
    }
}
