package com.classhub.domain.clinic.slot.web;

import com.classhub.domain.clinic.slot.application.ClinicSlotService;
import com.classhub.domain.clinic.slot.dto.request.ClinicSlotCreateRequest;
import com.classhub.domain.clinic.slot.dto.request.ClinicSlotUpdateRequest;
import com.classhub.domain.clinic.slot.dto.response.ClinicSlotResponse;
import com.classhub.domain.clinic.slot.model.ClinicSlot;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@Tag(name = "ClinicSlot API", description = "클리닉 슬롯 관리 API")
public class ClinicSlotController {

    private final ClinicSlotService clinicSlotService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT', 'STUDENT')")
    @Operation(summary = "클리닉 슬롯 목록 조회")
    public RsData<List<ClinicSlotResponse>> getSlots(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "teacherId", required = false) UUID teacherId,
            @RequestParam(name = "courseId", required = false) UUID courseId
    ) {
        List<ClinicSlot> slots = clinicSlotService.getSlots(principal, branchId, teacherId, courseId);
        List<ClinicSlotResponse> response = slots.stream()
                .map(ClinicSlotResponse::from)
                .toList();
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "클리닉 슬롯 생성")
    public RsData<ClinicSlotResponse> createSlot(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody ClinicSlotCreateRequest request
    ) {
        ClinicSlot slot = clinicSlotService.createSlot(principal.id(), request);
        return RsData.from(RsCode.CREATED, ClinicSlotResponse.from(slot));
    }

    @PatchMapping("/{slotId}")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "클리닉 슬롯 수정")
    public RsData<ClinicSlotResponse> updateSlot(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID slotId,
            @Valid @RequestBody ClinicSlotUpdateRequest request
    ) {
        ClinicSlot slot = clinicSlotService.updateSlot(principal.id(), slotId, request);
        return RsData.from(RsCode.SUCCESS, ClinicSlotResponse.from(slot));
    }

    @DeleteMapping("/{slotId}")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "클리닉 슬롯 삭제")
    public RsData<Void> deleteSlot(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID slotId
    ) {
        clinicSlotService.deleteSlot(principal.id(), slotId);
        return RsData.from(RsCode.SUCCESS, null);
    }
}
