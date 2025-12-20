package com.classhub.domain.course.application;

import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.dto.response.CourseScheduleResponse;
import com.classhub.domain.course.dto.response.PublicCourseResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicCourseService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Map<DayOfWeek, String> DAY_LABELS = buildDayLabels();

    private final CourseRepository courseRepository;
    private final MemberRepository memberRepository;
    private final CourseViewAssembler courseViewAssembler;

    public PageResponse<PublicCourseResponse> searchCourses(UUID companyId,
                                                            UUID branchId,
                                                            UUID teacherId,
                                                            String keyword,
                                                            boolean onlyVerified,
                                                            int page,
                                                            int size) {
        Page<Course> courses = courseRepository.searchPublicCourses(
                companyId,
                branchId,
                teacherId,
                normalizeKeyword(keyword),
                onlyVerified,
                PageRequest.of(page, size)
        );
        CourseViewAssembler.CourseContext context = courseViewAssembler.buildContext(courses.getContent());
        Map<UUID, Member> teacherMap = loadTeachers(courses.getContent());

        Page<PublicCourseResponse> mapped = courses.map(course -> {
            CourseResponse base = courseViewAssembler.toCourseResponse(course, context);
            Member teacher = teacherMap.get(course.getTeacherMemberId());
            if (teacher == null) {
                throw new BusinessException(RsCode.MEMBER_NOT_FOUND);
            }
            String scheduleSummary = buildScheduleSummary(base.schedules());
            return PublicCourseResponse.from(base, teacher.getId(), teacher.getName(), scheduleSummary);
        });
        return PageResponse.from(mapped);
    }

    private static Map<DayOfWeek, String> buildDayLabels() {
        Map<DayOfWeek, String> map = new EnumMap<>(DayOfWeek.class);
        map.put(DayOfWeek.MONDAY, "월");
        map.put(DayOfWeek.TUESDAY, "화");
        map.put(DayOfWeek.WEDNESDAY, "수");
        map.put(DayOfWeek.THURSDAY, "목");
        map.put(DayOfWeek.FRIDAY, "금");
        map.put(DayOfWeek.SATURDAY, "토");
        map.put(DayOfWeek.SUNDAY, "일");
        return map;
    }

    private Map<UUID, Member> loadTeachers(Collection<Course> courses) {
        List<UUID> teacherIds = courses.stream()
                .map(Course::getTeacherMemberId)
                .distinct()
                .toList();
        if (teacherIds.isEmpty()) {
            return Map.of();
        }
        List<Member> members = memberRepository.findAllById(teacherIds);
        if (members.size() < teacherIds.size()) {
            throw new BusinessException(RsCode.MEMBER_NOT_FOUND);
        }
        return members.stream()
                .collect(Collectors.toMap(Member::getId, member -> member));
    }

    private String buildScheduleSummary(List<CourseScheduleResponse> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return "";
        }
        return schedules.stream()
                .map(schedule -> DAY_LABELS.getOrDefault(schedule.dayOfWeek(), schedule.dayOfWeek().name().substring(0, 3))
                        + " "
                        + schedule.startTime().format(TIME_FORMATTER)
                        + "~"
                        + schedule.endTime().format(TIME_FORMATTER))
                .collect(Collectors.joining(", "));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
