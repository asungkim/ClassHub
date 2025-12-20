package com.classhub.domain.enrollment.web;

import com.classhub.domain.enrollment.application.StudentEnrollmentApprovalService;
import com.classhub.domain.enrollment.dto.response.TeacherEnrollmentRequestResponse;
import com.classhub.domain.enrollment.model.EnrollmentStatus;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student-enrollment-requests")
@RequiredArgsConstructor
@Tag(name = "Student Enrollment Approval API", description = "선생님·조교 수업 신청 처리 API")
public class StudentEnrollmentApprovalController {

    private final StudentEnrollmentApprovalService approvalService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT')")
    @Operation(summary = "수업 신청 목록 조회")
    public RsData<PageResponse<TeacherEnrollmentRequestResponse>> getRequests(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(name = "courseId", required = false) UUID courseId,
            @RequestParam(name = "status", required = false) Set<EnrollmentStatus> statuses,
            @RequestParam(name = "studentName", required = false) String studentName,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        PageResponse<TeacherEnrollmentRequestResponse> response;
        if (principal.role() == MemberRole.TEACHER) {
            response = approvalService.getRequestsForTeacher(
                    principal.id(),
                    courseId,
                    statuses,
                    studentName,
                    page,
                    size
            );
        } else if (principal.role() == MemberRole.ASSISTANT) {
            response = approvalService.getRequestsForAssistant(
                    principal.id(),
                    courseId,
                    statuses,
                    studentName,
                    page,
                    size
            );
        } else {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        return RsData.from(RsCode.SUCCESS, response);
    }

    @GetMapping("/{requestId}")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT')")
    @Operation(summary = "수업 신청 상세 조회")
    public RsData<TeacherEnrollmentRequestResponse> getRequest(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID requestId
    ) {
        TeacherEnrollmentRequestResponse response = approvalService.getRequestDetail(principal.id(), requestId);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PatchMapping("/{requestId}/approve")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT')")
    @Operation(summary = "수업 신청 승인")
    public RsData<TeacherEnrollmentRequestResponse> approveRequest(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID requestId
    ) {
        TeacherEnrollmentRequestResponse response = approvalService.approveRequest(principal.id(), requestId);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PatchMapping("/{requestId}/reject")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT')")
    @Operation(summary = "수업 신청 거절")
    public RsData<TeacherEnrollmentRequestResponse> rejectRequest(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID requestId
    ) {
        TeacherEnrollmentRequestResponse response = approvalService.rejectRequest(principal.id(), requestId);
        return RsData.from(RsCode.SUCCESS, response);
    }
}
