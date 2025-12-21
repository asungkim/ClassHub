package com.classhub.domain.progress.support;

import com.classhub.domain.assignment.model.TeacherAssistantAssignment;
import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProgressPermissionValidator {

    private final CourseRepository courseRepository;
    private final StudentCourseRecordRepository studentCourseRecordRepository;
    private final TeacherAssistantAssignmentRepository assistantAssignmentRepository;

    public Course ensureCourseAccess(MemberPrincipal principal,
                                     UUID courseId,
                                     ProgressAccessMode accessMode) {
        return switch (principal.role()) {
            case TEACHER -> ensureTeacherCourse(principal.id(), courseId);
            case ASSISTANT -> ensureAssistantCourse(principal.id(), courseId, accessMode);
            default -> throw new BusinessException(RsCode.FORBIDDEN);
        };
    }

    public StudentCourseRecord ensureRecordAccess(MemberPrincipal principal,
                                                  UUID recordId,
                                                  ProgressAccessMode accessMode) {
        return switch (principal.role()) {
            case TEACHER -> ensureTeacherRecord(principal.id(), recordId);
            case ASSISTANT -> {
                if (accessMode == ProgressAccessMode.WRITE) {
                    throw new BusinessException(RsCode.FORBIDDEN);
                }
                yield ensureAssistantRecord(principal.id(), recordId);
            }
            default -> throw new BusinessException(RsCode.FORBIDDEN);
        };
    }

    public List<StudentCourseRecord> ensureCalendarAccess(MemberPrincipal principal, UUID studentId) {
        return switch (principal.role()) {
            case TEACHER -> ensureCalendarForTeacher(principal.id(), studentId);
            case ASSISTANT -> ensureCalendarForAssistant(principal.id(), studentId);
            default -> throw new BusinessException(RsCode.FORBIDDEN);
        };
    }

    private Course ensureTeacherCourse(UUID teacherId, UUID courseId) {
        Course course = loadCourse(courseId);
        if (!course.getTeacherMemberId().equals(teacherId)) {
            throw new BusinessException(RsCode.COURSE_FORBIDDEN);
        }
        return course;
    }

    private Course ensureAssistantCourse(UUID assistantId,
                                         UUID courseId,
                                         ProgressAccessMode accessMode) {
        if (accessMode == ProgressAccessMode.WRITE) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        Course course = loadCourse(courseId);
        ensureAssistantAssignment(assistantId, course.getTeacherMemberId());
        return course;
    }

    private StudentCourseRecord ensureTeacherRecord(UUID teacherId, UUID recordId) {
        StudentCourseRecord record = loadRecord(recordId);
        ensureTeacherCourse(teacherId, record.getCourseId());
        return record;
    }

    private StudentCourseRecord ensureAssistantRecord(UUID assistantId, UUID recordId) {
        StudentCourseRecord record = loadRecord(recordId);
        Course course = loadCourse(record.getCourseId());
        ensureAssistantAssignment(assistantId, course.getTeacherMemberId());
        return record;
    }

    private List<StudentCourseRecord> ensureCalendarForTeacher(UUID teacherId, UUID studentId) {
        List<StudentCourseRecord> records = studentCourseRecordRepository
                .findActiveByStudentIdAndTeacherId(studentId, teacherId);
        if (records.isEmpty()) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        return records;
    }

    private List<StudentCourseRecord> ensureCalendarForAssistant(UUID assistantId, UUID studentId) {
        List<TeacherAssistantAssignment> assignments = assistantAssignmentRepository
                .findByAssistantMemberIdAndDeletedAtIsNull(assistantId);
        if (assignments.isEmpty()) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        List<UUID> teacherIds = assignments.stream()
                .map(TeacherAssistantAssignment::getTeacherMemberId)
                .distinct()
                .toList();
        if (teacherIds.isEmpty()) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        List<StudentCourseRecord> records = studentCourseRecordRepository
                .findActiveByStudentIdAndTeacherIds(studentId, teacherIds);
        if (records.isEmpty()) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        return records;
    }

    private Course loadCourse(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(RsCode.COURSE_NOT_FOUND));
        if (course.isDeleted()) {
            throw new BusinessException(RsCode.COURSE_NOT_FOUND);
        }
        return course;
    }

    private StudentCourseRecord loadRecord(UUID recordId) {
        StudentCourseRecord record = studentCourseRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(RsCode.STUDENT_COURSE_RECORD_NOT_FOUND));
        if (record.isDeleted()) {
            throw new BusinessException(RsCode.STUDENT_COURSE_RECORD_NOT_FOUND);
        }
        return record;
    }

    private void ensureAssistantAssignment(UUID assistantId, UUID teacherId) {
        boolean active = assistantAssignmentRepository
                .findByTeacherMemberIdAndAssistantMemberIdAndDeletedAtIsNull(teacherId, assistantId)
                .isPresent();
        if (!active) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
    }

    public enum ProgressAccessMode {
        READ,
        WRITE
    }
}
