package com.classhub.domain.studentcourse.web;

import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.studentcourse.application.StudentCourseManagementService;
import com.classhub.domain.studentcourse.dto.request.StudentCourseRecordUpdateRequest;
import com.classhub.domain.studentcourse.dto.response.StudentCourseDetailResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student-courses")
@RequiredArgsConstructor
@Tag(name = "Student Course Management API", description = "학생 수업 기록 관리 API")
public class StudentCourseManagementController {

    private final StudentCourseManagementService managementService;

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
}
