package com.classhub.domain.enrollment.application;

import com.classhub.domain.course.application.CourseViewAssembler;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.enrollment.dto.request.StudentEnrollmentRequestCreateRequest;
import com.classhub.domain.enrollment.dto.response.StudentEnrollmentRequestResponse;
import com.classhub.domain.enrollment.model.EnrollmentStatus;
import com.classhub.domain.enrollment.model.StudentEnrollmentRequest;
import com.classhub.domain.enrollment.repository.StudentEnrollmentRequestRepository;
import com.classhub.domain.studentcourse.repository.StudentCourseEnrollmentRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
public class StudentEnrollmentRequestService {

    private final StudentEnrollmentRequestRepository requestRepository;
    private final StudentCourseEnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final CourseViewAssembler courseViewAssembler;

    @Transactional
    public StudentEnrollmentRequestResponse createRequest(UUID studentId,
            StudentEnrollmentRequestCreateRequest request) {
        UUID courseId = Objects.requireNonNull(request.courseId(), "courseId must not be null");
        Course course = loadActiveCourse(courseId);
        boolean pendingExists = requestRepository.existsByStudentMemberIdAndCourseIdAndStatusIn(
                studentId,
                courseId,
                EnumSet.of(EnrollmentStatus.PENDING));
        if (pendingExists) {
            throw new BusinessException(RsCode.STUDENT_ENROLLMENT_REQUEST_CONFLICT);
        }
        boolean alreadyEnrolled = enrollmentRepository.existsByStudentMemberIdAndCourseIdAndDeletedAtIsNull(
                studentId,
                courseId);
        if (alreadyEnrolled) {
            throw new BusinessException(RsCode.STUDENT_ENROLLMENT_ALREADY_EXISTS);
        }
        StudentEnrollmentRequest entity = StudentEnrollmentRequest.builder()
                .studentMemberId(studentId)
                .courseId(courseId)
                .status(EnrollmentStatus.PENDING)
                .message(trimMessage(request.message()))
                .build();
        StudentEnrollmentRequest saved = requestRepository.save(entity);
        CourseResponse courseResponse = toCourseResponse(course);
        return toResponse(saved, courseResponse);
    }

    public PageResponse<StudentEnrollmentRequestResponse> getMyRequests(UUID studentId,
            Set<EnrollmentStatus> statuses,
            int page,
            int size) {
        Set<EnrollmentStatus> effectiveStatuses = (statuses == null || statuses.isEmpty())
                ? EnumSet.of(EnrollmentStatus.PENDING)
                : EnumSet.copyOf(statuses);
        PageRequest pageable = PageRequest.of(page, size);
        Page<StudentEnrollmentRequest> requestPage = requestRepository
                .findByStudentMemberIdAndStatusInOrderByCreatedAtDesc(
                        studentId,
                        effectiveStatuses,
                        pageable);
        if (requestPage.isEmpty()) {
            return PageResponse.from(new PageImpl<>(
                    List.of(),
                    pageable,
                    requestPage.getTotalElements()));
        }
        Map<UUID, CourseResponse> courseResponseMap = buildCourseResponseMap(requestPage.getContent());
        List<StudentEnrollmentRequestResponse> content = requestPage.stream()
                .map(req -> {
                    CourseResponse courseResponse = courseResponseMap.get(req.getCourseId());
                    if (courseResponse == null) {
                        throw new BusinessException(RsCode.COURSE_NOT_FOUND);
                    }
                    return toResponse(req, courseResponse);
                })
                .toList();
        Page<StudentEnrollmentRequestResponse> dtoPage = new PageImpl<>(
                content,
                pageable,
                requestPage.getTotalElements());
        return PageResponse.from(dtoPage);
    }

    @Transactional
    public StudentEnrollmentRequestResponse cancelRequest(UUID studentId, UUID requestId) {
        StudentEnrollmentRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(RsCode.STUDENT_ENROLLMENT_REQUEST_NOT_FOUND));
        if (!request.getStudentMemberId().equals(studentId)) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        if (request.getStatus() != EnrollmentStatus.PENDING) {
            throw new BusinessException(RsCode.INVALID_ENROLLMENT_REQUEST_STATE);
        }
        request.cancel(studentId, null);
        Course course = loadActiveCourse(request.getCourseId());
        CourseResponse courseResponse = toCourseResponse(course);
        return toResponse(request, courseResponse);
    }

    private Course loadActiveCourse(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(RsCode.COURSE_NOT_FOUND));
        if (course.isDeleted()) {
            throw new BusinessException(RsCode.COURSE_NOT_FOUND);
        }
        return course;
    }

    private CourseResponse toCourseResponse(Course course) {
        CourseViewAssembler.CourseContext context = courseViewAssembler.buildContext(List.of(course));
        return courseViewAssembler.toCourseResponse(course, context);
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
                        course -> courseViewAssembler.toCourseResponse(course, context)));
    }

    private StudentEnrollmentRequestResponse toResponse(StudentEnrollmentRequest request,
            CourseResponse courseResponse) {
        return new StudentEnrollmentRequestResponse(
                request.getId(),
                courseResponse,
                request.getStatus(),
                request.getMessage(),
                request.getProcessedAt(),
                request.getProcessedByMemberId(),
                request.getCreatedAt());
    }

    private String trimMessage(String message) {
        if (message == null) {
            return null;
        }
        String trimmed = message.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
