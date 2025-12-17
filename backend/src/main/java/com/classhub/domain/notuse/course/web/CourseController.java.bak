package com.classhub.domain.course.web;

import com.classhub.domain.course.application.CourseService;
import com.classhub.domain.course.dto.request.CourseCreateRequest;
import com.classhub.domain.course.dto.request.CourseUpdateRequest;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Course API", description = "반 관리 API")
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @Operation(summary = "반 생성", description = "새로운 반을 생성한다.")
    @PreAuthorize("hasAuthority('TEACHER')")
    public RsData<CourseResponse> createCourse(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody CourseCreateRequest request
    ) {
        if (principal == null) {
            throw new BusinessException(RsCode.UNAUTHENTICATED);
        }
        CourseResponse response = courseService.createCourse(principal.id(), request);
        return RsData.from(RsCode.CREATED, response);
    }

    @GetMapping
    @Operation(summary = "반 목록 조회", description = "로그인한 Teacher의 반 목록을 조회한다. isActive 파라미터로 필터링 가능.")
    @PreAuthorize("hasAuthority('TEACHER')")
    public RsData<List<CourseResponse>> getCourses(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(required = false) Boolean isActive
    ) {
        if (principal == null) {
            throw new BusinessException(RsCode.UNAUTHENTICATED);
        }
        List<CourseResponse> courses = courseService.getCoursesByTeacher(principal.id(), isActive);
        return RsData.from(RsCode.SUCCESS, courses);
    }

    @GetMapping("/{courseId}")
    @Operation(summary = "반 상세 조회", description = "특정 반의 상세 정보를 조회한다.")
    @PreAuthorize("hasAuthority('TEACHER')")
    public RsData<CourseResponse> getCourse(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID courseId
    ) {
        if (principal == null) {
            throw new BusinessException(RsCode.UNAUTHENTICATED);
        }
        CourseResponse response = courseService.getCourseById(courseId, principal.id());
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PatchMapping("/{courseId}")
    @Operation(summary = "반 수정", description = "반 정보를 수정한다.")
    @PreAuthorize("hasAuthority('TEACHER')")
    public RsData<CourseResponse> updateCourse(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID courseId,
            @Valid @RequestBody CourseUpdateRequest request
    ) {
        if (principal == null) {
            throw new BusinessException(RsCode.UNAUTHENTICATED);
        }
        CourseResponse response = courseService.updateCourse(courseId, principal.id(), request);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PatchMapping("/{courseId}/deactivate")
    @Operation(summary = "반 비활성화", description = "반을 비활성화한다.")
    @PreAuthorize("hasAuthority('TEACHER')")
    public RsData<?> deactivateCourse(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID courseId
    ) {
        if (principal == null) {
            throw new BusinessException(RsCode.UNAUTHENTICATED);
        }
        courseService.deactivateCourse(courseId, principal.id());
        return RsData.from(RsCode.SUCCESS);
    }

    @PatchMapping("/{courseId}/activate")
    @Operation(summary = "반 활성화", description = "비활성화된 반을 다시 활성화한다.")
    @PreAuthorize("hasAuthority('TEACHER')")
    public RsData<?> activateCourse(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID courseId
    ) {
        if (principal == null) {
            throw new BusinessException(RsCode.UNAUTHENTICATED);
        }
        courseService.activateCourse(courseId, principal.id());
        return RsData.from(RsCode.SUCCESS);
    }
}
