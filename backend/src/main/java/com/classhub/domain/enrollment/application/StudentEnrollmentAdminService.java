package com.classhub.domain.enrollment.application;

import com.classhub.domain.course.application.CourseViewAssembler;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.enrollment.dto.response.TeacherEnrollmentRequestResponse;
import com.classhub.domain.enrollment.model.EnrollmentStatus;
import com.classhub.domain.enrollment.model.StudentEnrollmentRequest;
import com.classhub.domain.enrollment.repository.StudentEnrollmentRequestRepository;
import com.classhub.domain.member.dto.response.StudentSummaryResponse;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.StudentInfo;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.member.repository.StudentInfoRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.util.KstTime;
import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentEnrollmentAdminService {

    private final StudentEnrollmentRequestRepository requestRepository;
    private final CourseRepository courseRepository;
    private final CourseViewAssembler courseViewAssembler;
    private final MemberRepository memberRepository;
    private final StudentInfoRepository studentInfoRepository;

    public PageResponse<TeacherEnrollmentRequestResponse> getRequests(UUID teacherId,
                                                                      UUID courseId,
                                                                      Set<EnrollmentStatus> statuses,
                                                                      String studentName,
                                                                      int page,
                                                                      int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<StudentEnrollmentRequest> requestPage = requestRepository.searchRequestsForAdmin(
                teacherId,
                courseId,
                resolveStatuses(statuses),
                normalizeKeyword(studentName),
                pageable
        );
        if (requestPage.isEmpty()) {
            return PageResponse.from(new PageImpl<>(List.of(), pageable, requestPage.getTotalElements()));
        }
        Map<UUID, CourseResponse> courseMap = buildCourseResponseMap(requestPage.getContent());
        Map<UUID, StudentSummaryResponse> studentMap = buildStudentSummaryMap(requestPage.getContent());
        List<TeacherEnrollmentRequestResponse> content = requestPage.getContent().stream()
                .map(req -> toResponse(req, courseMap, studentMap))
                .toList();
        Page<TeacherEnrollmentRequestResponse> dtoPage = new PageImpl<>(
                content,
                pageable,
                requestPage.getTotalElements()
        );
        return PageResponse.from(dtoPage);
    }

    private Set<EnrollmentStatus> resolveStatuses(Set<EnrollmentStatus> statuses) {
        return (statuses == null || statuses.isEmpty())
                ? EnumSet.allOf(EnrollmentStatus.class)
                : EnumSet.copyOf(statuses);
    }

    private Map<UUID, CourseResponse> buildCourseResponseMap(Collection<StudentEnrollmentRequest> requests) {
        List<UUID> courseIds = requests.stream()
                .map(StudentEnrollmentRequest::getCourseId)
                .distinct()
                .toList();
        List<Course> courses = courseRepository.findAllById(courseIds);
        CourseViewAssembler.CourseContext context = courseViewAssembler.buildContext(courses);
        return courses.stream()
                .collect(Collectors.toMap(
                        Course::getId,
                        course -> courseViewAssembler.toCourseResponse(course, context)
                ));
    }

    private Map<UUID, StudentSummaryResponse> buildStudentSummaryMap(Collection<StudentEnrollmentRequest> requests) {
        List<UUID> studentIds = requests.stream()
                .map(StudentEnrollmentRequest::getStudentMemberId)
                .distinct()
                .toList();
        Map<UUID, Member> memberMap = memberRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(Member::getId, member -> member));
        Map<UUID, StudentInfo> infoMap = studentInfoRepository.findByMemberIdIn(studentIds).stream()
                .collect(Collectors.toMap(StudentInfo::getMemberId, info -> info));
        return studentIds.stream()
                .collect(Collectors.toMap(
                        studentId -> studentId,
                        studentId -> toStudentSummary(memberMap.get(studentId), infoMap.get(studentId))
                ));
    }

    private TeacherEnrollmentRequestResponse toResponse(StudentEnrollmentRequest request,
                                                        Map<UUID, CourseResponse> courseMap,
                                                        Map<UUID, StudentSummaryResponse> studentMap) {
        CourseResponse courseResponse = courseMap.getOrDefault(request.getCourseId(), fallbackCourseResponse(request));
        StudentSummaryResponse summary = studentMap.get(request.getStudentMemberId());
        return new TeacherEnrollmentRequestResponse(
                request.getId(),
                courseResponse,
                summary,
                request.getStatus(),
                request.getMessage(),
                request.getProcessedAt(),
                request.getProcessedByMemberId(),
                request.getCreatedAt()
        );
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private StudentSummaryResponse toStudentSummary(Member member, StudentInfo info) {
        if (member == null) {
            return StudentSummaryResponse.builder()
                    .memberId(null)
                    .name("알 수 없음")
                    .email(null)
                    .phoneNumber(null)
                    .schoolName(info == null ? null : info.getSchoolName())
                    .grade(info == null || info.getGrade() == null ? null : info.getGrade().name())
                    .birthDate(info == null ? null : info.getBirthDate())
                    .age(calculateAge(info == null ? null : info.getBirthDate()))
                    .parentPhone(info == null ? null : info.getParentPhone())
                    .build();
        }
        return StudentSummaryResponse.builder()
                .memberId(member.getId())
                .name(member.getName())
                .email(member.getEmail())
                .phoneNumber(member.getPhoneNumber())
                .schoolName(info == null ? null : info.getSchoolName())
                .grade(info == null || info.getGrade() == null ? null : info.getGrade().name())
                .birthDate(info == null ? null : info.getBirthDate())
                .age(calculateAge(info == null ? null : info.getBirthDate()))
                .parentPhone(info == null ? null : info.getParentPhone())
                .build();
    }

    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now(KstTime.clock())).getYears();
    }

    private CourseResponse fallbackCourseResponse(StudentEnrollmentRequest request) {
        return new CourseResponse(
                request.getCourseId(),
                null,
                null,
                null,
                null,
                "삭제된 Course",
                "원본 Course 정보를 찾을 수 없습니다.",
                null,
                null,
                false,
                List.of()
        );
    }
}
