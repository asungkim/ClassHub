package com.classhub.domain.clinic.session.web;

import com.classhub.domain.clinic.session.application.ClinicSessionService;
import com.classhub.domain.clinic.session.dto.request.ClinicSessionEmergencyCreateRequest;
import com.classhub.domain.clinic.session.dto.request.ClinicSessionRegularCreateRequest;
import com.classhub.domain.clinic.session.dto.response.ClinicSessionResponse;
import com.classhub.domain.clinic.session.model.ClinicSession;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import com.classhub.global.util.DateRangeParser;
import com.classhub.global.util.DateRangeParser.DateRange;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "ClinicSession API", description = "클리닉 세션 관리 API")
public class ClinicSessionController {

    private final ClinicSessionService clinicSessionService;

    @GetMapping("/clinic-sessions")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT', 'STUDENT')")
    @Operation(summary = "클리닉 세션 조회")
    public RsData<List<ClinicSessionResponse>> getSessions(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam("dateRange") String dateRange,
            @RequestParam("branchId") UUID branchId,
            @RequestParam(value = "teacherId", required = false) UUID teacherId
    ) {
        DateRange range = DateRangeParser.parse(dateRange);
        List<ClinicSessionResponse> response = clinicSessionService.getSessions(
                principal,
                teacherId,
                branchId,
                range.startDate(),
                range.endDate()
        );
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PostMapping("/clinic-slots/{slotId}/sessions")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "정규 클리닉 세션 수동 생성")
    public RsData<ClinicSessionResponse> createRegularSession(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID slotId,
            @Valid @RequestBody ClinicSessionRegularCreateRequest request
    ) {
        ClinicSession session = clinicSessionService.createRegularSession(principal.id(), slotId, request.date());
        return RsData.from(RsCode.CREATED, ClinicSessionResponse.from(session));
    }

    @PostMapping("/clinic-sessions/emergency")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT')")
    @Operation(summary = "긴급 클리닉 세션 생성")
    public RsData<ClinicSessionResponse> createEmergencySession(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody ClinicSessionEmergencyCreateRequest request
    ) {
        ClinicSession session = clinicSessionService.createEmergencySession(principal, request);
        return RsData.from(RsCode.CREATED, ClinicSessionResponse.from(session));
    }

    @PatchMapping("/clinic-sessions/{sessionId}/cancel")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT')")
    @Operation(summary = "클리닉 세션 취소")
    public RsData<Void> cancelSession(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID sessionId
    ) {
        clinicSessionService.cancelSession(principal, sessionId);
        return RsData.from(RsCode.SUCCESS, null);
    }
}
