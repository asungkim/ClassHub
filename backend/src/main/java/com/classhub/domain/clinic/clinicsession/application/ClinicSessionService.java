package com.classhub.domain.clinic.clinicsession.application;

import com.classhub.domain.assignment.model.TeacherBranchAssignment;
import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.assignment.repository.TeacherBranchAssignmentRepository;
import com.classhub.domain.clinic.clinicsession.dto.request.ClinicSessionEmergencyCreateRequest;
import com.classhub.domain.clinic.clinicsession.model.ClinicSession;
import com.classhub.domain.clinic.clinicsession.model.ClinicSessionType;
import com.classhub.domain.clinic.clinicsession.repository.ClinicSessionRepository;
import com.classhub.domain.clinic.clinicslot.model.ClinicSlot;
import com.classhub.domain.clinic.clinicslot.repository.ClinicSlotRepository;
import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.branch.repository.BranchRepository;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ClinicSessionService {

    private final ClinicSessionRepository clinicSessionRepository;
    private final ClinicSlotRepository clinicSlotRepository;
    private final TeacherBranchAssignmentRepository teacherBranchAssignmentRepository;
    private final TeacherAssistantAssignmentRepository teacherAssistantAssignmentRepository;
    private final BranchRepository branchRepository;
    private final StudentCourseRecordRepository studentCourseRecordRepository;

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
    public List<ClinicSession> getSessionsForTeacher(
            UUID teacherId,
            UUID branchId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDateRange(branchId, startDate, endDate);
        Branch branch = requireVerifiedBranch(branchId);
        ensureTeacherAssignment(teacherId, branch.getId());
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
        ensureTeacherAssignment(teacherId, branch.getId());
        ensureAssistantAssignment(assistantId, teacherId);
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
        long count = studentCourseRecordRepository.countActiveByStudentAndTeacherAndBranch(
                studentId,
                teacherId,
                branchId
        );
        if (count == 0L) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
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
        ensureTeacherAssignment(teacherId, branch.getId());
        if (principal.role() == MemberRole.ASSISTANT) {
            ensureAssistantAssignment(principal.id(), teacherId);
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

    private void ensureTeacherAssignment(UUID teacherId, UUID branchId) {
        TeacherBranchAssignment assignment = teacherBranchAssignmentRepository
                .findByTeacherMemberIdAndBranchId(teacherId, branchId)
                .orElseThrow(() -> new BusinessException(RsCode.FORBIDDEN));
        if (!assignment.isActive()) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
    }

    private void ensureAssistantAssignment(UUID assistantId, UUID teacherId) {
        boolean assigned = teacherAssistantAssignmentRepository
                .findByTeacherMemberIdAndAssistantMemberIdAndDeletedAtIsNull(teacherId, assistantId)
                .isPresent();
        if (!assigned) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
    }

    private void ensureCancelPermission(MemberPrincipal principal, ClinicSession session) {
        if (principal.role() == MemberRole.TEACHER) {
            if (!Objects.equals(principal.id(), session.getTeacherMemberId())) {
                throw new BusinessException(RsCode.FORBIDDEN);
            }
            return;
        }
        if (principal.role() == MemberRole.ASSISTANT) {
            ensureAssistantAssignment(principal.id(), session.getTeacherMemberId());
            return;
        }
        throw new BusinessException(RsCode.FORBIDDEN);
    }

    private boolean canCancel(ClinicSession session) {
        LocalDateTime sessionStart = LocalDateTime.of(session.getDate(), session.getStartTime());
        return LocalDateTime.now().isBefore(sessionStart);
    }
}
