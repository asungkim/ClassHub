package com.classhub.domain.course.application;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseArchiveService {

    private final CourseRepository courseRepository;

    public int archiveExpiredCourses(LocalDate today) {
        if (today == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        LocalDate threshold = today.minusDays(7);
        List<Course> courses = courseRepository.findByEndDateLessThanEqualAndDeletedAtIsNull(threshold);
        if (courses.isEmpty()) {
            return 0;
        }
        courses.forEach(Course::deactivate);
        courseRepository.saveAll(courses);
        return courses.size();
    }
}
