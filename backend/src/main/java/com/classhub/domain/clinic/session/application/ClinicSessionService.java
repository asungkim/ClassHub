package com.classhub.domain.clinic.session.application;

import com.classhub.domain.clinic.attendance.repository.ClinicAttendanceRepository;
import com.classhub.domain.clinic.attendance.repository.ClinicAttendanceCountProjection;
import com.classhub.domain.clinic.permission.application.ClinicPermissionValidator;
import com.classhub.domain.clinic.session.dto.request.ClinicSessionEmergencyCreateRequest;
import com.classhub.domain.clinic.session.dto.response.ClinicSessionResponse;
import com.classhub.domain.clinic.session.model.ClinicSession;
import com.classhub.domain.clinic.session.model.ClinicSessionType;
import com.classhub.domain.clinic.session.repository.ClinicSessionRepository;
import com.classhub.domain.clinic.slot.model.ClinicSlot;
import com.classhub.domain.clinic.slot.repository.ClinicSlotRepository;
import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.branch.repository.BranchRepository;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import com.classhub.global.util.KstTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
public class ClinicSessionService {

    private final ClinicSessionRepository clinicSessionRepository;
    private final ClinicSlotRepository clinicSlotRepository;
    private final BranchRepository branchRepository;
    private final ClinicPermissionValidator clinicPermissionValidator;
    private final ClinicAttendanceRepository clinicAttendanceRepository;

    public ClinicSession createRegularSession(UUID teacherId, UUID slotId, LocalDate date) {
        if (slotId == null || date == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        ClinicSlot slot = clinicSlotRepository.findByIdAndDeletedAtIsNull(slotId)
                .orElseThrow(RsCode.CLINIC_SLOT_NOT_FOUND::toException);
        if (!Objects.equals(slot.getTeacherMemberId(), teacherId)) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        if (clinicSessionRepository.findBySlotIdAndDateAndDeletedAtIsNull(slotId, date).isPresent()) {
            throw new BusinessException(RsCode.CLINIC_SESSION_ALREADY_EXISTS);
        }

        ClinicSession session = ClinicSession.builder()
                .slotId(slot.getId())
                .teacherMemberId(slot.getTeacherMemberId())
                .branchId(slot.getBranchId())
                .sessionType(ClinicSessionType.REGULAR)
                .creatorMemberId(null)
                .date(date)
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .capacity(slot.getDefaultCapacity())
                .canceled(false)
                .build();
        return clinicSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public List<ClinicSessionResponse> getSessions(MemberPrincipal principal,
                                                   UUID teacherId,
                                                   UUID branchId,
                                                   LocalDate startDate,
                                                   LocalDate endDate) {
        List<ClinicSession> sessions;
        if (principal.role() == MemberRole.TEACHER) {
            sessions = getSessionsForTeacher(principal.id(), branchId, startDate, endDate);
        } else if (principal.role() == MemberRole.ASSISTANT) {
            sessions = getSessionsForAssistant(principal.id(), teacherId, branchId, startDate, endDate);
        } else if (principal.role() == MemberRole.STUDENT) {
            sessions = getSessionsForStudent(principal.id(), teacherId, branchId, startDate, endDate);
        } else {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        if (sessions.isEmpty()) {
            return List.of();
        }
        Map<UUID, Integer> attendanceCounts = clinicAttendanceRepository
                .findAttendanceCountsByClinicSessionIds(
                        sessions.stream().map(ClinicSession::getId).toList()
                )
                .stream()
                .collect(Collectors.toMap(
                        ClinicAttendanceCountProjection::getClinicSessionId,
                        projection -> projection.getAttendanceCount().intValue()
                ));
        return sessions.stream()
                .map(session -> ClinicSessionResponse.from(
                        session,
                        attendanceCounts.getOrDefault(session.getId(), 0)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClinicSession> getSessionsForTeacher(
            UUID teacherId,
            UUID branchId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDateRange(branchId, startDate, endDate);
        Branch branch = requireVerifiedBranch(branchId);
        clinicPermissionValidator.ensureTeacherAssignment(teacherId, branch.getId());
        return clinicSessionRepository.findByTeacherMemberIdAndBranchIdAndDateRange(
                teacherId,
                branch.getId(),
                startDate,
                endDate
        );
    }

    @Transactional(readOnly = true)
    public List<ClinicSession> getSessionsForAssistant(
            UUID assistantId,
            UUID teacherId,
            UUID branchId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (teacherId == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        validateDateRange(branchId, startDate, endDate);
        Branch branch = requireVerifiedBranch(branchId);
        clinicPermissionValidator.ensureTeacherAssignment(teacherId, branch.getId());
        clinicPermissionValidator.ensureAssistantAssignment(assistantId, teacherId);
        return clinicSessionRepository.findByTeacherMemberIdAndBranchIdAndDateRange(
                teacherId,
                branch.getId(),
                startDate,
                endDate
        );
    }

    @Transactional(readOnly = true)
    public List<ClinicSession> getSessionsForStudent(
            UUID studentId,
            UUID teacherId,
            UUID branchId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (teacherId == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        validateDateRange(branchId, startDate, endDate);
        clinicPermissionValidator.ensureStudentAccess(studentId, teacherId, branchId);
        return clinicSessionRepository.findByTeacherMemberIdAndBranchIdAndDateRange(
                teacherId,
                branchId,
                startDate,
                endDate
        );
    }

    public ClinicSession createEmergencySession(MemberPrincipal principal, ClinicSessionEmergencyCreateRequest request) {
        validateEmergencyRequest(request);
        UUID teacherId = resolveTeacherId(principal, request.teacherId());
        Branch branch = requireVerifiedBranch(request.branchId());
        clinicPermissionValidator.ensureTeacherAssignment(teacherId, branch.getId());
        if (principal.role() == MemberRole.ASSISTANT) {
            clinicPermissionValidator.ensureAssistantAssignment(principal.id(), teacherId);
        }

        ClinicSession session = ClinicSession.builder()
                .slotId(null)
                .teacherMemberId(teacherId)
                .branchId(branch.getId())
                .sessionType(ClinicSessionType.EMERGENCY)
                .creatorMemberId(principal.id())
                .date(request.date())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .capacity(request.capacity())
                .canceled(false)
                .build();
        return clinicSessionRepository.save(session);
    }

    public void cancelSession(MemberPrincipal principal, UUID sessionId) {
        if (sessionId == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        ClinicSession session = clinicSessionRepository.findByIdAndDeletedAtIsNull(sessionId)
                .orElseThrow(RsCode.CLINIC_SESSION_NOT_FOUND::toException);
        ensureCancelPermission(principal, session);
        if (!canCancel(session)) {
            throw new BusinessException(RsCode.CLINIC_SESSION_CANCEL_FORBIDDEN);
        }
        session.cancel();
        clinicSessionRepository.save(session);
    }

    private void validateEmergencyRequest(ClinicSessionEmergencyCreateRequest request) {
        if (request == null
                || request.branchId() == null
                || request.date() == null
                || request.startTime() == null
                || request.endTime() == null
                || request.capacity() == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        validateTimeRange(request.startTime(), request.endTime(), request.capacity());
    }

    private void validateDateRange(UUID branchId, LocalDate startDate, LocalDate endDate) {
        if (branchId == null || startDate == null || endDate == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime, Integer capacity) {
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException(RsCode.CLINIC_SESSION_TIME_INVALID);
        }
        if (capacity < 1) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
    }

    private UUID resolveTeacherId(MemberPrincipal principal, UUID requestedTeacherId) {
        if (principal.role() == MemberRole.TEACHER) {
            if (requestedTeacherId != null && !requestedTeacherId.equals(principal.id())) {
                throw new BusinessException(RsCode.BAD_REQUEST);
            }
            return principal.id();
        }
        if (principal.role() == MemberRole.ASSISTANT) {
            if (requestedTeacherId == null) {
                throw new BusinessException(RsCode.BAD_REQUEST);
            }
            return requestedTeacherId;
        }
        throw new BusinessException(RsCode.FORBIDDEN);
    }

    private Branch requireVerifiedBranch(UUID branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BusinessException(RsCode.BRANCH_NOT_FOUND));
        if (branch.isDeleted() || branch.getVerifiedStatus() != VerifiedStatus.VERIFIED) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        return branch;
    }

    private void ensureCancelPermission(MemberPrincipal principal, ClinicSession session) {
        if (principal.role() == MemberRole.TEACHER) {
            if (!Objects.equals(principal.id(), session.getTeacherMemberId())) {
                throw new BusinessException(RsCode.FORBIDDEN);
            }
            return;
        }
        if (principal.role() == MemberRole.ASSISTANT) {
            clinicPermissionValidator.ensureAssistantAssignment(principal.id(), session.getTeacherMemberId());
            return;
        }
        throw new BusinessException(RsCode.FORBIDDEN);
    }

    private boolean canCancel(ClinicSession session) {
        LocalDateTime sessionStart = LocalDateTime.of(session.getDate(), session.getStartTime());
        return LocalDateTime.now(KstTime.clock()).isBefore(sessionStart);
    }
}
