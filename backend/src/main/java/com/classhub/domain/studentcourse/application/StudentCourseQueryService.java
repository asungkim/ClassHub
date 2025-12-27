package com.classhub.domain.studentcourse.application;

import com.classhub.domain.course.application.CourseViewAssembler;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.studentcourse.dto.response.StudentMyCourseResponse;
import com.classhub.domain.studentcourse.model.StudentCourseAssignment;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseAssignmentRepository;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
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

    private final StudentCourseAssignmentRepository assignmentRepository;
    private final StudentCourseRecordRepository recordRepository;
    private final CourseRepository courseRepository;
    private final CourseViewAssembler courseViewAssembler;

    public PageResponse<StudentMyCourseResponse> getMyCourses(UUID studentId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<StudentCourseAssignment> assignmentPage = assignmentRepository
                .findByStudentMemberIdWithActiveCourse(studentId, pageable);
        if (assignmentPage.isEmpty()) {
            return PageResponse.from(new PageImpl<>(List.of(), pageable, assignmentPage.getTotalElements()));
        }
        List<StudentCourseAssignment> assignments = assignmentPage.getContent();
        List<UUID> courseIds = assignments.stream()
                .map(StudentCourseAssignment::getCourseId)
                .distinct()
                .toList();
        List<Course> courses = courseRepository.findAllById(courseIds).stream()
                .filter(course -> !course.isDeleted())
                .toList();
        if (courses.size() < courseIds.size()) {
            throw new BusinessException(RsCode.COURSE_NOT_FOUND);
        }
        CourseViewAssembler.CourseContext context = courseViewAssembler.buildContext(courses);
        Map<UUID, CourseResponse> courseMap = courses.stream()
                .collect(Collectors.toMap(
                        Course::getId,
                        course -> courseViewAssembler.toCourseResponse(course, context)
                ));
        Map<UUID, StudentCourseRecord> recordMap = recordRepository
                .findByStudentMemberIdAndDeletedAtIsNull(studentId)
                .stream()
                .collect(Collectors.toMap(StudentCourseRecord::getCourseId, record -> record, (a, b) -> a));
        List<StudentMyCourseResponse> content = assignments.stream()
                .map(assignment -> {
                    CourseResponse course = courseMap.get(assignment.getCourseId());
                    if (course == null) {
                        throw new BusinessException(RsCode.COURSE_NOT_FOUND);
                    }
                    StudentCourseRecord record = recordMap.get(assignment.getCourseId());
                    return new StudentMyCourseResponse(
                            assignment.getId(),
                            assignment.getAssignedAt(),
                            assignment.isActive(),
                            record == null ? null : record.getId(),
                            course
                    );
                })
                .toList();
        Page<StudentMyCourseResponse> dtoPage = new PageImpl<>(
                content,
                pageable,
                assignmentPage.getTotalElements()
        );
        return PageResponse.from(dtoPage);
    }
}
