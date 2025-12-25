package com.classhub.domain.studentcourse.web;

import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.studentcourse.application.StudentCourseManagementService;
import com.classhub.domain.studentcourse.dto.StudentCourseStatusFilter;
import com.classhub.domain.studentcourse.dto.request.StudentCourseRecordUpdateRequest;
import com.classhub.domain.studentcourse.dto.response.StudentCourseDetailResponse;
import com.classhub.domain.studentcourse.dto.response.StudentCourseListItemResponse;
import com.classhub.domain.studentcourse.dto.response.StudentStudentDetailResponse;
import com.classhub.domain.studentcourse.dto.response.StudentStudentListItemResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student-courses")
@RequiredArgsConstructor
@Tag(name = "Student Course Management API", description = "선생님/조교 학생 수업 관리 API")
public class StudentCourseManagementController {

    private final StudentCourseManagementService managementService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT')")
    @Operation(summary = "학생 수업 목록 조회")
    public RsData<PageResponse<StudentCourseListItemResponse>> getStudentCourses(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(name = "courseId", required = false) UUID courseId,
            @RequestParam(name = "status", defaultValue = "ACTIVE") String rawStatus,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        StudentCourseStatusFilter statusFilter = parseStatus(rawStatus);
        PageResponse<StudentCourseListItemResponse> response = managementService.getStudentCourses(
                principal,
                courseId,
                statusFilter,
                keyword,
                page,
                size
        );
        return RsData.from(RsCode.SUCCESS, response);
    }

    @GetMapping("/students")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT')")
    @Operation(summary = "학생 목록 조회")
    public RsData<PageResponse<StudentStudentListItemResponse>> getStudents(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(name = "status", defaultValue = "ACTIVE") String rawStatus,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        StudentCourseStatusFilter statusFilter = parseStatus(rawStatus);
        PageResponse<StudentStudentListItemResponse> response = managementService.getStudents(
                principal,
                statusFilter,
                keyword,
                page,
                size
        );
        return RsData.from(RsCode.SUCCESS, response);
    }

    @GetMapping("/students/{studentId}")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT')")
    @Operation(summary = "학생 상세 조회")
    public RsData<StudentStudentDetailResponse> getStudentDetail(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID studentId
    ) {
        StudentStudentDetailResponse response = managementService.getStudentDetail(principal, studentId);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @GetMapping("/{recordId}")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "학생 수업 상세 조회")
    public RsData<StudentCourseDetailResponse> getStudentCourse(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID recordId
    ) {
        StudentCourseDetailResponse response = managementService.getStudentCourseDetail(principal.id(), recordId);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PatchMapping("/{recordId}")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "학생 수업 기록 수정")
    public RsData<StudentCourseDetailResponse> updateStudentCourse(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID recordId,
            @Valid @RequestBody StudentCourseRecordUpdateRequest request
    ) {
        StudentCourseDetailResponse response = managementService.updateStudentCourseRecord(
                principal.id(),
                recordId,
                request
        );
        return RsData.from(RsCode.SUCCESS, response);
    }

    private StudentCourseStatusFilter parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return StudentCourseStatusFilter.ACTIVE;
        }
        try {
            return StudentCourseStatusFilter.valueOf(rawStatus.toUpperCase(Locale.US));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
    }
}
