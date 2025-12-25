package com.classhub.domain.enrollment.web;

import com.classhub.domain.enrollment.application.StudentTeacherRequestService;
import com.classhub.domain.enrollment.dto.request.StudentTeacherRequestCreateRequest;
import com.classhub.domain.enrollment.dto.response.StudentTeacherRequestResponse;
import com.classhub.domain.enrollment.model.TeacherStudentRequestStatus;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Set;
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
@RequiredArgsConstructor
@RequestMapping("/api/v1/teacher-student-requests")
@Tag(name = "Teacher Student Request API", description = "학생 선생님 요청 API")
public class StudentTeacherRequestController {

    private final StudentTeacherRequestService requestService;

    @PostMapping
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "학생 선생님 요청 생성")
    public RsData<StudentTeacherRequestResponse> createRequest(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody StudentTeacherRequestCreateRequest request
    ) {
        StudentTeacherRequestResponse response = requestService.createRequest(principal.id(), request);
        return RsData.from(RsCode.CREATED, response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "학생 선생님 요청 목록 조회")
    public RsData<PageResponse<StudentTeacherRequestResponse>> getMyRequests(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(name = "status", required = false) Set<TeacherStudentRequestStatus> statuses,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        PageResponse<StudentTeacherRequestResponse> response = requestService.getMyRequests(
                principal.id(),
                statuses,
                page,
                size
        );
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PatchMapping("/{requestId}/cancel")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "학생 선생님 요청 취소")
    public RsData<StudentTeacherRequestResponse> cancelRequest(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID requestId
    ) {
        StudentTeacherRequestResponse response = requestService.cancelRequest(principal.id(), requestId);
        return RsData.from(RsCode.SUCCESS, response);
    }
}
