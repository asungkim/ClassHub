package com.classhub.domain.enrollment.web;

import com.classhub.domain.enrollment.application.StudentEnrollmentAdminService;
import com.classhub.domain.enrollment.dto.response.TeacherEnrollmentRequestResponse;
import com.classhub.domain.enrollment.model.EnrollmentStatus;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/student-enrollment-requests")
@RequiredArgsConstructor
@Tag(name = "Admin Student Enrollment API", description = "어드민 수업 신청 감사 조회 API")
public class AdminStudentEnrollmentController {

    private final StudentEnrollmentAdminService adminService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "수업 신청 전체 조회")
    public RsData<PageResponse<TeacherEnrollmentRequestResponse>> getRequests(
            @RequestParam(name = "teacherId", required = false) UUID teacherId,
            @RequestParam(name = "courseId", required = false) UUID courseId,
            @RequestParam(name = "status", required = false) Set<EnrollmentStatus> statuses,
            @RequestParam(name = "studentName", required = false) String studentName,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        PageResponse<TeacherEnrollmentRequestResponse> response = adminService.getRequests(
                teacherId,
                courseId,
                statuses,
                studentName,
                page,
                size
        );
        return RsData.from(RsCode.SUCCESS, response);
    }
}
