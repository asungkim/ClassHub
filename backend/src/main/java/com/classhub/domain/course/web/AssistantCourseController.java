package com.classhub.domain.course.web;

import com.classhub.domain.course.application.AssistantCourseService;
import com.classhub.domain.course.dto.CourseStatusFilter;
import com.classhub.domain.course.dto.response.CourseWithTeacherResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assistants/me/courses")
@RequiredArgsConstructor
@Tag(name = "Assistant Course API")
public class AssistantCourseController {

    private final AssistantCourseService assistantCourseService;

    @GetMapping
    @PreAuthorize("hasAuthority('ASSISTANT')")
    @Operation(summary = "조교 Course 목록 조회")
    public RsData<PageResponse<CourseWithTeacherResponse>> getCourses(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(name = "teacherId", required = false) UUID teacherId,
            @RequestParam(name = "status", required = false) String rawStatus,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        UUID assistantId = requireAssistant(principal);
        CourseStatusFilter filter = parseStatus(rawStatus);
        PageResponse<CourseWithTeacherResponse> response = assistantCourseService.getCourses(
                assistantId,
                teacherId,
                filter,
                keyword,
                page,
                size
        );
        return RsData.from(RsCode.SUCCESS, response);
    }

    private UUID requireAssistant(MemberPrincipal principal) {
        if (principal == null || principal.role() != MemberRole.ASSISTANT) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        return principal.id();
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
