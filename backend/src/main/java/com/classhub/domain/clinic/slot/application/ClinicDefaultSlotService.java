package com.classhub.domain.clinic.slot.application;

import com.classhub.domain.clinic.attendance.model.ClinicAttendance;
import com.classhub.domain.clinic.attendance.repository.ClinicAttendanceRepository;
import com.classhub.domain.clinic.attendance.support.ClinicAttendancePolicy;
import com.classhub.domain.clinic.session.model.ClinicSession;
import com.classhub.domain.clinic.session.repository.ClinicSessionRepository;
import com.classhub.domain.clinic.slot.model.ClinicSlot;
import com.classhub.domain.clinic.slot.repository.ClinicSlotRepository;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import com.classhub.global.util.KstTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ClinicDefaultSlotService {

    private final StudentCourseRecordRepository recordRepository;
    private final CourseRepository courseRepository;
    private final ClinicSlotRepository clinicSlotRepository;
    private final ClinicSessionRepository clinicSessionRepository;
    private final ClinicAttendanceRepository clinicAttendanceRepository;

    public StudentCourseRecord updateDefaultSlotForStudent(UUID studentId, UUID courseId, UUID defaultSlotId) {
        if (defaultSlotId == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        StudentCourseRecord record = recordRepository
                .findByStudentMemberIdAndCourseIdAndDeletedAtIsNull(studentId, courseId)
                .orElseThrow(RsCode.STUDENT_COURSE_RECORD_NOT_FOUND::toException);
        Course course = courseRepository.findById(record.getCourseId())
                .orElseThrow(RsCode.COURSE_NOT_FOUND::toException);
        return applyDefaultSlot(record, course, defaultSlotId);
    }

    public StudentCourseRecord applyDefaultSlot(StudentCourseRecord record, Course course, UUID defaultSlotId) {
        if (defaultSlotId == null) {
            record.updateDefaultClinicSlot(null);
            return record;
        }
        if (Objects.equals(defaultSlotId, record.getDefaultClinicSlotId())) {
            throw new BusinessException(RsCode.CLINIC_SLOT_DUPLICATED);
        }
        ClinicSlot slot = clinicSlotRepository.findByIdAndDeletedAtIsNullForUpdate(defaultSlotId)
                .orElseThrow(RsCode.CLINIC_SLOT_NOT_FOUND::toException);
        ensureSlotMatchesCourse(slot, course);

        List<StudentCourseRecord> studentRecords = recordRepository
                .findByStudentMemberIdAndDeletedAtIsNull(record.getStudentMemberId());
        ensureSlotNotDuplicated(record, defaultSlotId, studentRecords);
        ensureSlotNotOverlapping(slot, record, studentRecords);
        ensureSlotCapacity(slot, defaultSlotId);

        boolean wasUnset = record.getDefaultClinicSlotId() == null;
        record.updateDefaultClinicSlot(defaultSlotId);
        if (wasUnset && !course.isDeleted()) {
            createAttendancesForCurrentWeek(record, slot);
        }
        return record;
    }

    public void createAttendancesForCurrentWeekIfPossible(StudentCourseRecord record, Course course) {
        if (record == null || course == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        if (record.isDeleted() || course.isDeleted()) {
            return;
        }
        UUID slotId = record.getDefaultClinicSlotId();
        if (slotId == null) {
            return;
        }
        ClinicSlot slot = clinicSlotRepository.findByIdAndDeletedAtIsNull(slotId).orElse(null);
        if (slot == null) {
            return;
        }
        if (!Objects.equals(slot.getTeacherMemberId(), course.getTeacherMemberId())
                || !Objects.equals(slot.getBranchId(), course.getBranchId())) {
            return;
        }
        createAttendancesForCurrentWeek(record, slot);
    }

    private void ensureSlotMatchesCourse(ClinicSlot slot, Course course) {
        if (!Objects.equals(slot.getTeacherMemberId(), course.getTeacherMemberId())
                || !Objects.equals(slot.getBranchId(), course.getBranchId())) {
            throw new BusinessException(RsCode.CLINIC_SLOT_NOT_FOUND);
        }
    }

    private void ensureSlotNotDuplicated(StudentCourseRecord record,
                                         UUID slotId,
                                         List<StudentCourseRecord> studentRecords) {
        boolean duplicated = studentRecords.stream()
                .filter(other -> !other.getId().equals(record.getId()))
                .anyMatch(other -> slotId.equals(other.getDefaultClinicSlotId()));
        if (duplicated) {
            throw new BusinessException(RsCode.CLINIC_SLOT_DUPLICATED);
        }
    }

    private void ensureSlotNotOverlapping(ClinicSlot slot,
                                          StudentCourseRecord record,
                                          List<StudentCourseRecord> studentRecords) {
        List<UUID> slotIds = studentRecords.stream()
                .filter(other -> !other.getId().equals(record.getId()))
                .map(StudentCourseRecord::getDefaultClinicSlotId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (slotIds.isEmpty()) {
            return;
        }
        Map<UUID, ClinicSlot> slotMap = clinicSlotRepository.findByIdInAndDeletedAtIsNull(slotIds).stream()
                .collect(Collectors.toMap(ClinicSlot::getId, Function.identity()));
        boolean overlapped = slotIds.stream()
                .map(slotMap::get)
                .filter(Objects::nonNull)
                .anyMatch(existing -> isOverlapping(slot, existing));
        if (overlapped) {
            throw new BusinessException(RsCode.CLINIC_SLOT_TIME_OVERLAP);
        }
    }

    private void ensureSlotCapacity(ClinicSlot slot, UUID slotId) {
        long assignedCount = recordRepository.countByDefaultClinicSlotIdAndDeletedAtIsNull(slotId);
        if (assignedCount >= slot.getDefaultCapacity()) {
            throw new BusinessException(RsCode.CLINIC_SLOT_CAPACITY_EXCEEDED);
        }
    }

    private boolean isOverlapping(ClinicSlot slot, ClinicSlot other) {
        if (slot.getDayOfWeek() != other.getDayOfWeek()) {
            return false;
        }
        return slot.getStartTime().isBefore(other.getEndTime())
                && other.getStartTime().isBefore(slot.getEndTime());
    }

    private void createAttendancesForCurrentWeek(StudentCourseRecord record, ClinicSlot slot) {
        LocalDate today = LocalDate.now(KstTime.clock());
        ClinicAttendancePolicy.WeekRange weekRange = ClinicAttendancePolicy.resolveWeek(today);
        List<ClinicSession> sessions = clinicSessionRepository.findBySlotIdAndDateRange(
                slot.getId(),
                weekRange.startDate(),
                weekRange.endDate()
        );
        LocalDateTime now = LocalDateTime.now(KstTime.clock());
        for (ClinicSession session : sessions) {
            if (session.isCanceled()) {
                continue;
            }
            LocalDateTime sessionStart = LocalDateTime.of(session.getDate(), session.getStartTime());
            if (!sessionStart.isAfter(now)) {
                continue;
            }
            if (clinicAttendanceRepository.existsByClinicSessionIdAndStudentCourseRecordId(
                    session.getId(),
                    record.getId()
            )) {
                continue;
            }
            ClinicAttendance attendance = ClinicAttendance.builder()
                    .clinicSessionId(session.getId())
                    .studentCourseRecordId(record.getId())
                    .build();
            clinicAttendanceRepository.save(attendance);
        }
    }
}
