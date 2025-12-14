package com.classhub.domain.calendar.application;

import com.classhub.domain.calendar.dto.response.CalendarClinicRecordDto;
import com.classhub.domain.calendar.dto.response.CalendarPersonalLessonDto;
import com.classhub.domain.calendar.dto.response.CalendarSharedLessonDto;
import com.classhub.domain.calendar.dto.response.StudentCalendarResponse;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.personallesson.model.PersonalLesson;
import com.classhub.domain.personallesson.repository.PersonalLessonRepository;
import com.classhub.domain.sharedlesson.model.SharedLesson;
import com.classhub.domain.sharedlesson.repository.SharedLessonRepository;
import com.classhub.domain.studentcourseenrollment.repository.StudentCourseEnrollmentRepository;
import com.classhub.domain.studentprofile.model.StudentProfile;
import com.classhub.domain.studentprofile.repository.StudentProfileRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentCalendarQueryService {

    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;

    private final MemberRepository memberRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentCourseEnrollmentRepository studentCourseEnrollmentRepository;
    private final SharedLessonRepository sharedLessonRepository;
    private final PersonalLessonRepository personalLessonRepository;

    public StudentCalendarResponse getMonthlyCalendar(
            UUID requesterId,
            UUID studentProfileId,
            int year,
            int month
    ) {
        // 1. 요일 검증
        validateYearMonth(year, month);

        // 2. Member 검증
        Member requester = memberRepository.findById(requesterId)
                .orElseThrow(RsCode.MEMBER_NOT_FOUND::toException);

        // 3. StudentProfile 검증
        StudentProfile studentProfile = studentProfileRepository.findById(studentProfileId)
                .orElseThrow(RsCode.STUDENT_PROFILE_NOT_FOUND::toException);

        // 4. ROLE 권한 검증
        if (!hasAccess(requester, studentProfile)) {
            throw RsCode.FORBIDDEN.toException();
        }

        // 5. YearMonth 생성
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        // 6. courseId를 통해 SharedLesson 추출
        List<UUID> courseIds = studentCourseEnrollmentRepository.findAllCourseIdsByStudentProfileId(studentProfileId);

        List<SharedLesson> sharedLessons = courseIds.isEmpty()
                ? List.of()
                : sharedLessonRepository.findAllByCourse_IdInAndDateBetweenOrderByDateAsc(courseIds, start, end);

        // 7. studentProfileId를 통해 PersonalLesson 추출
        List<PersonalLesson> personalLessons = personalLessonRepository
                .findAllByStudentProfile_IdAndDateBetweenOrderByDateAsc(studentProfileId, start, end);

        Map<UUID, MemberRole> writerRoles = resolveWriterRoles(sharedLessons, personalLessons);

        List<CalendarSharedLessonDto> sharedLessonDtos = sharedLessons.stream()
                .sorted(Comparator
                        .comparing(SharedLesson::getDate)
                        .thenComparing(lesson -> lesson.getCourse().getName(), String.CASE_INSENSITIVE_ORDER))
                .map(lesson -> CalendarSharedLessonDto.from(
                        lesson,
                        writerRoles.getOrDefault(lesson.getWriterId(), MemberRole.TEACHER)
                ))
                .toList();

        List<CalendarPersonalLessonDto> personalLessonDtos = personalLessons.stream()
                .sorted(Comparator
                        .comparing(PersonalLesson::getDate)
                        .thenComparing(PersonalLesson::getCreatedAt))
                .map(lesson -> CalendarPersonalLessonDto.from(
                        lesson,
                        writerRoles.getOrDefault(lesson.getWriterId(), MemberRole.TEACHER)
                ))
                .toList();

        return StudentCalendarResponse.of(
                studentProfileId,
                year,
                month,
                sharedLessonDtos,
                personalLessonDtos,
                List.<CalendarClinicRecordDto>of()
        );
    }

    private Map<UUID, MemberRole> resolveWriterRoles(
            List<SharedLesson> sharedLessons,
            List<PersonalLesson> personalLessons
    ) {
        Set<UUID> writerIds = new HashSet<>();
        sharedLessons.forEach(lesson -> writerIds.add(lesson.getWriterId()));
        personalLessons.forEach(lesson -> writerIds.add(lesson.getWriterId()));

        if (writerIds.isEmpty()) {
            return Map.of();
        }

        return memberRepository.findAllById(writerIds).stream()
                .collect(Collectors.toMap(Member::getId, Member::getRole, (existing, ignored) -> existing));
    }

    private boolean hasAccess(Member requester, StudentProfile studentProfile) {
        return switch (requester.getRole()) {
            case TEACHER -> studentProfile.getTeacherId().equals(requester.getId());
            case ASSISTANT -> requester.getTeacherId() != null
                    && requester.getTeacherId().equals(studentProfile.getTeacherId());
            case SUPERADMIN -> true;
            default -> false;
        };
    }

    private void validateYearMonth(int year, int month) {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw RsCode.BAD_REQUEST.toException();
        }
        if (month < 1 || month > 12) {
            throw RsCode.BAD_REQUEST.toException();
        }
    }
}
