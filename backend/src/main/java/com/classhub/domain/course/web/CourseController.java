package com.classhub.domain.course.web;

import com.classhub.domain.course.application.CourseService;
import com.classhub.domain.course.dto.CourseStatusFilter;
import com.classhub.domain.course.dto.request.CourseCreateRequest;
import com.classhub.domain.course.dto.request.CourseStatusUpdateRequest;
import com.classhub.domain.course.dto.request.CourseUpdateRequest;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1/courses")
@Tag(name = "Course API (Teacher)", description = "선생님 반 관리 API")
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Course 목록 조회", description = "선생님이 생성한 Course를 목록 뷰로 조회한다.")
    public RsData<PageResponse<CourseResponse>> getCourses(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(name = "status", defaultValue = "ACTIVE") String status,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        CourseStatusFilter filter = parseStatus(status);
        PageResponse<CourseResponse> response = courseService.getCourses(
                principal.id(),
                filter,
                branchId,
                keyword,
                page,
                size
        );
        return RsData.from(RsCode.SUCCESS, response);
    }

    @GetMapping("/schedule")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Course 캘린더 조회", description = "기간 내 Course 스케줄 목록을 조회한다.")
    public RsData<List<CourseResponse>> getCourseSchedules(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<CourseResponse> responses = courseService.getCoursesWithinPeriod(principal.id(), startDate, endDate);
        return RsData.from(RsCode.SUCCESS, responses);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Course 생성", description = "선생님이 Course를 생성한다.")
    public RsData<CourseResponse> createCourse(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody CourseCreateRequest request
    ) {
        CourseResponse response = courseService.createCourse(principal.id(), request);
        return RsData.from(RsCode.CREATED, response);
    }

    @GetMapping("/{courseId}")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Course 상세 조회", description = "Course 상세 정보를 조회한다.")
    public RsData<CourseResponse> getCourse(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID courseId
    ) {
        CourseResponse response = courseService.getCourse(principal.id(), courseId);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PatchMapping("/{courseId}")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Course 수정", description = "Course 기본 정보와 스케줄을 수정한다.")
    public RsData<CourseResponse> updateCourse(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID courseId,
            @Valid @RequestBody CourseUpdateRequest request
    ) {
        CourseResponse response = courseService.updateCourse(principal.id(), courseId, request);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PatchMapping("/{courseId}/status")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Course 활성/비활성화", description = "Course 상태를 on/off 한다.")
    public RsData<CourseResponse> updateCourseStatus(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID courseId,
            @Valid @RequestBody CourseStatusUpdateRequest request
    ) {
        CourseResponse response = courseService.updateCourseStatus(principal.id(), courseId, request);
        return RsData.from(RsCode.SUCCESS, response);
    }

    private CourseStatusFilter parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return CourseStatusFilter.ACTIVE;
        }
        try {
            return CourseStatusFilter.valueOf(rawStatus.toUpperCase(Locale.US));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
    }
}
