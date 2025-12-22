package com.classhub.domain.clinic.clinicattendance.application;

import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.clinic.clinicattendance.model.ClinicAttendance;
import com.classhub.domain.clinic.clinicattendance.repository.ClinicAttendanceRepository;
import com.classhub.domain.clinic.clinicattendance.repository.ClinicAttendanceDetailProjection;
import com.classhub.domain.clinic.clinicattendance.support.ClinicAttendancePolicy;
import com.classhub.domain.clinic.clinicattendance.dto.response.ClinicAttendanceResponse;
import com.classhub.domain.clinic.clinicattendance.dto.response.StudentClinicAttendanceListResponse;
import com.classhub.domain.clinic.clinicattendance.dto.response.StudentClinicAttendanceResponse;
import com.classhub.domain.clinic.clinicattendance.dto.response.ClinicAttendanceDetailResponse;
import com.classhub.domain.clinic.clinicsession.model.ClinicSession;
import com.classhub.domain.clinic.clinicsession.repository.ClinicSessionRepository;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ClinicAttendanceService {

    private final ClinicAttendanceRepository clinicAttendanceRepository;
    private final ClinicSessionRepository clinicSessionRepository;
    private final StudentCourseRecordRepository studentCourseRecordRepository;
    private final CourseRepository courseRepository;
    private final TeacherAssistantAssignmentRepository teacherAssistantAssignmentRepository;

    @Transactional(readOnly = true)
    public List<ClinicAttendanceDetailResponse> getAttendanceDetails(MemberPrincipal principal, UUID sessionId) {
        ClinicSession session = loadSession(sessionId);
        ensureStaffAccess(principal, session);
        List<ClinicAttendanceDetailProjection> projections =
                clinicAttendanceRepository.findDetailsByClinicSessionId(sessionId);
        return projections.stream()
                .map(projection -> new ClinicAttendanceDetailResponse(
                        projection.getAttendanceId(),
                        projection.getRecordId(),
                        projection.getStudentCourseRecordId(),
                        projection.getStudentMemberId(),
                        projection.getStudentName(),
                        projection.getPhoneNumber(),
                        projection.getSchoolName(),
                        projection.getGrade() == null ? null : projection.getGrade().name(),
                        projection.getParentPhoneNumber(),
                        calculateAge(projection.getBirthDate())
                ))
                .toList();
    }

    public ClinicAttendance addAttendance(MemberPrincipal principal, UUID sessionId, UUID recordId) {
        ClinicSession session = loadSessionForUpdate(sessionId);
        ensureStaffAccess(principal, session);
        ensureSessionWritable(session);
        StudentCourseRecord record = loadActiveRecord(recordId);
        Course course = loadCourse(record.getCourseId());
        ensureRecordMatchesSession(course, session);
        ensureAttendanceCreatable(session, record.getId());
        return saveAttendance(sessionId, record.getId());
    }

    public void deleteAttendance(MemberPrincipal principal, UUID attendanceId) {
        if (attendanceId == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        ClinicAttendance attendance = clinicAttendanceRepository.findById(attendanceId)
                .orElseThrow(RsCode.CLINIC_ATTENDANCE_NOT_FOUND::toException);
        ClinicSession session = loadSession(attendance.getClinicSessionId());
        ensureStaffAccess(principal, session);
        ensureSessionWritable(session);
        clinicAttendanceRepository.delete(attendance);
    }

    public ClinicAttendance requestAttendance(MemberPrincipal principal, UUID sessionId, UUID recordId) {
        ensureStudentRole(principal);
        ClinicSession session = loadSessionForUpdate(sessionId);
        ensureSessionActive(session);
        ensureSessionNotLocked(session);
        StudentCourseRecord record = loadActiveRecord(recordId);
        if (!Objects.equals(record.getStudentMemberId(), principal.id())) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        Course course = loadCourse(record.getCourseId());
        ensureRecordMatchesSession(course, session);
        ensureAttendanceCreatable(session, record.getId());
        return saveAttendance(sessionId, record.getId());
    }

    public ClinicAttendance moveAttendance(MemberPrincipal principal, UUID fromSessionId, UUID toSessionId) {
        ensureStudentRole(principal);
        if (fromSessionId == null || toSessionId == null || fromSessionId.equals(toSessionId)) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        ClinicSession fromSession = loadSession(fromSessionId);
        ClinicSession toSession = loadSessionForUpdate(toSessionId);
        ensureSessionActive(fromSession);
        ensureSessionActive(toSession);

        if (!ClinicAttendancePolicy.isMoveAllowed(fromSession, LocalDateTime.now())) {
            throw new BusinessException(RsCode.CLINIC_ATTENDANCE_MOVE_FORBIDDEN);
        }
        if (!isSameWeek(fromSession, toSession)) {
            throw new BusinessException(RsCode.CLINIC_ATTENDANCE_MOVE_FORBIDDEN);
        }

        List<StudentCourseRecord> records = studentCourseRecordRepository
                .findActiveByStudentIdAndTeacherId(principal.id(), fromSession.getTeacherMemberId());
        if (records.isEmpty()) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        List<UUID> recordIds = records.stream().map(StudentCourseRecord::getId).toList();
        ClinicAttendance attendance = clinicAttendanceRepository
                .findByClinicSessionIdAndStudentCourseRecordIdIn(fromSessionId, recordIds)
                .orElseThrow(RsCode.CLINIC_ATTENDANCE_NOT_FOUND::toException);
        StudentCourseRecord record = loadActiveRecord(attendance.getStudentCourseRecordId());
        Course course = loadCourse(record.getCourseId());
        ensureRecordMatchesSession(course, toSession);

        ensureAttendanceCreatable(toSession, record.getId());
        clinicAttendanceRepository.delete(attendance);
        return saveAttendance(toSessionId, record.getId());
    }

    @Transactional(readOnly = true)
    public StudentClinicAttendanceListResponse getStudentAttendanceResponses(MemberPrincipal principal,
                                                                             LocalDate startDate,
                                                                             LocalDate endDate) {
        ensureStudentRole(principal);
        List<ClinicAttendance> attendances = loadStudentAttendances(principal, startDate, endDate);
        if (attendances.isEmpty()) {
            return new StudentClinicAttendanceListResponse(List.of());
        }
        Map<UUID, ClinicSession> sessionMap = clinicSessionRepository.findAllById(
                        attendances.stream().map(ClinicAttendance::getClinicSessionId).distinct().toList()
                )
                .stream()
                .collect(Collectors.toMap(ClinicSession::getId, session -> session));
        List<StudentClinicAttendanceResponse> items = attendances.stream()
                .map(attendance -> {
                    ClinicSession session = sessionMap.get(attendance.getClinicSessionId());
                    if (session == null) {
                        throw new BusinessException(RsCode.CLINIC_SESSION_NOT_FOUND);
                    }
                    return StudentClinicAttendanceResponse.from(
                            ClinicAttendanceResponse.from(attendance),
                            session
                    );
                })
                .toList();
        return new StudentClinicAttendanceListResponse(items);
    }

    private ClinicSession loadSession(UUID sessionId) {
        if (sessionId == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        return clinicSessionRepository.findByIdAndDeletedAtIsNull(sessionId)
                .orElseThrow(RsCode.CLINIC_SESSION_NOT_FOUND::toException);
    }

    private ClinicSession loadSessionForUpdate(UUID sessionId) {
        if (sessionId == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        return clinicSessionRepository.findByIdAndDeletedAtIsNullForUpdate(sessionId)
                .orElseThrow(RsCode.CLINIC_SESSION_NOT_FOUND::toException);
    }

    private StudentCourseRecord loadActiveRecord(UUID recordId) {
        if (recordId == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        StudentCourseRecord record = studentCourseRecordRepository.findById(recordId)
                .orElseThrow(RsCode.STUDENT_COURSE_RECORD_NOT_FOUND::toException);
        if (record.isDeleted()) {
            throw new BusinessException(RsCode.STUDENT_COURSE_RECORD_NOT_FOUND);
        }
        return record;
    }

    private Course loadCourse(UUID courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(RsCode.COURSE_NOT_FOUND::toException);
    }

    private List<ClinicAttendance> loadStudentAttendances(MemberPrincipal principal,
                                                          LocalDate startDate,
                                                          LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        List<StudentCourseRecord> records = studentCourseRecordRepository.findByStudentMemberIdAndDeletedAtIsNull(
                principal.id()
        );
        List<UUID> recordIds = records.stream().map(StudentCourseRecord::getId).toList();
        if (recordIds.isEmpty()) {
            return List.of();
        }
        return clinicAttendanceRepository.findByStudentCourseRecordIdInAndDateRange(recordIds, startDate, endDate);
    }

    private void ensureStaffAccess(MemberPrincipal principal, ClinicSession session) {
        if (principal.role() == MemberRole.TEACHER) {
            if (!Objects.equals(principal.id(), session.getTeacherMemberId())) {
                throw new BusinessException(RsCode.FORBIDDEN);
            }
            return;
        }
        if (principal.role() == MemberRole.ASSISTANT) {
            boolean assigned = teacherAssistantAssignmentRepository
                    .findByTeacherMemberIdAndAssistantMemberIdAndDeletedAtIsNull(
                            session.getTeacherMemberId(),
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

    private void ensureStudentRole(MemberPrincipal principal) {
        if (principal.role() != MemberRole.STUDENT) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
    }

    private void ensureRecordMatchesSession(Course course, ClinicSession session) {
        if (!Objects.equals(course.getTeacherMemberId(), session.getTeacherMemberId())
                || !Objects.equals(course.getBranchId(), session.getBranchId())) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
    }

    private void ensureAttendanceCreatable(ClinicSession session, UUID recordId) {
        ensureSessionActive(session);
        ensureSessionNotLocked(session);
        if (clinicAttendanceRepository.existsByClinicSessionIdAndStudentCourseRecordId(session.getId(), recordId)) {
            throw new BusinessException(RsCode.CLINIC_ATTENDANCE_DUPLICATED);
        }
        long currentCount = clinicAttendanceRepository.countByClinicSessionId(session.getId());
        if (currentCount >= session.getCapacity()) {
            throw new BusinessException(RsCode.CLINIC_SESSION_FULL);
        }
        long overlap = clinicAttendanceRepository.countOverlappingAttendances(
                recordId,
                session.getDate(),
                session.getStartTime(),
                session.getEndTime()
        );
        if (overlap > 0) {
            throw new BusinessException(RsCode.CLINIC_ATTENDANCE_TIME_OVERLAP);
        }
    }

    private void ensureSessionWritable(ClinicSession session) {
        ensureSessionActive(session);
        ensureSessionNotLocked(session);
    }

    private void ensureSessionActive(ClinicSession session) {
        if (session.isCanceled()) {
            throw new BusinessException(RsCode.CLINIC_SESSION_CANCELED);
        }
    }

    private void ensureSessionNotLocked(ClinicSession session) {
        if (ClinicAttendancePolicy.isLocked(session, LocalDateTime.now())) {
            throw new BusinessException(RsCode.CLINIC_ATTENDANCE_LOCKED);
        }
    }

    private boolean isSameWeek(ClinicSession fromSession, ClinicSession toSession) {
        var week = ClinicAttendancePolicy.resolveWeek(fromSession.getDate());
        return !toSession.getDate().isBefore(week.startDate())
                && !toSession.getDate().isAfter(week.endDate());
    }

    private ClinicAttendance saveAttendance(UUID sessionId, UUID recordId) {
        ClinicAttendance attendance = ClinicAttendance.builder()
                .clinicSessionId(sessionId)
                .studentCourseRecordId(recordId)
                .build();
        return clinicAttendanceRepository.save(attendance);
    }

    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
