package com.classhub.domain.clinic.clinicrecord.application;

import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.clinic.clinicattendance.model.ClinicAttendance;
import com.classhub.domain.clinic.clinicattendance.repository.ClinicAttendanceRepository;
import com.classhub.domain.clinic.clinicrecord.dto.request.ClinicRecordCreateRequest;
import com.classhub.domain.clinic.clinicrecord.dto.request.ClinicRecordUpdateRequest;
import com.classhub.domain.clinic.clinicrecord.model.ClinicRecord;
import com.classhub.domain.clinic.clinicrecord.repository.ClinicRecordRepository;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ClinicRecordService {

    private final ClinicRecordRepository clinicRecordRepository;
    private final ClinicAttendanceRepository clinicAttendanceRepository;
    private final StudentCourseRecordRepository studentCourseRecordRepository;
    private final CourseRepository courseRepository;
    private final TeacherAssistantAssignmentRepository teacherAssistantAssignmentRepository;

    public ClinicRecord createRecord(MemberPrincipal principal, ClinicRecordCreateRequest request) {
        if (request == null || request.clinicAttendanceId() == null
                || request.title() == null || request.content() == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        if (clinicRecordRepository.existsByClinicAttendanceId(request.clinicAttendanceId())) {
            throw new BusinessException(RsCode.CLINIC_RECORD_ALREADY_EXISTS);
        }
        ClinicAttendance attendance = loadAttendance(request.clinicAttendanceId());
        Course course = loadCourseByAttendance(attendance);
        ensureWriterPermission(principal, course);

        ClinicRecord record = ClinicRecord.builder()
                .clinicAttendanceId(attendance.getId())
                .writerId(principal.id())
                .title(request.title())
                .content(request.content())
                .homeworkProgress(request.homeworkProgress())
                .build();
        return clinicRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public ClinicRecord getRecord(MemberPrincipal principal, UUID attendanceId) {
        if (attendanceId == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        ClinicRecord record = clinicRecordRepository.findByClinicAttendanceId(attendanceId)
                .orElseThrow(RsCode.CLINIC_RECORD_NOT_FOUND::toException);
        ClinicAttendance attendance = loadAttendance(attendanceId);
        Course course = loadCourseByAttendance(attendance);
        ensureWriterPermission(principal, course);
        return record;
    }

    public ClinicRecord updateRecord(MemberPrincipal principal, UUID recordId, ClinicRecordUpdateRequest request) {
        if (recordId == null || request == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        ClinicRecord record = clinicRecordRepository.findById(recordId)
                .orElseThrow(RsCode.CLINIC_RECORD_NOT_FOUND::toException);
        Course course = loadCourseByAttendance(loadAttendance(record.getClinicAttendanceId()));
        ensureWriterPermission(principal, course);
        record.update(request.title(), request.content(), request.homeworkProgress());
        return clinicRecordRepository.save(record);
    }

    public void deleteRecord(MemberPrincipal principal, UUID recordId) {
        if (recordId == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        ClinicRecord record = clinicRecordRepository.findById(recordId)
                .orElseThrow(RsCode.CLINIC_RECORD_NOT_FOUND::toException);
        Course course = loadCourseByAttendance(loadAttendance(record.getClinicAttendanceId()));
        ensureWriterPermission(principal, course);
        clinicRecordRepository.delete(record);
    }

    private ClinicAttendance loadAttendance(UUID attendanceId) {
        return clinicAttendanceRepository.findById(attendanceId)
                .orElseThrow(RsCode.CLINIC_ATTENDANCE_NOT_FOUND::toException);
    }

    private Course loadCourseByAttendance(ClinicAttendance attendance) {
        StudentCourseRecord record = studentCourseRecordRepository
                .findById(attendance.getStudentCourseRecordId())
                .orElseThrow(RsCode.STUDENT_COURSE_RECORD_NOT_FOUND::toException);
        if (record.isDeleted()) {
            throw new BusinessException(RsCode.STUDENT_COURSE_RECORD_NOT_FOUND);
        }
        Course course = courseRepository.findById(record.getCourseId())
                .orElseThrow(RsCode.COURSE_NOT_FOUND::toException);
        if (course.isDeleted()) {
            throw new BusinessException(RsCode.COURSE_NOT_FOUND);
        }
        return course;
    }

    private void ensureWriterPermission(MemberPrincipal principal, Course course) {
        if (principal.role() == MemberRole.TEACHER) {
            if (!Objects.equals(course.getTeacherMemberId(), principal.id())) {
                throw new BusinessException(RsCode.FORBIDDEN);
            }
            return;
        }
        if (principal.role() == MemberRole.ASSISTANT) {
            boolean assigned = teacherAssistantAssignmentRepository
                    .findByTeacherMemberIdAndAssistantMemberIdAndDeletedAtIsNull(
                            course.getTeacherMemberId(),
                            principal.id()
                    )
                    .isPresent();
            if (!assigned) {
                throw new BusinessException(RsCode.FORBIDDEN);
            }
            return;
        }
        throw new BusinessException(RsCode.FORBIDDEN);
    }
}
