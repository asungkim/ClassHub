package com.classhub.domain.progress.course.web;

import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.progress.course.application.CourseProgressService;
import com.classhub.domain.progress.course.dto.request.CourseProgressComposeRequest;
import com.classhub.domain.progress.course.dto.request.CourseProgressCreateRequest;
import com.classhub.domain.progress.course.dto.request.CourseProgressUpdateRequest;
import com.classhub.domain.progress.course.dto.response.CourseProgressResponse;
import com.classhub.domain.progress.dto.ProgressSliceResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
@Tag(name = "Course Progress API", description = "반 공통 진도 관리 API")
public class CourseProgressController {

    private final CourseProgressService courseProgressService;

    @PostMapping("/courses/{courseId}/course-progress")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT')")
    @Operation(summary = "반 공통 진도 생성")
    public RsData<CourseProgressResponse> createCourseProgress(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID courseId,
            @Valid @RequestBody CourseProgressCreateRequest request
    ) {
        CourseProgressResponse response = courseProgressService.createCourseProgress(principal, courseId, request);
        return RsData.from(RsCode.CREATED, response);
    }

    @PostMapping("/courses/{courseId}/course-progress/compose")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT')")
    @Operation(summary = "공통 진도 + 개인 진도 배치 생성")
    public RsData<CourseProgressResponse> composeCourseProgress(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID courseId,
            @Valid @RequestBody CourseProgressComposeRequest request
    ) {
        CourseProgressResponse response = courseProgressService.composeCourseProgress(principal, courseId, request);
        return RsData.from(RsCode.CREATED, response);
    }

    @GetMapping("/courses/{courseId}/course-progress")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT')")
    @Operation(summary = "반 공통 진도 목록 조회")
    public RsData<ProgressSliceResponse<CourseProgressResponse>> getCourseProgresses(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID courseId,
            @RequestParam(name = "cursorCreatedAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursorCreatedAt,
            @RequestParam(name = "cursorId", required = false) UUID cursorId,
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        ProgressSliceResponse<CourseProgressResponse> response = courseProgressService.getCourseProgresses(
                principal,
                courseId,
                cursorCreatedAt,
                cursorId,
                limit
        );
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PatchMapping("/course-progress/{progressId}")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT')")
    @Operation(summary = "반 공통 진도 수정")
    public RsData<CourseProgressResponse> updateCourseProgress(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID progressId,
            @Valid @RequestBody CourseProgressUpdateRequest request
    ) {
        CourseProgressResponse response = courseProgressService.updateCourseProgress(principal, progressId, request);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @DeleteMapping("/course-progress/{progressId}")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT')")
    @Operation(summary = "반 공통 진도 삭제")
    public RsData<Void> deleteCourseProgress(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID progressId
    ) {
        courseProgressService.deleteCourseProgress(principal, progressId);
        return RsData.from(RsCode.SUCCESS, null);
    }
}
