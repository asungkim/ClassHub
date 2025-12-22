package com.classhub.domain.clinic.clinicsession.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.clinic.clinicsession.model.ClinicSession;
import com.classhub.domain.clinic.clinicsession.model.ClinicSessionType;
import com.classhub.domain.clinic.clinicslot.model.ClinicSlot;
import com.classhub.domain.clinic.clinicslot.repository.ClinicSlotRepository;
import com.classhub.global.config.JpaConfig;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class ClinicSessionRepositoryTest {

    @Autowired
    private ClinicSessionRepository clinicSessionRepository;
    @Autowired
    private ClinicSlotRepository clinicSlotRepository;

    @Test
    void findBySlotIdAndDateAndDeletedAtIsNull_shouldReturnSession() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        ClinicSlot slot = clinicSlotRepository.save(createSlot(teacherId, branchId));
        LocalDate date = LocalDate.of(2024, Month.MARCH, 4);
        ClinicSession session = clinicSessionRepository.save(
                createRegularSession(slot, teacherId, branchId, date)
        );

        Optional<ClinicSession> result = clinicSessionRepository
                .findBySlotIdAndDateAndDeletedAtIsNull(slot.getId(), date);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(session.getId());
    }

    @Test
    void findByTeacherMemberIdAndBranchIdAndDateRange_shouldReturnMatchingSessions() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        ClinicSlot slot = clinicSlotRepository.save(createSlot(teacherId, branchId));
        ClinicSession regularSession = clinicSessionRepository.save(
                createRegularSession(slot, teacherId, branchId, LocalDate.of(2024, Month.MARCH, 5))
        );
        ClinicSession emergencySession = clinicSessionRepository.save(
                createEmergencySession(teacherId, branchId, LocalDate.of(2024, Month.MARCH, 6))
        );

        ClinicSlot otherSlot = clinicSlotRepository.save(createSlot(UUID.randomUUID(), branchId));
        clinicSessionRepository.save(
                createRegularSession(otherSlot, otherSlot.getTeacherMemberId(), branchId, LocalDate.of(2024, Month.MARCH, 5))
        );
        clinicSessionRepository.save(
                createEmergencySession(teacherId, UUID.randomUUID(), LocalDate.of(2024, Month.MARCH, 5))
        );

        List<ClinicSession> results = clinicSessionRepository
                .findByTeacherMemberIdAndBranchIdAndDateRange(
                        teacherId,
                        branchId,
                        LocalDate.of(2024, Month.MARCH, 1),
                        LocalDate.of(2024, Month.MARCH, 31)
                );

        assertThat(results)
                .extracting(ClinicSession::getId)
                .containsExactlyInAnyOrder(regularSession.getId(), emergencySession.getId());
    }

    private ClinicSlot createSlot(UUID teacherId, UUID branchId) {
        return ClinicSlot.builder()
                .teacherMemberId(teacherId)
                .creatorMemberId(teacherId)
                .branchId(branchId)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .defaultCapacity(10)
                .build();
    }

    private ClinicSession createRegularSession(ClinicSlot slot,
                                               UUID teacherId,
                                               UUID branchId,
                                               LocalDate date) {
        return ClinicSession.builder()
                .slotId(slot.getId())
                .teacherMemberId(teacherId)
                .branchId(branchId)
                .sessionType(ClinicSessionType.REGULAR)
                .creatorMemberId(null)
                .date(date)
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .capacity(slot.getDefaultCapacity())
                .canceled(false)
                .build();
    }

    private ClinicSession createEmergencySession(UUID teacherId,
                                                 UUID branchId,
                                                 LocalDate date) {
        return ClinicSession.builder()
                .slotId(null)
                .teacherMemberId(teacherId)
                .branchId(branchId)
                .sessionType(ClinicSessionType.EMERGENCY)
                .creatorMemberId(teacherId)
                .date(date)
                .startTime(LocalTime.of(20, 0))
                .endTime(LocalTime.of(21, 0))
                .capacity(5)
                .canceled(false)
                .build();
    }
}
