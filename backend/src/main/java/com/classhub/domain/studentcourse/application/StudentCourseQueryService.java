package com.classhub.domain.studentcourse.application;

import com.classhub.domain.course.application.CourseViewAssembler;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.studentcourse.dto.response.StudentCourseResponse;
import com.classhub.domain.studentcourse.model.StudentCourseEnrollment;
import com.classhub.domain.studentcourse.repository.StudentCourseEnrollmentRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.util.List;
import java.util.Map;
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
public class StudentCourseQueryService {

    private final StudentCourseEnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final CourseViewAssembler courseViewAssembler;

    public PageResponse<StudentCourseResponse> getMyCourses(UUID studentId, String keyword, int page, int size) {
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        PageRequest pageable = PageRequest.of(page, size);
        Page<StudentCourseEnrollment> enrollmentPage = enrollmentRepository.searchActiveEnrollments(
                studentId,
                normalizedKeyword,
                pageable
        );
        if (enrollmentPage.isEmpty()) {
            return PageResponse.from(new PageImpl<>(List.of(), pageable, enrollmentPage.getTotalElements()));
        }
        Map<UUID, CourseResponse> courseMap = loadCourseResponses(enrollmentPage.getContent());
        List<StudentCourseResponse> content = enrollmentPage.getContent().stream()
                .map(enrollment -> {
                    CourseResponse courseResponse = courseMap.get(enrollment.getCourseId());
                    if (courseResponse == null) {
                        throw new BusinessException(RsCode.COURSE_NOT_FOUND);
                    }
                    return new StudentCourseResponse(
                            enrollment.getId(),
                            enrollment.getEnrolledAt(),
                            courseResponse
                    );
                })
                .toList();
        Page<StudentCourseResponse> dtoPage = new PageImpl<>(
                content,
                pageable,
                enrollmentPage.getTotalElements()
        );
        return PageResponse.from(dtoPage);
    }

    private Map<UUID, CourseResponse> loadCourseResponses(List<StudentCourseEnrollment> enrollments) {
        List<UUID> courseIds = enrollments.stream()
                .map(StudentCourseEnrollment::getCourseId)
                .distinct()
                .toList();
        List<Course> courses = courseRepository.findAllById(courseIds).stream()
                .filter(course -> !course.isDeleted())
                .toList();
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
}
