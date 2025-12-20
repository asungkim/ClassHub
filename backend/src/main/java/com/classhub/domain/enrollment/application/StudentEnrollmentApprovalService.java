package com.classhub.domain.enrollment.application;

import com.classhub.domain.assignment.model.TeacherAssistantAssignment;
import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
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
import com.classhub.domain.studentcourse.model.StudentCourseEnrollment;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseEnrollmentRepository;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
import java.time.Period;
import java.time.LocalDateTime;
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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentEnrollmentApprovalService {

    private final StudentEnrollmentRequestRepository requestRepository;
    private final StudentCourseEnrollmentRepository enrollmentRepository;
    private final StudentCourseRecordRepository recordRepository;
    private final CourseRepository courseRepository;
    private final CourseViewAssembler courseViewAssembler;
    private final MemberRepository memberRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final TeacherAssistantAssignmentRepository assistantAssignmentRepository;

    public PageResponse<TeacherEnrollmentRequestResponse> getRequestsForTeacher(UUID teacherId,
            UUID courseId,
            Set<EnrollmentStatus> statuses,
            String studentName,
            int page,
            int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<StudentEnrollmentRequest> requestPage = requestRepository.searchRequestsForTeacher(
                teacherId,
                courseId,
                resolveStatuses(statuses),
                normalizeKeyword(studentName),
                pageable);
        return toPageResponse(requestPage, pageable);
    }

    public PageResponse<TeacherEnrollmentRequestResponse> getRequestsForAssistant(UUID assistantId,
                                                                                  UUID courseId,
                                                                                  Set<EnrollmentStatus> statuses,
                                                                                  String studentName,
                                                                                  int page,
                                                                                  int size) {
        PageRequest pageable = PageRequest.of(page, size);
        List<TeacherAssistantAssignment> assignments = assistantAssignmentRepository
                .findByAssistantMemberIdAndDeletedAtIsNull(assistantId);
        if (assignments.isEmpty()) {
            return emptyPage(pageable);
        }
        List<UUID> teacherIds = assignments.stream()
                .map(TeacherAssistantAssignment::getTeacherMemberId)
                .distinct()
                .toList();
        Page<StudentEnrollmentRequest> requestPage = requestRepository.searchRequestsForTeachers(
                teacherIds,
                courseId,
                resolveStatuses(statuses),
                normalizeKeyword(studentName),
                pageable);
        return toPageResponse(requestPage, pageable);
    }

    public TeacherEnrollmentRequestResponse getRequestDetail(UUID actorId, UUID requestId) {
        StudentEnrollmentRequest request = loadRequest(requestId);
        Course course = loadCourse(request.getCourseId());
        ensurePermission(actorId, course.getTeacherMemberId());
        return buildSingleResponse(request, course);
    }

    @Transactional
    public TeacherEnrollmentRequestResponse approveRequest(UUID processorId, UUID requestId) {
        StudentEnrollmentRequest request = loadRequest(requestId);
        ensurePending(request);
        Course course = loadCourse(request.getCourseId());
        ensurePermission(processorId, course.getTeacherMemberId());
        boolean alreadyEnrolled = enrollmentRepository.existsByStudentMemberIdAndCourseIdAndDeletedAtIsNull(
                request.getStudentMemberId(),
                request.getCourseId());
        if (alreadyEnrolled) {
            throw new BusinessException(RsCode.STUDENT_ENROLLMENT_ALREADY_EXISTS);
        }
        request.approve(processorId, LocalDateTime.now());
        StudentCourseEnrollment enrollment = StudentCourseEnrollment.create(
                request.getStudentMemberId(),
                request.getCourseId(),
                LocalDateTime.now());
        enrollmentRepository.save(enrollment);
        StudentCourseRecord record = StudentCourseRecord.create(
                request.getStudentMemberId(),
                request.getCourseId(),
                null,
                null,
                null);
        recordRepository.save(record);
        return buildSingleResponse(request, course);
    }

    @Transactional
    public TeacherEnrollmentRequestResponse rejectRequest(UUID processorId, UUID requestId) {
        StudentEnrollmentRequest request = loadRequest(requestId);
        ensurePending(request);
        Course course = loadCourse(request.getCourseId());
        ensurePermission(processorId, course.getTeacherMemberId());
        request.reject(processorId, LocalDateTime.now());
        return buildSingleResponse(request, course);
    }

    private PageResponse<TeacherEnrollmentRequestResponse> toPageResponse(Page<StudentEnrollmentRequest> requestPage,
            PageRequest pageable) {
        if (requestPage.isEmpty()) {
            Page<TeacherEnrollmentRequestResponse> emptyPage = new PageImpl<>(
                    List.of(),
                    pageable,
                    requestPage.getTotalElements());
            return PageResponse.from(emptyPage);
        }
        Map<UUID, CourseResponse> courseMap = buildCourseResponseMap(requestPage.getContent());
        Map<UUID, StudentSummary> studentMap = buildStudentSummaryMap(requestPage.getContent());
        List<TeacherEnrollmentRequestResponse> content = requestPage.getContent().stream()
                .map(request -> toResponse(request, courseMap, studentMap))
                .toList();
        Page<TeacherEnrollmentRequestResponse> dtoPage = new PageImpl<>(
                content,
                pageable,
                requestPage.getTotalElements());
        return PageResponse.from(dtoPage);
    }

    private PageResponse<TeacherEnrollmentRequestResponse> emptyPage(PageRequest pageable) {
        Page<TeacherEnrollmentRequestResponse> empty = new PageImpl<>(List.of(), pageable, 0);
        return PageResponse.from(empty);
    }

    private Map<UUID, CourseResponse> buildCourseResponseMap(Collection<StudentEnrollmentRequest> requests) {
        List<UUID> courseIds = requests.stream()
                .map(StudentEnrollmentRequest::getCourseId)
                .distinct()
                .toList();
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        List<Course> courses = courseRepository.findAllById(courseIds);
        if (courses.size() < courseIds.size()) {
            throw new BusinessException(RsCode.COURSE_NOT_FOUND);
        }
        CourseViewAssembler.CourseContext context = courseViewAssembler.buildContext(courses);
        return courses.stream()
                .collect(Collectors.toMap(
                        Course::getId,
                        course -> courseViewAssembler.toCourseResponse(course, context)));
    }

    private Map<UUID, StudentSummary> buildStudentSummaryMap(Collection<StudentEnrollmentRequest> requests) {
        List<UUID> studentIds = requests.stream()
                .map(StudentEnrollmentRequest::getStudentMemberId)
                .distinct()
                .toList();
        if (studentIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Member> memberMap = memberRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(Member::getId, member -> member));
        if (memberMap.size() < studentIds.size()) {
            throw new BusinessException(RsCode.MEMBER_NOT_FOUND);
        }
        Map<UUID, StudentInfo> infoMap = studentInfoRepository.findByMemberIdIn(studentIds).stream()
                .collect(Collectors.toMap(StudentInfo::getMemberId, info -> info));
        if (infoMap.size() < studentIds.size()) {
            throw new BusinessException(RsCode.STUDENT_PROFILE_NOT_FOUND);
        }
        return studentIds.stream()
                .collect(Collectors.toMap(
                        studentId -> studentId,
                        studentId -> toStudentSummary(memberMap.get(studentId), infoMap.get(studentId))));
    }

    private TeacherEnrollmentRequestResponse toResponse(StudentEnrollmentRequest request,
            Map<UUID, CourseResponse> courseMap,
            Map<UUID, StudentSummary> studentMap) {
        CourseResponse courseResponse = courseMap.get(request.getCourseId());
        if (courseResponse == null) {
            throw new BusinessException(RsCode.COURSE_NOT_FOUND);
        }
        StudentSummary summary = studentMap.get(request.getStudentMemberId());
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
                request.getCreatedAt());
    }

    private TeacherEnrollmentRequestResponse buildSingleResponse(StudentEnrollmentRequest request, Course course) {
        CourseViewAssembler.CourseContext context = courseViewAssembler.buildContext(List.of(course));
        CourseResponse courseResponse = courseViewAssembler.toCourseResponse(course, context);
        StudentSummary summary = loadStudentSummary(request.getStudentMemberId());
        return new TeacherEnrollmentRequestResponse(
                request.getId(),
                courseResponse,
                summary,
                request.getStatus(),
                request.getMessage(),
                request.getProcessedAt(),
                request.getProcessedByMemberId(),
                request.getCreatedAt());
    }

    private StudentSummary loadStudentSummary(UUID studentId) {
        Member member = memberRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(RsCode.MEMBER_NOT_FOUND));
        StudentInfo studentInfo = studentInfoRepository.findByMemberId(studentId)
                .orElseThrow(() -> new BusinessException(RsCode.STUDENT_PROFILE_NOT_FOUND));
        return toStudentSummary(member, studentInfo);
    }

    private StudentSummary toStudentSummary(Member member, StudentInfo info) {
        return new StudentSummary(
                member.getId(),
                member.getName(),
                member.getEmail(),
                member.getPhoneNumber(),
                info.getSchoolName(),
                info.getGrade().name(),
                calculateAge(info.getBirthDate()));
    }

    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    private StudentEnrollmentRequest loadRequest(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(RsCode.STUDENT_ENROLLMENT_REQUEST_NOT_FOUND));
    }

    private Course loadCourse(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(RsCode.COURSE_NOT_FOUND));
        if (course.isDeleted()) {
            throw new BusinessException(RsCode.COURSE_NOT_FOUND);
        }
        return course;
    }

    private void ensurePermission(UUID processorId, UUID teacherId) {
        if (teacherId.equals(processorId)) {
            return;
        }
        boolean allowed = assistantAssignmentRepository
                .findByTeacherMemberIdAndAssistantMemberIdAndDeletedAtIsNull(teacherId, processorId)
                .isPresent();
        if (!allowed) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
    }

    private void ensurePending(StudentEnrollmentRequest request) {
        if (request.getStatus() != EnrollmentStatus.PENDING) {
            throw new BusinessException(RsCode.INVALID_ENROLLMENT_REQUEST_STATE);
        }
    }

    private Set<EnrollmentStatus> resolveStatuses(Set<EnrollmentStatus> statuses) {
        return (statuses == null || statuses.isEmpty())
                ? EnumSet.of(EnrollmentStatus.PENDING)
                : EnumSet.copyOf(statuses);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
