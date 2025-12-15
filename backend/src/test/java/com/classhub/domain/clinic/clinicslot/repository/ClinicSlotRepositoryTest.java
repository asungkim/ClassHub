package com.classhub.domain.clinic.clinicslot.repository;

import com.classhub.domain.clinic.clinicslot.model.ClinicSlot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("ClinicSlot Repository 테스트")
class ClinicSlotRepositoryTest {

    @Autowired
    private ClinicSlotRepository clinicSlotRepository;

    @Test
    @DisplayName("슬롯을 저장할 수 있다")
    void shouldSaveClinicSlot() {
        // given
        UUID teacherId = UUID.randomUUID();
        ClinicSlot slot = ClinicSlot.builder()
            .teacherId(teacherId)
            .dayOfWeek(DayOfWeek.MONDAY)
            .startTime(LocalTime.of(14, 0))
            .endTime(LocalTime.of(16, 0))
            .capacity(10)
            .build();

        // when
        ClinicSlot saved = clinicSlotRepository.save(slot);

        // then
        ClinicSlot found = clinicSlotRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getTeacherId()).isEqualTo(teacherId);
        assertThat(found.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(found.getStartTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(found.getEndTime()).isEqualTo(LocalTime.of(16, 0));
        assertThat(found.getCapacity()).isEqualTo(10);
        assertThat(found.isActive()).isTrue();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("teacherId와 활성 상태로 슬롯을 조회할 수 있다")
    void shouldFindByTeacherIdAndIsActive() {
        // given
        UUID teacherId = UUID.randomUUID();
        ClinicSlot activeSlot = ClinicSlot.builder()
            .teacherId(teacherId)
            .dayOfWeek(DayOfWeek.MONDAY)
            .startTime(LocalTime.of(14, 0))
            .endTime(LocalTime.of(16, 0))
            .capacity(10)
            .build();

        ClinicSlot inactiveSlot = ClinicSlot.builder()
            .teacherId(teacherId)
            .dayOfWeek(DayOfWeek.TUESDAY)
            .startTime(LocalTime.of(14, 0))
            .endTime(LocalTime.of(16, 0))
            .capacity(10)
            .isActive(false)
            .build();

        clinicSlotRepository.save(activeSlot);
        clinicSlotRepository.save(inactiveSlot);

        // when
        List<ClinicSlot> activeSlots = clinicSlotRepository.findByTeacherIdAndIsActive(teacherId, true);

        // then
        assertThat(activeSlots).hasSize(1);
        assertThat(activeSlots.get(0).isActive()).isTrue();
        assertThat(activeSlots.get(0).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    @DisplayName("teacherId, 요일, 활성 상태로 슬롯을 조회할 수 있다")
    void shouldFindByTeacherIdAndDayOfWeekAndIsActive() {
        // given
        UUID teacherId = UUID.randomUUID();

        ClinicSlot mondaySlot = ClinicSlot.builder()
            .teacherId(teacherId)
            .dayOfWeek(DayOfWeek.MONDAY)
            .startTime(LocalTime.of(14, 0))
            .endTime(LocalTime.of(16, 0))
            .capacity(10)
            .build();

        ClinicSlot fridaySlot = ClinicSlot.builder()
            .teacherId(teacherId)
            .dayOfWeek(DayOfWeek.FRIDAY)
            .startTime(LocalTime.of(14, 0))
            .endTime(LocalTime.of(16, 0))
            .capacity(10)
            .build();

        clinicSlotRepository.save(mondaySlot);
        clinicSlotRepository.save(fridaySlot);

        // when
        List<ClinicSlot> mondaySlots = clinicSlotRepository.findByTeacherIdAndDayOfWeekAndIsActive(
            teacherId, DayOfWeek.MONDAY, true
        );

        // then
        assertThat(mondaySlots).hasSize(1);
        assertThat(mondaySlots.get(0).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    @DisplayName("슬롯 ID와 teacherId로 소유권 검증을 할 수 있다")
    void shouldFindByIdAndTeacherId() {
        // given
        UUID teacherId = UUID.randomUUID();
        UUID otherTeacherId = UUID.randomUUID();

        ClinicSlot slot = ClinicSlot.builder()
            .teacherId(teacherId)
            .dayOfWeek(DayOfWeek.MONDAY)
            .startTime(LocalTime.of(14, 0))
            .endTime(LocalTime.of(16, 0))
            .capacity(10)
            .build();

        ClinicSlot saved = clinicSlotRepository.save(slot);

        // when
        var found = clinicSlotRepository.findByIdAndTeacherId(saved.getId(), teacherId);
        var notFound = clinicSlotRepository.findByIdAndTeacherId(saved.getId(), otherTeacherId);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getTeacherId()).isEqualTo(teacherId);
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("시간이 겹치는 활성 슬롯을 찾을 수 있다")
    void shouldFindOverlappingSlots_whenTimeConflicts() {
        // given
        UUID teacherId = UUID.randomUUID();

        ClinicSlot existingSlot = ClinicSlot.builder()
            .teacherId(teacherId)
            .dayOfWeek(DayOfWeek.MONDAY)
            .startTime(LocalTime.of(14, 0))
            .endTime(LocalTime.of(16, 0))
            .capacity(10)
            .build();

        ClinicSlot saved = clinicSlotRepository.save(existingSlot);

        // when: 15:00-17:00으로 시간이 겹침
        List<ClinicSlot> overlapping = clinicSlotRepository.findOverlappingSlots(
            teacherId,
            DayOfWeek.MONDAY,
            LocalTime.of(15, 0),
            LocalTime.of(17, 0),
            UUID.randomUUID()  // 다른 ID
        );

        // then
        assertThat(overlapping).hasSize(1);
        assertThat(overlapping.get(0).getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("시간이 겹치지 않으면 빈 목록을 반환한다")
    void shouldNotFindOverlappingSlots_whenNoConflict() {
        // given
        UUID teacherId = UUID.randomUUID();

        ClinicSlot existingSlot = ClinicSlot.builder()
            .teacherId(teacherId)
            .dayOfWeek(DayOfWeek.MONDAY)
            .startTime(LocalTime.of(14, 0))
            .endTime(LocalTime.of(16, 0))
            .capacity(10)
            .build();

        clinicSlotRepository.save(existingSlot);

        // when: 10:00-12:00으로 시간이 안 겹침
        List<ClinicSlot> overlapping = clinicSlotRepository.findOverlappingSlots(
            teacherId,
            DayOfWeek.MONDAY,
            LocalTime.of(10, 0),
            LocalTime.of(12, 0),
            UUID.randomUUID()
        );

        // then
        assertThat(overlapping).isEmpty();
    }
}
