package com.classhub.domain.clinic.slot.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.clinic.slot.model.ClinicSlot;
import com.classhub.global.config.JpaConfig;
import java.time.DayOfWeek;
import java.time.LocalTime;
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
class ClinicSlotRepositoryTest {

    @Autowired
    private ClinicSlotRepository clinicSlotRepository;

    @Test
    void findByTeacherMemberIdAndBranchIdAndDeletedAtIsNull_shouldReturnActiveSlots() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        ClinicSlot activeSlot = clinicSlotRepository.save(createSlot(teacherId, branchId, DayOfWeek.MONDAY));
        clinicSlotRepository.save(createSlot(teacherId, UUID.randomUUID(), DayOfWeek.TUESDAY));
        clinicSlotRepository.save(createSlot(UUID.randomUUID(), branchId, DayOfWeek.WEDNESDAY));

        ClinicSlot deletedSlot = clinicSlotRepository.save(createSlot(teacherId, branchId, DayOfWeek.THURSDAY));
        deletedSlot.delete();
        clinicSlotRepository.save(deletedSlot);

        List<ClinicSlot> results = clinicSlotRepository
                .findByTeacherMemberIdAndBranchIdAndDeletedAtIsNull(teacherId, branchId);

        assertThat(results)
                .extracting(ClinicSlot::getId)
                .containsExactlyInAnyOrder(activeSlot.getId());
    }

    @Test
    void findByIdAndDeletedAtIsNull_shouldReturnEmpty_whenDeleted() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        ClinicSlot slot = clinicSlotRepository.save(createSlot(teacherId, branchId, DayOfWeek.FRIDAY));
        slot.delete();
        clinicSlotRepository.save(slot);

        Optional<ClinicSlot> result = clinicSlotRepository.findByIdAndDeletedAtIsNull(slot.getId());

        assertThat(result).isEmpty();
    }

    private ClinicSlot createSlot(UUID teacherId, UUID branchId, DayOfWeek dayOfWeek) {
        return ClinicSlot.builder()
                .teacherMemberId(teacherId)
                .creatorMemberId(teacherId)
                .branchId(branchId)
                .dayOfWeek(dayOfWeek)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .defaultCapacity(10)
                .build();
    }
}
