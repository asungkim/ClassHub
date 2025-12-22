package com.classhub.domain.clinic.clinicattendance.web;

import com.classhub.domain.clinic.clinicattendance.application.ClinicAttendanceService;
import com.classhub.domain.clinic.clinicattendance.dto.request.ClinicAttendanceCreateRequest;
import com.classhub.domain.clinic.clinicattendance.dto.request.ClinicAttendanceMoveRequest;
import com.classhub.domain.clinic.clinicattendance.dto.response.ClinicAttendanceResponse;
import com.classhub.domain.clinic.clinicattendance.dto.response.StudentClinicAttendanceListResponse;
import com.classhub.domain.clinic.clinicattendance.dto.response.StudentClinicAttendanceResponse;
import com.classhub.domain.clinic.clinicattendance.model.ClinicAttendance;
import com.classhub.domain.clinic.clinicsession.model.ClinicSession;
import com.classhub.domain.clinic.clinicsession.repository.ClinicSessionRepository;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.global.exception.BusinessException;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "ClinicAttendance API", description = "클리닉 출석 관리 API")
public class ClinicAttendanceController {

    private final ClinicAttendanceService clinicAttendanceService;
    private final ClinicSessionRepository clinicSessionRepository;

    @GetMapping("/clinic-attendances")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT')")
    @Operation(summary = "클리닉 출석 명단 조회")
    public RsData<List<ClinicAttendanceResponse>> getAttendances(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam("clinicSessionId") UUID clinicSessionId
    ) {
        List<ClinicAttendance> attendances = clinicAttendanceService.getAttendances(principal, clinicSessionId);
        List<ClinicAttendanceResponse> response = attendances.stream()
                .map(ClinicAttendanceResponse::from)
                .toList();
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PostMapping("/clinic-sessions/{sessionId}/attendances")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT')")
    @Operation(summary = "클리닉 출석 추가")
    public RsData<ClinicAttendanceResponse> addAttendance(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID sessionId,
            @Valid @RequestBody ClinicAttendanceCreateRequest request
    ) {
        ClinicAttendance attendance = clinicAttendanceService
                .addAttendance(principal, sessionId, request.studentCourseRecordId());
        return RsData.from(RsCode.CREATED, ClinicAttendanceResponse.from(attendance));
    }

    @DeleteMapping("/clinic-attendances/{attendanceId}")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT')")
    @Operation(summary = "클리닉 출석 삭제")
    public RsData<Void> deleteAttendance(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID attendanceId
    ) {
        clinicAttendanceService.deleteAttendance(principal, attendanceId);
        return RsData.from(RsCode.SUCCESS, null);
    }

    @PostMapping("/students/me/clinic-attendances")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "학생 클리닉 참석 신청")
    public RsData<ClinicAttendanceResponse> requestAttendance(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam("clinicSessionId") UUID clinicSessionId,
            @RequestParam("studentCourseRecordId") UUID studentCourseRecordId
    ) {
        ClinicAttendance attendance = clinicAttendanceService
                .requestAttendance(principal, clinicSessionId, studentCourseRecordId);
        return RsData.from(RsCode.CREATED, ClinicAttendanceResponse.from(attendance));
    }

    @GetMapping("/students/me/clinic-attendances")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "학생 클리닉 참석 목록 조회")
    public RsData<StudentClinicAttendanceListResponse> getStudentAttendances(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam("dateRange") String dateRange
    ) {
        DateRange range = DateRangeParser.parse(dateRange);
        List<ClinicAttendance> attendances = clinicAttendanceService.getStudentAttendances(
                principal,
                range.startDate(),
                range.endDate()
        );
        List<StudentClinicAttendanceResponse> items = attendances.stream()
                .map(attendance -> {
                    ClinicSession session = clinicSessionRepository
                            .findByIdAndDeletedAtIsNull(attendance.getClinicSessionId())
                            .orElseThrow(RsCode.CLINIC_SESSION_NOT_FOUND::toException);
                    return StudentClinicAttendanceResponse.from(ClinicAttendanceResponse.from(attendance), session);
                })
                .toList();
        return RsData.from(RsCode.SUCCESS, new StudentClinicAttendanceListResponse(items));
    }

    @PatchMapping("/students/me/clinic-attendances")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "학생 클리닉 이동")
    public RsData<ClinicAttendanceResponse> moveAttendance(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody ClinicAttendanceMoveRequest request
    ) {
        ClinicAttendance attendance = clinicAttendanceService
                .moveAttendance(principal, request.fromSessionId(), request.toSessionId());
        return RsData.from(RsCode.SUCCESS, ClinicAttendanceResponse.from(attendance));
    }
}
