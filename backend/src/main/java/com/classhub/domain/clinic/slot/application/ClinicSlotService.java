package com.classhub.domain.clinic.slot.application;

import com.classhub.domain.clinic.permission.application.ClinicPermissionValidator;
import com.classhub.domain.clinic.batch.application.ClinicBatchService;
import com.classhub.domain.clinic.slot.dto.request.ClinicSlotCreateRequest;
import com.classhub.domain.clinic.slot.dto.request.ClinicSlotUpdateRequest;
import com.classhub.domain.clinic.slot.model.ClinicSlot;
import com.classhub.domain.clinic.slot.repository.ClinicSlotRepository;
import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.branch.repository.BranchRepository;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import com.classhub.global.util.KstTime;
import com.classhub.global.validator.ScheduleTimeRangeValidator;
import java.time.DayOfWeek;
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
public class ClinicSlotService {

    private final ClinicSlotRepository clinicSlotRepository;
    private final StudentCourseRecordRepository studentCourseRecordRepository;
    private final BranchRepository branchRepository;
    private final CourseRepository courseRepository;
    private final ClinicBatchService clinicBatchService;
    private final ClinicPermissionValidator clinicPermissionValidator;

    public ClinicSlot createSlot(UUID teacherId, ClinicSlotCreateRequest request) {
        validateCreateRequest(request);
        Branch branch = requireVerifiedBranch(request.branchId());
        clinicPermissionValidator.ensureTeacherAssignment(teacherId, branch.getId());
        ensureSlotNotOverlapping(
                teacherId,
                branch.getId(),
                request.dayOfWeek(),
                request.startTime(),
                request.endTime(),
                null
        );

        ClinicSlot slot = ClinicSlot.builder()
                .teacherMemberId(teacherId)
                .creatorMemberId(teacherId)
                .branchId(branch.getId())
                .dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .defaultCapacity(request.defaultCapacity())
                .build();

        ClinicSlot saved = clinicSlotRepository.save(slot);
        clinicBatchService.generateRemainingSessionsForSlot(saved, LocalDateTime.now(KstTime.clock()));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ClinicSlot> getSlots(MemberPrincipal principal,
                                     UUID branchId,
                                     UUID teacherId,
                                     UUID courseId) {
        if (principal.role() == MemberRole.TEACHER) {
            return getSlotsForTeacher(principal.id(), branchId);
        }
        if (principal.role() == MemberRole.ASSISTANT) {
            return getSlotsForAssistant(principal.id(), teacherId, branchId);
        }
        if (principal.role() == MemberRole.STUDENT) {
            return getSlotsForStudent(principal.id(), courseId);
        }
        throw new BusinessException(RsCode.FORBIDDEN);
    }

    @Transactional(readOnly = true)
    public List<ClinicSlot> getSlotsForTeacher(UUID teacherId, UUID branchId) {
        if (branchId == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        Branch branch = requireVerifiedBranch(branchId);
        clinicPermissionValidator.ensureTeacherAssignment(teacherId, branch.getId());
        return clinicSlotRepository.findByTeacherMemberIdAndBranchIdAndDeletedAtIsNull(teacherId, branchId);
    }

    @Transactional(readOnly = true)
    public List<ClinicSlot> getSlotsForAssistant(UUID assistantId, UUID teacherId, UUID branchId) {
        if (teacherId == null || branchId == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        clinicPermissionValidator.ensureAssistantAssignment(assistantId, teacherId);
        return clinicSlotRepository.findByTeacherMemberIdAndBranchIdAndDeletedAtIsNull(teacherId, branchId);
    }

    @Transactional(readOnly = true)
    public List<ClinicSlot> getSlotsForStudent(UUID studentId, UUID courseId) {
        if (courseId == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        StudentCourseRecord record = studentCourseRecordRepository
                .findByStudentMemberIdAndCourseIdAndDeletedAtIsNull(studentId, courseId)
                .orElseThrow(RsCode.STUDENT_COURSE_RECORD_NOT_FOUND::toException);
        Course course = courseRepository.findById(record.getCourseId())
                .orElseThrow(RsCode.COURSE_NOT_FOUND::toException);
        return clinicSlotRepository.findByTeacherMemberIdAndBranchIdAndDeletedAtIsNull(
                course.getTeacherMemberId(),
                course.getBranchId()
        );
    }

    public ClinicSlot updateSlot(UUID teacherId, UUID slotId, ClinicSlotUpdateRequest request) {
        validateUpdateRequest(request);
        ClinicSlot slot = clinicSlotRepository.findByIdAndDeletedAtIsNull(slotId)
                .orElseThrow(RsCode.CLINIC_SLOT_NOT_FOUND::toException);
        if (!Objects.equals(slot.getTeacherMemberId(), teacherId)) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }

        Integer previousCapacity = slot.getDefaultCapacity();
        boolean scheduleChanged = isScheduleChanged(
                slot,
                request.dayOfWeek(),
                request.startTime(),
                request.endTime()
        );
        boolean capacityChanged = !Objects.equals(previousCapacity, request.defaultCapacity());

        if (scheduleChanged) {
            ensureSlotNotOverlapping(
                    teacherId,
                    slot.getBranchId(),
                    request.dayOfWeek(),
                    request.startTime(),
                    request.endTime(),
                    slotId
            );
        }

        if (!scheduleChanged) {
            long assignedCount = studentCourseRecordRepository
                    .countByDefaultClinicSlotIdAndDeletedAtIsNull(slotId);
            if (request.defaultCapacity() < assignedCount) {
                throw new BusinessException(RsCode.CLINIC_SLOT_CAPACITY_CONFLICT);
            }
        }

        slot.updateSchedule(
                request.dayOfWeek(),
                request.startTime(),
                request.endTime(),
                request.defaultCapacity()
        );

        ClinicSlot saved = clinicSlotRepository.save(slot);
        return saved;
    }

    public void deleteSlot(UUID teacherId, UUID slotId) {
        ClinicSlot slot = clinicSlotRepository.findByIdAndDeletedAtIsNull(slotId)
                .orElseThrow(RsCode.CLINIC_SLOT_NOT_FOUND::toException);
        if (!Objects.equals(slot.getTeacherMemberId(), teacherId)) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        slot.delete();
        studentCourseRecordRepository.clearDefaultClinicSlotId(slotId);
        clinicSlotRepository.save(slot);
    }

    private void validateCreateRequest(ClinicSlotCreateRequest request) {
        if (request == null
                || request.branchId() == null
                || request.dayOfWeek() == null
                || request.startTime() == null
                || request.endTime() == null
                || request.defaultCapacity() == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        validateTimeRange(request.dayOfWeek(), request.startTime(), request.endTime(), request.defaultCapacity());
    }

    private void validateUpdateRequest(ClinicSlotUpdateRequest request) {
        if (request == null
                || request.dayOfWeek() == null
                || request.startTime() == null
                || request.endTime() == null
                || request.defaultCapacity() == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        validateTimeRange(request.dayOfWeek(), request.startTime(), request.endTime(), request.defaultCapacity());
    }

    private void validateTimeRange(
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Integer defaultCapacity
    ) {
        ScheduleTimeRangeValidator.validate(startTime, endTime, RsCode.CLINIC_SLOT_TIME_INVALID);
        if (defaultCapacity < 1) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
    }

    private Branch requireVerifiedBranch(UUID branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BusinessException(RsCode.BRANCH_NOT_FOUND));
        if (branch.isDeleted() || branch.getVerifiedStatus() != VerifiedStatus.VERIFIED) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        return branch;
    }

    private boolean isScheduleChanged(
            ClinicSlot slot,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {
        return slot.getDayOfWeek() != dayOfWeek
                || !slot.getStartTime().equals(startTime)
                || !slot.getEndTime().equals(endTime);
    }

    private void ensureSlotNotOverlapping(UUID teacherId,
                                          UUID branchId,
                                          DayOfWeek dayOfWeek,
                                          LocalTime startTime,
                                          LocalTime endTime,
                                          UUID excludeId) {
        List<ClinicSlot> slots = clinicSlotRepository
                .findByTeacherMemberIdAndBranchIdAndDeletedAtIsNull(teacherId, branchId);
        boolean overlapped = slots.stream()
                .filter(slot -> excludeId == null || !slot.getId().equals(excludeId))
                .anyMatch(slot -> isOverlapping(slot, dayOfWeek, startTime, endTime));
        if (overlapped) {
            throw new BusinessException(RsCode.CLINIC_SLOT_CONFLICT);
        }
    }

    private boolean isOverlapping(ClinicSlot slot,
                                  DayOfWeek dayOfWeek,
                                  LocalTime startTime,
                                  LocalTime endTime) {
        if (slot.getDayOfWeek() != dayOfWeek) {
            return false;
        }
        return slot.getStartTime().isBefore(endTime)
                && startTime.isBefore(slot.getEndTime());
    }
}
