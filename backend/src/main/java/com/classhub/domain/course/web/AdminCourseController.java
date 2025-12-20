package com.classhub.domain.course.web;

import com.classhub.domain.course.application.AdminCourseService;
import com.classhub.domain.course.dto.CourseStatusFilter;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/courses")
@RequiredArgsConstructor
@Tag(name = "Admin Course API")
public class AdminCourseController {

    private final AdminCourseService adminCourseService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "모든 Course 조회")
    public RsData<PageResponse<CourseResponse>> getCourses(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(name = "teacherId", required = false) UUID teacherId,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "companyId", required = false) UUID companyId,
            @RequestParam(name = "status", required = false) String rawStatus,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        requireSuperAdmin(principal);
        CourseStatusFilter statusFilter = parseStatus(rawStatus);
        PageResponse<CourseResponse> response = adminCourseService.searchCourses(
                teacherId,
                branchId,
                companyId,
                statusFilter,
                keyword,
                page,
                size
        );
        return RsData.from(RsCode.SUCCESS, response);
    }

    @DeleteMapping("/{courseId}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Course 하드 삭제")
    public RsData<Void> deleteCourse(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID courseId
    ) {
        requireSuperAdmin(principal);
        adminCourseService.deleteCourse(courseId);
        return RsData.from(RsCode.SUCCESS, null);
    }

    private void requireSuperAdmin(MemberPrincipal principal) {
        if (principal == null || principal.role() != MemberRole.SUPER_ADMIN) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
    }

    private CourseStatusFilter parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return CourseStatusFilter.ALL;
        }
        try {
            return CourseStatusFilter.valueOf(rawStatus.toUpperCase(Locale.US));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
    }
}
