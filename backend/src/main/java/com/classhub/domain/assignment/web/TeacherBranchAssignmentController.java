package com.classhub.domain.assignment.web;

import com.classhub.domain.assignment.application.TeacherBranchAssignmentService;
import com.classhub.domain.assignment.dto.TeacherBranchAssignmentStatusFilter;
import com.classhub.domain.assignment.dto.request.TeacherBranchAssignmentCreateRequest;
import com.classhub.domain.assignment.dto.request.TeacherBranchAssignmentStatusUpdateRequest;
import com.classhub.domain.assignment.dto.response.TeacherBranchAssignmentResponse;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Locale;
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
@RequestMapping("/api/v1/teachers/me/branches")
@Tag(name = "Teacher Branch Assignment API", description = "선생님-지점 연결 API")
public class TeacherBranchAssignmentController {

    private final TeacherBranchAssignmentService teacherBranchAssignmentService;

    @GetMapping
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "지점 Assignment 목록", description = "선생님이 연결한 지점 목록을 상태별로 조회한다.")
    public RsData<PageResponse<TeacherBranchAssignmentResponse>> getAssignments(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(name = "status", defaultValue = "ACTIVE") String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        TeacherBranchAssignmentStatusFilter filter = parseStatus(status);
        PageResponse<TeacherBranchAssignmentResponse> response = teacherBranchAssignmentService.getAssignments(
                principal.id(),
                filter,
                page,
                size
        );
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "지점 Assignment 생성", description = "선생님이 특정 지점과의 연결을 생성한다.")
    public RsData<TeacherBranchAssignmentResponse> createAssignment(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody TeacherBranchAssignmentCreateRequest request
    ) {
        TeacherBranchAssignmentResponse response = teacherBranchAssignmentService.createAssignment(principal.id(), request);
        return RsData.from(RsCode.CREATED, response);
    }

    @PatchMapping("/{assignmentId}")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "지점 Assignment 활성/비활성화", description = "선생님이 출강 중단 시 Assignment를 비활성화하고, 필요 시 다시 활성화한다.")
    public RsData<TeacherBranchAssignmentResponse> updateAssignmentStatus(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID assignmentId,
            @Valid @RequestBody TeacherBranchAssignmentStatusUpdateRequest request
    ) {
        TeacherBranchAssignmentResponse response = teacherBranchAssignmentService.updateAssignmentStatus(
                principal.id(),
                assignmentId,
                request
        );
        return RsData.from(RsCode.SUCCESS, response);
    }

    private TeacherBranchAssignmentStatusFilter parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return TeacherBranchAssignmentStatusFilter.ACTIVE;
        }
        try {
            return TeacherBranchAssignmentStatusFilter.valueOf(rawStatus.toUpperCase(Locale.US));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
    }
}
