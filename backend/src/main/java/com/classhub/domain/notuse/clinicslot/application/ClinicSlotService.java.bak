package com.classhub.domain.clinic.clinicslot.application;

import com.classhub.domain.clinic.clinicslot.dto.ClinicSlotCreateRequest;
import com.classhub.domain.clinic.clinicslot.dto.ClinicSlotResponse;
import com.classhub.domain.clinic.clinicslot.dto.ClinicSlotUpdateRequest;
import com.classhub.domain.clinic.clinicslot.model.ClinicSlot;
import com.classhub.domain.clinic.clinicslot.repository.ClinicSlotRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClinicSlotService {

    private final ClinicSlotRepository clinicSlotRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public ClinicSlotResponse createSlot(UUID principalId, ClinicSlotCreateRequest request) {
        Member teacher = getTeacher(principalId);
        LocalTime startTime = parseTime(request.startTime());
        LocalTime endTime = parseTime(request.endTime());
        validateTimeRange(startTime, endTime);
        ensureNoConflict(teacher.getId(), request.dayOfWeek(), startTime, endTime, null);

        ClinicSlot slot = ClinicSlot.builder()
                .teacherId(teacher.getId())
                .dayOfWeek(request.dayOfWeek())
                .startTime(startTime)
                .endTime(endTime)
                .capacity(request.capacity())
                .build();
        ClinicSlot saved = clinicSlotRepository.save(slot);
        return ClinicSlotResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ClinicSlotResponse> getSlots(UUID principalId, Boolean active, DayOfWeek dayOfWeek) {
        Member teacher = getTeacher(principalId);
        UUID teacherId = teacher.getId();
        List<ClinicSlot> slots;

        if (dayOfWeek != null && active != null) {
            slots = clinicSlotRepository.findByTeacherIdAndDayOfWeekAndIsActive(teacherId, dayOfWeek, active);
        } else if (dayOfWeek != null) {
            slots = clinicSlotRepository.findByTeacherIdAndDayOfWeek(teacherId, dayOfWeek);
        } else if (active != null) {
            slots = clinicSlotRepository.findByTeacherIdAndIsActive(teacherId, active);
        } else {
            slots = clinicSlotRepository.findByTeacherId(teacherId);
        }

        return slots.stream()
                .map(ClinicSlotResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClinicSlotResponse getSlot(UUID principalId, UUID slotId) {
        Member teacher = getTeacher(principalId);
        ClinicSlot slot = getOwnedSlot(slotId, teacher.getId());
        return ClinicSlotResponse.from(slot);
    }

    @Transactional
    public ClinicSlotResponse updateSlot(UUID principalId, UUID slotId, ClinicSlotUpdateRequest request) {
        Member teacher = getTeacher(principalId);
        ClinicSlot slot = getOwnedSlot(slotId, teacher.getId());

        DayOfWeek targetDay = request.dayOfWeek() != null ? request.dayOfWeek() : slot.getDayOfWeek();
        LocalTime targetStart = request.startTime() != null ? parseTime(request.startTime()) : slot.getStartTime();
        LocalTime targetEnd = request.endTime() != null ? parseTime(request.endTime()) : slot.getEndTime();
        Integer targetCapacity = request.capacity() != null ? request.capacity() : slot.getCapacity();

        validateTimeRange(targetStart, targetEnd);
        if (slot.isActive()) {
            ensureNoConflict(slot.getTeacherId(), targetDay, targetStart, targetEnd, slot.getId());
        }

        slot.updateSlot(targetDay, targetStart, targetEnd, targetCapacity);
        return ClinicSlotResponse.from(slot);
    }

    @Transactional
    public void deleteSlot(UUID principalId, UUID slotId) {
        Member teacher = getTeacher(principalId);
        ClinicSlot slot = getOwnedSlot(slotId, teacher.getId());
        clinicSlotRepository.delete(slot);
    }

    @Transactional
    public void deactivateSlot(UUID principalId, UUID slotId) {
        Member teacher = getTeacher(principalId);
        ClinicSlot slot = getOwnedSlot(slotId, teacher.getId());
        slot.deactivate();
    }

    @Transactional
    public void activateSlot(UUID principalId, UUID slotId) {
        Member teacher = getTeacher(principalId);
        ClinicSlot slot = getOwnedSlot(slotId, teacher.getId());
        if (slot.isActive()) {
            return;
        }
        ensureNoConflict(slot.getTeacherId(), slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime(), slot.getId());
        slot.activate();
    }

    private Member getTeacher(UUID principalId) {
        Member member = memberRepository.findById(principalId)
                .orElseThrow(() -> new BusinessException(RsCode.UNAUTHENTICATED));
        if (member.getRole() != MemberRole.TEACHER) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        return member;
    }

    private ClinicSlot getOwnedSlot(UUID slotId, UUID teacherId) {
        ClinicSlot slot = clinicSlotRepository.findById(slotId)
                .orElseThrow(() -> new BusinessException(RsCode.CLINIC_SLOT_NOT_FOUND));
        if (!slot.getTeacherId().equals(teacherId)) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        return slot;
    }

    private LocalTime parseTime(String value) {
        return LocalTime.parse(value);
    }

    private void validateTimeRange(LocalTime start, LocalTime end) {
        if (!start.isBefore(end)) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
    }

    private void ensureNoConflict(
            UUID teacherId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            UUID excludeId
    ) {
        List<ClinicSlot> conflicts = clinicSlotRepository.findOverlappingSlots(
                teacherId,
                dayOfWeek,
                startTime,
                endTime,
                excludeId
        );
        if (!conflicts.isEmpty()) {
            throw new BusinessException(RsCode.CLINIC_SLOT_CONFLICT);
        }
    }
}
