package com.classhub.domain.clinic.clinicslot.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("ClinicSlotService TDD")
class ClinicSlotServiceTest {

    @Autowired
    private ClinicSlotService clinicSlotService;

    @Autowired
    private ClinicSlotRepository clinicSlotRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member teacher;
    private Member otherTeacher;

    @BeforeEach
    void setUp() {
        clinicSlotRepository.deleteAll();
        memberRepository.deleteAll();

        teacher = memberRepository.save(
                Member.builder()
                        .email("teacher@classhub.dev")
                        .password("password")
                        .name("Teacher One")
                        .role(MemberRole.TEACHER)
                        .build()
        );
        otherTeacher = memberRepository.save(
                Member.builder()
                        .email("other@classhub.dev")
                        .password("password")
                        .name("Teacher Two")
                        .role(MemberRole.TEACHER)
                        .build()
        );
    }

    @Test
    @DisplayName("Teacher는 ClinicSlot을 생성할 수 있다")
    void shouldCreateSlot_whenValidRequest() {
        ClinicSlotResponse response = clinicSlotService.createSlot(
                teacher.getId(),
                new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "14:00", "16:00", 10)
        );

        assertThat(response.teacherId()).isEqualTo(teacher.getId());
        assertThat(response.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(response.startTime().toString()).isEqualTo("14:00");
        assertThat(response.endTime().toString()).isEqualTo("16:00");
        assertThat(response.capacity()).isEqualTo(10);
    }

    @Test
    @DisplayName("존재하지 않거나 Teacher가 아닌 사용자는 ClinicSlot을 생성할 수 없다")
    void shouldThrowException_whenInvalidTeacher() {
        UUID invalidTeacherId = UUID.randomUUID();

        assertThatThrownBy(() -> clinicSlotService.createSlot(
                invalidTeacherId,
                new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "14:00", "16:00", 10)
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.UNAUTHENTICATED);
    }

    @Test
    @DisplayName("시작 시간이 종료 시간보다 늦으면 예외가 발생한다")
    void shouldThrowException_whenStartTimeAfterEndTime() {
        assertThatThrownBy(() -> clinicSlotService.createSlot(
                teacher.getId(),
                new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "16:00", "15:00", 10)
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("같은 요일과 시간이 겹치면 슬롯 생성에 실패한다")
    void shouldThrowException_whenTimeConflictOnCreate() {
        clinicSlotService.createSlot(
                teacher.getId(),
                new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "14:00", "16:00", 10)
        );

        assertThatThrownBy(() -> clinicSlotService.createSlot(
                teacher.getId(),
                new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "15:00", "17:00", 10)
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.CLINIC_SLOT_CONFLICT);
    }

    @Test
    @DisplayName("Teacher는 활성 상태로 ClinicSlot 목록을 조회할 수 있다")
    void shouldGetSlots_whenActiveFilter() {
        clinicSlotService.createSlot(
                teacher.getId(),
                new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "14:00", "16:00", 10)
        );
        ClinicSlotResponse inactive = clinicSlotService.createSlot(
                teacher.getId(),
                new ClinicSlotCreateRequest(DayOfWeek.TUESDAY, "10:00", "12:00", 8)
        );
        clinicSlotService.deactivateSlot(teacher.getId(), inactive.id());

        List<ClinicSlotResponse> activeSlots = clinicSlotService.getSlots(teacher.getId(), true, null);

        assertThat(activeSlots).hasSize(1);
        assertThat(activeSlots.getFirst().dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    @DisplayName("Teacher는 요일로 ClinicSlot 목록을 필터링할 수 있다")
    void shouldGetSlots_whenDayOfWeekFilter() {
        clinicSlotService.createSlot(
                teacher.getId(),
                new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "14:00", "16:00", 10)
        );
        clinicSlotService.createSlot(
                teacher.getId(),
                new ClinicSlotCreateRequest(DayOfWeek.FRIDAY, "10:00", "12:00", 8)
        );

        List<ClinicSlotResponse> mondaySlots = clinicSlotService.getSlots(teacher.getId(), true, DayOfWeek.MONDAY);

        assertThat(mondaySlots).hasSize(1);
        assertThat(mondaySlots.getFirst().dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    @DisplayName("소유한 ClinicSlot을 상세 조회할 수 있다")
    void shouldGetSlot_whenValidOwner() {
        ClinicSlotResponse created = clinicSlotService.createSlot(
                teacher.getId(),
                new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "14:00", "16:00", 10)
        );

        ClinicSlotResponse found = clinicSlotService.getSlot(teacher.getId(), created.id());

        assertThat(found.id()).isEqualTo(created.id());
    }

    @Test
    @DisplayName("소유하지 않은 ClinicSlot은 조회할 수 없다")
    void shouldThrowException_whenNotOwner() {
        ClinicSlotResponse created = clinicSlotService.createSlot(
                otherTeacher.getId(),
                new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "14:00", "16:00", 10)
        );

        assertThatThrownBy(() -> clinicSlotService.getSlot(teacher.getId(), created.id()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }

    @Test
    @DisplayName("ClinicSlot 정보를 수정할 수 있다")
    void shouldUpdateSlot_whenValidData() {
        ClinicSlotResponse created = clinicSlotService.createSlot(
                teacher.getId(),
                new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "14:00", "16:00", 10)
        );

        ClinicSlotResponse updated = clinicSlotService.updateSlot(
                teacher.getId(),
                created.id(),
                new ClinicSlotUpdateRequest(DayOfWeek.TUESDAY, "15:00", "17:00", 12)
        );

        assertThat(updated.dayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
        assertThat(updated.startTime().toString()).isEqualTo("15:00");
        assertThat(updated.capacity()).isEqualTo(12);
    }

    @Test
    @DisplayName("수정 시 다른 활성 슬롯과 시간이 겹치면 실패한다")
    void shouldThrowException_whenUpdateCausesConflict() {
        ClinicSlotResponse slot1 = clinicSlotService.createSlot(
                teacher.getId(),
                new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "14:00", "16:00", 10)
        );
        clinicSlotService.createSlot(
                teacher.getId(),
                new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "16:00", "18:00", 10)
        );

        assertThatThrownBy(() -> clinicSlotService.updateSlot(
                teacher.getId(),
                slot1.id(),
                new ClinicSlotUpdateRequest(DayOfWeek.MONDAY, "15:30", "17:30", null)
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.CLINIC_SLOT_CONFLICT);
    }

    @Test
    @DisplayName("ClinicSlot을 삭제할 수 있다")
    void shouldDeleteSlot_whenValidOwner() {
        ClinicSlotResponse created = clinicSlotService.createSlot(
                teacher.getId(),
                new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "14:00", "16:00", 10)
        );

        clinicSlotService.deleteSlot(teacher.getId(), created.id());

        assertThat(clinicSlotRepository.findById(created.id())).isEmpty();
    }

    @Test
    @DisplayName("소유하지 않은 ClinicSlot 삭제는 실패한다")
    void shouldThrowException_whenDeleteNotOwner() {
        ClinicSlotResponse created = clinicSlotService.createSlot(
                otherTeacher.getId(),
                new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "14:00", "16:00", 10)
        );

        assertThatThrownBy(() -> clinicSlotService.deleteSlot(teacher.getId(), created.id()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }

    @Test
    @DisplayName("ClinicSlot을 비활성화할 수 있다")
    void shouldDeactivateSlot_whenValidOwner() {
        ClinicSlotResponse created = clinicSlotService.createSlot(
                teacher.getId(),
                new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "14:00", "16:00", 10)
        );

        clinicSlotService.deactivateSlot(teacher.getId(), created.id());

        ClinicSlot slot = clinicSlotRepository.findById(created.id()).orElseThrow();
        assertThat(slot.isActive()).isFalse();
    }

    @Test
    @DisplayName("비활성화된 ClinicSlot을 활성화할 수 있다")
    void shouldActivateSlot_whenNoConflict() {
        ClinicSlotResponse created = clinicSlotService.createSlot(
                teacher.getId(),
                new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "14:00", "16:00", 10)
        );
        clinicSlotService.deactivateSlot(teacher.getId(), created.id());

        clinicSlotService.activateSlot(teacher.getId(), created.id());

        ClinicSlot slot = clinicSlotRepository.findById(created.id()).orElseThrow();
        assertThat(slot.isActive()).isTrue();
    }

    @Test
    @DisplayName("활성화 시 다른 슬롯과 시간이 겹치면 실패한다")
    void shouldThrowException_whenActivateCausesConflict() {
        ClinicSlotResponse activeSlot = clinicSlotService.createSlot(
                teacher.getId(),
                new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "14:00", "16:00", 10)
        );
        ClinicSlotResponse deactivated = clinicSlotService.createSlot(
                teacher.getId(),
                new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "16:00", "18:00", 10)
        );
        clinicSlotService.deactivateSlot(teacher.getId(), deactivated.id());

        // update deactivated slot to overlapping time
        clinicSlotService.updateSlot(
                teacher.getId(),
                deactivated.id(),
                new ClinicSlotUpdateRequest(DayOfWeek.MONDAY, "15:00", "17:00", null)
        );

        assertThatThrownBy(() -> clinicSlotService.activateSlot(teacher.getId(), deactivated.id()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.CLINIC_SLOT_CONFLICT);
    }
}
