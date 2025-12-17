package com.classhub.domain.clinic.clinicslot.web;

import com.classhub.domain.clinic.clinicslot.application.ClinicSlotService;
import com.classhub.domain.clinic.clinicslot.dto.ClinicSlotCreateRequest;
import com.classhub.domain.clinic.clinicslot.dto.ClinicSlotResponse;
import com.classhub.domain.clinic.clinicslot.dto.ClinicSlotUpdateRequest;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clinic-slots")
@RequiredArgsConstructor
@Tag(name = "ClinicSlot API", description = "ClinicSlot CRUD API")
public class ClinicSlotController {

    private final ClinicSlotService clinicSlotService;

    @PostMapping
    @Operation(summary = "ClinicSlot 생성", description = "요일/시간/정원을 지정해 클리닉 슬롯을 생성한다.")
    @PreAuthorize("hasAuthority('TEACHER')")
    public RsData<ClinicSlotResponse> createSlot(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody ClinicSlotCreateRequest request
    ) {
        ClinicSlotResponse response = clinicSlotService.createSlot(principal.id(), request);
        return RsData.from(RsCode.CREATED, response);
    }

    @GetMapping
    @Operation(summary = "ClinicSlot 목록", description = "Teacher가 생성한 클리닉 슬롯 목록을 조회한다.")
    @PreAuthorize("hasAuthority('TEACHER')")
    public RsData<List<ClinicSlotResponse>> getSlots(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "dayOfWeek", required = false) DayOfWeek dayOfWeek
    ) {
        List<ClinicSlotResponse> slots = clinicSlotService.getSlots(principal.id(), isActive, dayOfWeek);
        return RsData.from(RsCode.SUCCESS, slots);
    }

    @GetMapping("/{slotId}")
    @Operation(summary = "ClinicSlot 상세", description = "클리닉 슬롯 상세 정보를 조회한다.")
    @PreAuthorize("hasAuthority('TEACHER')")
    public RsData<ClinicSlotResponse> getSlot(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID slotId
    ) {
        ClinicSlotResponse response = clinicSlotService.getSlot(principal.id(), slotId);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PatchMapping("/{slotId}")
    @Operation(summary = "ClinicSlot 수정", description = "클리닉 슬롯의 요일/시간/정원을 수정한다.")
    @PreAuthorize("hasAuthority('TEACHER')")
    public RsData<ClinicSlotResponse> updateSlot(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID slotId,
            @Valid @RequestBody ClinicSlotUpdateRequest request
    ) {
        ClinicSlotResponse response = clinicSlotService.updateSlot(principal.id(), slotId, request);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @DeleteMapping("/{slotId}")
    @Operation(summary = "ClinicSlot 삭제", description = "클리닉 슬롯을 삭제한다.")
    @PreAuthorize("hasAuthority('TEACHER')")
    public RsData<Void> deleteSlot(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID slotId
    ) {
        clinicSlotService.deleteSlot(principal.id(), slotId);
        return RsData.from(RsCode.SUCCESS, null);
    }

    @PatchMapping("/{slotId}/deactivate")
    @Operation(summary = "ClinicSlot 비활성화", description = "슬롯을 비활성화한다.")
    @PreAuthorize("hasAuthority('TEACHER')")
    public RsData<Void> deactivateSlot(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID slotId
    ) {
        clinicSlotService.deactivateSlot(principal.id(), slotId);
        return RsData.from(RsCode.SUCCESS, null);
    }

    @PatchMapping("/{slotId}/activate")
    @Operation(summary = "ClinicSlot 활성화", description = "비활성화된 슬롯을 다시 활성화한다.")
    @PreAuthorize("hasAuthority('TEACHER')")
    public RsData<Void> activateSlot(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID slotId
    ) {
        clinicSlotService.activateSlot(principal.id(), slotId);
        return RsData.from(RsCode.SUCCESS, null);
    }
}
