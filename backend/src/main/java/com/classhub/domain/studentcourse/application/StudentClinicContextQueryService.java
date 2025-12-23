package com.classhub.domain.studentcourse.application;

import com.classhub.domain.course.application.CourseViewAssembler;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.studentcourse.dto.response.StudentClinicContextResponse;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentClinicContextQueryService {

    private final StudentCourseRecordRepository recordRepository;
    private final CourseRepository courseRepository;
    private final CourseViewAssembler courseViewAssembler;
    private final MemberRepository memberRepository;

    public List<StudentClinicContextResponse> getContexts(UUID studentId) {
        List<StudentCourseRecord> records = recordRepository.findByStudentMemberIdAndDeletedAtIsNull(studentId);
        if (records.isEmpty()) {
            return List.of();
        }
        List<UUID> courseIds = records.stream()
                .map(StudentCourseRecord::getCourseId)
                .distinct()
                .toList();
        List<Course> courses = courseRepository.findAllById(courseIds).stream()
                .filter(course -> !course.isDeleted())
                .toList();
        if (courses.size() < courseIds.size()) {
            throw new BusinessException(RsCode.COURSE_NOT_FOUND);
        }

        CourseViewAssembler.CourseContext context = courseViewAssembler.buildContext(courses);
        Map<UUID, Course> courseMap = courses.stream()
                .collect(Collectors.toMap(Course::getId, course -> course));
        Map<UUID, CourseResponse> courseResponseMap = courses.stream()
                .collect(Collectors.toMap(
                        Course::getId,
                        course -> courseViewAssembler.toCourseResponse(course, context)
                ));
        Map<UUID, Member> teacherMap = loadTeachers(courses);

        return records.stream()
                .map(record -> {
                    Course course = courseMap.get(record.getCourseId());
                    if (course == null) {
                        throw new BusinessException(RsCode.COURSE_NOT_FOUND);
                    }
                    CourseResponse courseResponse = courseResponseMap.get(record.getCourseId());
                    if (courseResponse == null) {
                        throw new BusinessException(RsCode.COURSE_NOT_FOUND);
                    }
                    Member teacher = teacherMap.get(course.getTeacherMemberId());
                    if (teacher == null) {
                        throw new BusinessException(RsCode.MEMBER_NOT_FOUND);
                    }
                    return new StudentClinicContextResponse(
                            courseResponse.courseId(),
                            courseResponse.name(),
                            record.getId(),
                            record.getDefaultClinicSlotId(),
                            course.getTeacherMemberId(),
                            teacher.getName(),
                            courseResponse.branchId(),
                            courseResponse.branchName(),
                            courseResponse.companyId(),
                            courseResponse.companyName()
                    );
                })
                .toList();
    }

    private Map<UUID, Member> loadTeachers(List<Course> courses) {
        List<UUID> teacherIds = courses.stream()
                .map(Course::getTeacherMemberId)
                .distinct()
                .toList();
        List<Member> teachers = memberRepository.findAllById(teacherIds).stream()
                .filter(member -> !member.isDeleted())
                .toList();
        if (teachers.size() < teacherIds.size()) {
            throw new BusinessException(RsCode.MEMBER_NOT_FOUND);
        }
        return teachers.stream()
                .collect(Collectors.toMap(Member::getId, teacher -> teacher));
    }
}
