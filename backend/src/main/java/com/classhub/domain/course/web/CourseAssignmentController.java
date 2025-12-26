package com.classhub.domain.course.web;

import com.classhub.domain.course.application.CourseAssignmentService;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.dto.response.CourseStudentResponse;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.dto.response.StudentSummaryResponse;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/courses")
@Tag(name = "Course Assignment API", description = "반 배치 지원 API")
public class CourseAssignmentController {

    private final CourseAssignmentService courseAssignmentService;

    @GetMapping("/assignable")
    @PreAuthorize("hasAnyAuthority('TEACHER','ASSISTANT')")
    @Operation(summary = "배치 가능한 반 목록 조회")
    public RsData<PageResponse<CourseResponse>> getAssignableCourses(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        PageResponse<CourseResponse> response = courseAssignmentService.getAssignableCourses(
                principal,
                branchId,
                keyword,
                page,
                size
        );
        return RsData.from(RsCode.SUCCESS, response);
    }

    @GetMapping("/{courseId}/assignment-candidates")
    @PreAuthorize("hasAnyAuthority('TEACHER','ASSISTANT')")
    @Operation(summary = "반 배치 후보 학생 조회")
    public RsData<PageResponse<StudentSummaryResponse>> getAssignmentCandidates(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID courseId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        PageResponse<StudentSummaryResponse> response = courseAssignmentService.getAssignmentCandidates(
                principal,
                courseId,
                keyword,
                page,
                size
        );
        return RsData.from(RsCode.SUCCESS, response);
    }

    @GetMapping("/{courseId}/students")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "반 수강 학생 조회")
    public RsData<PageResponse<CourseStudentResponse>> getCourseStudents(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID courseId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        PageResponse<CourseStudentResponse> response = courseAssignmentService.getCourseStudents(
                principal,
                courseId,
                page,
                size
        );
        return RsData.from(RsCode.SUCCESS, response);
    }
}
