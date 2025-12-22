package com.classhub.domain.clinic.clinicbatch.application;

import com.classhub.domain.clinic.clinicattendance.support.ClinicAttendancePolicy;
import com.classhub.domain.clinic.clinicattendance.model.ClinicAttendance;
import com.classhub.domain.clinic.clinicattendance.repository.ClinicAttendanceRepository;
import com.classhub.domain.clinic.clinicsession.model.ClinicSession;
import com.classhub.domain.clinic.clinicsession.model.ClinicSessionType;
import com.classhub.domain.clinic.clinicsession.repository.ClinicSessionRepository;
import com.classhub.domain.clinic.clinicslot.model.ClinicSlot;
import com.classhub.domain.clinic.clinicslot.repository.ClinicSlotRepository;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ClinicBatchService {

    private final ClinicSlotRepository clinicSlotRepository;
    private final ClinicSessionRepository clinicSessionRepository;
    private final StudentCourseRecordRepository studentCourseRecordRepository;
    private final ClinicAttendanceRepository clinicAttendanceRepository;

    public List<ClinicSession> generateWeeklySessions(LocalDate baseDate) {
        if (baseDate == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        ClinicAttendancePolicy.WeekRange weekRange = ClinicAttendancePolicy.resolveWeek(baseDate);
        List<ClinicSession> created = new ArrayList<>();
        for (ClinicSlot slot : clinicSlotRepository.findByDeletedAtIsNull()) {
            if (!isSlotValid(slot)) {
                continue;
            }
            LocalDate sessionDate = resolveSessionDate(weekRange, slot.getDayOfWeek());
            if (sessionDate == null) {
                continue;
            }
            boolean exists = clinicSessionRepository
                    .findBySlotIdAndDateAndDeletedAtIsNull(slot.getId(), sessionDate)
                    .isPresent();
            if (exists) {
                continue;
            }
            ClinicSession session = ClinicSession.builder()
                    .slotId(slot.getId())
                    .teacherMemberId(slot.getTeacherMemberId())
                    .branchId(slot.getBranchId())
                    .sessionType(ClinicSessionType.REGULAR)
                    .creatorMemberId(null)
                    .date(sessionDate)
                    .startTime(slot.getStartTime())
                    .endTime(slot.getEndTime())
                    .capacity(slot.getDefaultCapacity())
                    .canceled(false)
                    .build();
            try {
                created.add(clinicSessionRepository.save(session));
            } catch (ObjectOptimisticLockingFailureException | DataIntegrityViolationException ex) {
                log.warn("Clinic batch session skip: slotId={}, date={}, reason={}",
                        slot.getId(),
                        sessionDate,
                        ex.getClass().getSimpleName());
            }
        }
        return created;
    }

    public List<ClinicAttendance> generateWeeklyAttendances(LocalDate baseDate) {
        if (baseDate == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        ClinicAttendancePolicy.WeekRange weekRange = ClinicAttendancePolicy.resolveWeek(baseDate);
        List<ClinicAttendance> created = new ArrayList<>();
        for (ClinicSlot slot : clinicSlotRepository.findByDeletedAtIsNull()) {
            if (!isSlotValid(slot)) {
                continue;
            }
            LocalDate sessionDate = resolveSessionDate(weekRange, slot.getDayOfWeek());
            if (sessionDate == null) {
                continue;
            }
            ClinicSession session = clinicSessionRepository
                    .findBySlotIdAndDateAndDeletedAtIsNull(slot.getId(), sessionDate)
                    .orElse(null);
            if (session == null || session.isCanceled()) {
                continue;
            }
            created.addAll(createAttendancesForSession(session, slot.getId()));
        }
        return created;
    }

    public List<ClinicSession> generateRemainingSessionsForSlot(ClinicSlot slot, LocalDateTime now) {
        if (slot == null || now == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        if (!isSlotValid(slot)) {
            return List.of();
        }
        ClinicAttendancePolicy.WeekRange weekRange = ClinicAttendancePolicy.resolveWeek(now.toLocalDate());
        LocalDate sessionDate = resolveSessionDate(weekRange, slot.getDayOfWeek());
        if (sessionDate == null) {
            return List.of();
        }
        if (isSessionTimePassed(sessionDate, slot.getStartTime(), now)) {
            return List.of();
        }
        boolean exists = clinicSessionRepository
                .findBySlotIdAndDateAndDeletedAtIsNull(slot.getId(), sessionDate)
                .isPresent();
        if (exists) {
            return List.of();
        }
        ClinicSession session = ClinicSession.builder()
                .slotId(slot.getId())
                .teacherMemberId(slot.getTeacherMemberId())
                .branchId(slot.getBranchId())
                .sessionType(ClinicSessionType.REGULAR)
                .creatorMemberId(null)
                .date(sessionDate)
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .capacity(slot.getDefaultCapacity())
                .canceled(false)
                .build();
        try {
            ClinicSession saved = clinicSessionRepository.save(session);
            createAttendancesForSession(saved, slot.getId());
            return List.of(saved);
        } catch (ObjectOptimisticLockingFailureException | DataIntegrityViolationException ex) {
            log.warn("Clinic batch session skip: slotId={}, date={}, reason={}",
                    slot.getId(),
                    sessionDate,
                    ex.getClass().getSimpleName());
            return List.of();
        }
    }

    private boolean isSlotValid(ClinicSlot slot) {
        if (slot == null
                || slot.getDayOfWeek() == null
                || slot.getStartTime() == null
                || slot.getEndTime() == null
                || slot.getDefaultCapacity() == null) {
            return false;
        }
        if (slot.getDefaultCapacity() < 1) {
            return false;
        }
        return slot.getStartTime().isBefore(slot.getEndTime());
    }

    private boolean isSessionTimePassed(LocalDate sessionDate, LocalTime startTime, LocalDateTime now) {
        if (sessionDate.isBefore(now.toLocalDate())) {
            return true;
        }
        if (sessionDate.isAfter(now.toLocalDate())) {
            return false;
        }
        return !startTime.isAfter(now.toLocalTime());
    }

    private List<ClinicAttendance> createAttendancesForSession(ClinicSession session, UUID slotId) {
        List<ClinicAttendance> created = new ArrayList<>();
        List<StudentCourseRecord> records = studentCourseRecordRepository
                .findByDefaultClinicSlotIdAndDeletedAtIsNull(slotId);
        long currentCount = clinicAttendanceRepository.countByClinicSessionId(session.getId());
        for (StudentCourseRecord record : records) {
            if (currentCount >= session.getCapacity()) {
                break;
            }
            if (clinicAttendanceRepository.existsByClinicSessionIdAndStudentCourseRecordId(
                    session.getId(),
                    record.getId()
            )) {
                continue;
            }
            long overlap = clinicAttendanceRepository.countOverlappingAttendances(
                    record.getId(),
                    session.getDate(),
                    session.getStartTime(),
                    session.getEndTime()
            );
            if (overlap > 0) {
                continue;
            }
            ClinicAttendance attendance = ClinicAttendance.builder()
                    .clinicSessionId(session.getId())
                    .studentCourseRecordId(record.getId())
                    .build();
            try {
                created.add(clinicAttendanceRepository.save(attendance));
                currentCount++;
            } catch (ObjectOptimisticLockingFailureException | DataIntegrityViolationException ex) {
                log.warn("Clinic batch attendance skip: sessionId={}, recordId={}, reason={}",
                        session.getId(),
                        record.getId(),
                        ex.getClass().getSimpleName());
            }
        }
        return created;
    }

    private LocalDate resolveSessionDate(ClinicAttendancePolicy.WeekRange weekRange, DayOfWeek dayOfWeek) {
        if (weekRange == null || dayOfWeek == null) {
            return null;
        }
        LocalDate sessionDate = weekRange.startDate().with(TemporalAdjusters.nextOrSame(dayOfWeek));
        if (sessionDate.isAfter(weekRange.endDate())) {
            return null;
        }
        return sessionDate;
    }
}
