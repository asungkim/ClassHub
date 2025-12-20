package com.classhub.domain.enrollment.application;

import com.classhub.domain.course.application.CourseViewAssembler;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.enrollment.dto.response.TeacherEnrollmentRequestResponse;
import com.classhub.domain.enrollment.dto.response.TeacherEnrollmentRequestResponse.StudentSummary;
import com.classhub.domain.enrollment.model.EnrollmentStatus;
import com.classhub.domain.enrollment.model.StudentEnrollmentRequest;
import com.classhub.domain.enrollment.repository.StudentEnrollmentRequestRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.StudentInfo;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.member.repository.StudentInfoRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
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
        Map<UUID, StudentSummary> studentMap = buildStudentSummaryMap(requestPage.getContent());
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
        if (courses.size() < courseIds.size()) {
            throw new BusinessException(RsCode.COURSE_NOT_FOUND);
        }
        CourseViewAssembler.CourseContext context = courseViewAssembler.buildContext(courses);
        return courses.stream()
                .collect(Collectors.toMap(
                        Course::getId,
                        course -> courseViewAssembler.toCourseResponse(course, context)
                ));
    }

    private Map<UUID, StudentSummary> buildStudentSummaryMap(Collection<StudentEnrollmentRequest> requests) {
        List<UUID> studentIds = requests.stream()
                .map(StudentEnrollmentRequest::getStudentMemberId)
                .distinct()
                .toList();
        List<Member> members = memberRepository.findAllById(studentIds);
        if (members.size() < studentIds.size()) {
            throw new BusinessException(RsCode.MEMBER_NOT_FOUND);
        }
        Map<UUID, Member> memberMap = members.stream()
                .collect(Collectors.toMap(Member::getId, member -> member));
        List<StudentInfo> infos = studentInfoRepository.findByMemberIdIn(studentIds);
        if (infos.size() < studentIds.size()) {
            throw new BusinessException(RsCode.STUDENT_PROFILE_NOT_FOUND);
        }
        Map<UUID, StudentInfo> infoMap = infos.stream()
                .collect(Collectors.toMap(StudentInfo::getMemberId, info -> info));
        return studentIds.stream()
                .collect(Collectors.toMap(
                        studentId -> studentId,
                        studentId -> {
                            Member member = memberMap.get(studentId);
                            StudentInfo info = infoMap.get(studentId);
                            return new StudentSummary(
                                    member.getId(),
                                    member.getName(),
                                    member.getEmail(),
                                    member.getPhoneNumber(),
                                    info.getSchoolName(),
                                    info.getGrade().name(),
                                    info.getBirthDate() == null ? null : java.time.Period.between(info.getBirthDate(), java.time.LocalDate.now()).getYears()
                            );
                        }
                ));
    }

    private TeacherEnrollmentRequestResponse toResponse(StudentEnrollmentRequest request,
                                                        Map<UUID, CourseResponse> courseMap,
                                                        Map<UUID, StudentSummary> studentMap) {
        CourseResponse courseResponse = courseMap.get(request.getCourseId());
        StudentSummary summary = studentMap.get(request.getStudentMemberId());
        if (courseResponse == null) {
            throw new BusinessException(RsCode.COURSE_NOT_FOUND);
        }
        if (summary == null) {
            throw new BusinessException(RsCode.STUDENT_PROFILE_NOT_FOUND);
        }
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
}
