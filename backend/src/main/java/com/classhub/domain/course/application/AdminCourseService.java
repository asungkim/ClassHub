package com.classhub.domain.course.application;

import com.classhub.domain.course.dto.CourseStatusFilter;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminCourseService {

    private final CourseRepository courseRepository;
    private final CourseViewAssembler courseViewAssembler;

    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> searchCourses(UUID teacherId,
                                                      UUID branchId,
                                                      UUID companyId,
                                                      CourseStatusFilter status,
                                                      String keyword,
                                                      int page,
                                                      int size) {
        Page<Course> result = courseRepository.searchCoursesForAdmin(
                teacherId,
                branchId,
                companyId,
                status,
                normalizeKeyword(keyword),
                PageRequest.of(page, size)
        );
        CourseViewAssembler.CourseContext context = courseViewAssembler.buildContext(result.getContent());
        Page<CourseResponse> mapped = result.map(course -> courseViewAssembler.toCourseResponse(course, context));
        return PageResponse.from(mapped);
    }

    public void deleteCourse(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(RsCode.COURSE_NOT_FOUND));
        courseRepository.delete(course);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
