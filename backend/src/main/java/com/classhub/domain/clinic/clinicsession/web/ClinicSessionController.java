package com.classhub.domain.clinic.clinicsession.web;

import com.classhub.domain.clinic.clinicsession.application.ClinicSessionService;
import com.classhub.domain.clinic.clinicsession.dto.request.ClinicSessionEmergencyCreateRequest;
import com.classhub.domain.clinic.clinicsession.dto.request.ClinicSessionRegularCreateRequest;
import com.classhub.domain.clinic.clinicsession.dto.response.ClinicSessionResponse;
import com.classhub.domain.clinic.clinicsession.model.ClinicSession;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
        DateRange range = parseDateRange(dateRange);
        List<ClinicSession> sessions = resolveSessionsByRole(principal, teacherId, branchId, range);
        List<ClinicSessionResponse> response = sessions.stream()
                .map(ClinicSessionResponse::from)
                .toList();
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

    private List<ClinicSession> resolveSessionsByRole(
            MemberPrincipal principal,
            UUID teacherId,
            UUID branchId,
            DateRange range
    ) {
        if (principal.role() == MemberRole.TEACHER) {
            return clinicSessionService.getSessionsForTeacher(principal.id(), branchId, range.startDate(), range.endDate());
        }
        if (principal.role() == MemberRole.ASSISTANT) {
            return clinicSessionService.getSessionsForAssistant(principal.id(), teacherId, branchId,
                    range.startDate(), range.endDate());
        }
        if (principal.role() == MemberRole.STUDENT) {
            return clinicSessionService.getSessionsForStudent(principal.id(), teacherId, branchId,
                    range.startDate(), range.endDate());
        }
        throw new BusinessException(RsCode.FORBIDDEN);
    }

    private DateRange parseDateRange(String dateRange) {
        if (dateRange == null || dateRange.isBlank()) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        String[] parts = dateRange.split(",");
        if (parts.length != 2) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        try {
            LocalDate start = LocalDate.parse(parts[0].trim());
            LocalDate end = LocalDate.parse(parts[1].trim());
            if (start.isAfter(end)) {
                throw new BusinessException(RsCode.BAD_REQUEST);
            }
            return new DateRange(start, end);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}
