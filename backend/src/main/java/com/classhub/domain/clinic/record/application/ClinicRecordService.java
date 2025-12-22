package com.classhub.domain.clinic.record.application;

import com.classhub.domain.clinic.permission.application.ClinicPermissionValidator;
import com.classhub.domain.clinic.attendance.model.ClinicAttendance;
import com.classhub.domain.clinic.attendance.repository.ClinicAttendanceRepository;
import com.classhub.domain.clinic.record.dto.request.ClinicRecordCreateRequest;
import com.classhub.domain.clinic.record.dto.request.ClinicRecordUpdateRequest;
import com.classhub.domain.clinic.record.model.ClinicRecord;
import com.classhub.domain.clinic.record.repository.ClinicRecordRepository;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
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
    private final ClinicPermissionValidator clinicPermissionValidator;

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
        clinicPermissionValidator.ensureStaffAccess(principal, course.getTeacherMemberId());
    }
}
