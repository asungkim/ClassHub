package com.classhub.domain.clinic.attendance.web;

import com.classhub.domain.clinic.attendance.application.ClinicAttendanceService;
import com.classhub.domain.clinic.attendance.dto.request.ClinicAttendanceCreateRequest;
import com.classhub.domain.clinic.attendance.dto.request.ClinicAttendanceMoveRequest;
import com.classhub.domain.clinic.attendance.dto.request.StudentClinicAttendanceRequest;
import com.classhub.domain.clinic.attendance.dto.response.ClinicAttendanceDetailResponse;
import com.classhub.domain.clinic.attendance.dto.response.ClinicAttendanceResponse;
import com.classhub.domain.clinic.attendance.dto.response.StudentClinicAttendanceListResponse;
import com.classhub.domain.clinic.attendance.model.ClinicAttendance;
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

    @GetMapping("/clinic-attendances")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT')")
    @Operation(summary = "클리닉 출석 명단 조회")
    public RsData<List<ClinicAttendanceDetailResponse>> getAttendances(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam("clinicSessionId") UUID clinicSessionId
    ) {
        List<ClinicAttendanceDetailResponse> response =
                clinicAttendanceService.getAttendanceDetails(principal, clinicSessionId);
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
            @Valid @RequestBody StudentClinicAttendanceRequest request
    ) {
        ClinicAttendance attendance = clinicAttendanceService
                .requestAttendance(principal, request.clinicSessionId(), request.courseId());
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
        StudentClinicAttendanceListResponse response = clinicAttendanceService.getStudentAttendanceResponses(
                principal,
                range.startDate(),
                range.endDate()
        );
        return RsData.from(RsCode.SUCCESS, response);
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

    @DeleteMapping("/students/me/clinic-attendances/{attendanceId}")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "학생 클리닉 참석 취소")
    public RsData<Void> cancelAttendance(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID attendanceId
    ) {
        clinicAttendanceService.cancelStudentAttendance(principal, attendanceId);
        return RsData.from(RsCode.SUCCESS, null);
    }
}
