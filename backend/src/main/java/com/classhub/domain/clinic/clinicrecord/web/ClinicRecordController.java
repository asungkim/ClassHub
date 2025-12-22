package com.classhub.domain.clinic.clinicrecord.web;

import com.classhub.domain.clinic.clinicrecord.application.ClinicRecordService;
import com.classhub.domain.clinic.clinicrecord.dto.request.ClinicRecordCreateRequest;
import com.classhub.domain.clinic.clinicrecord.dto.request.ClinicRecordUpdateRequest;
import com.classhub.domain.clinic.clinicrecord.dto.response.ClinicRecordResponse;
import com.classhub.domain.clinic.clinicrecord.model.ClinicRecord;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/clinic-records")
@RequiredArgsConstructor
@Tag(name = "ClinicRecord API", description = "클리닉 기록 관리 API")
public class ClinicRecordController {

    private final ClinicRecordService clinicRecordService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT')")
    @Operation(summary = "클리닉 기록 생성")
    public RsData<ClinicRecordResponse> createRecord(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody ClinicRecordCreateRequest request
    ) {
        ClinicRecord record = clinicRecordService.createRecord(principal, request);
        return RsData.from(RsCode.CREATED, ClinicRecordResponse.from(record));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT')")
    @Operation(summary = "클리닉 기록 조회")
    public RsData<ClinicRecordResponse> getRecord(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam("clinicAttendanceId") UUID clinicAttendanceId
    ) {
        ClinicRecord record = clinicRecordService.getRecord(principal, clinicAttendanceId);
        return RsData.from(RsCode.SUCCESS, ClinicRecordResponse.from(record));
    }

    @PatchMapping("/{recordId}")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT')")
    @Operation(summary = "클리닉 기록 수정")
    public RsData<ClinicRecordResponse> updateRecord(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID recordId,
            @Valid @RequestBody ClinicRecordUpdateRequest request
    ) {
        ClinicRecord record = clinicRecordService.updateRecord(principal, recordId, request);
        return RsData.from(RsCode.SUCCESS, ClinicRecordResponse.from(record));
    }

    @DeleteMapping("/{recordId}")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT')")
    @Operation(summary = "클리닉 기록 삭제")
    public RsData<Void> deleteRecord(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID recordId
    ) {
        clinicRecordService.deleteRecord(principal, recordId);
        return RsData.from(RsCode.SUCCESS, null);
    }
}
