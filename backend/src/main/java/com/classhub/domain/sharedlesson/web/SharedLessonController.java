package com.classhub.domain.sharedlesson.web;

import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.sharedlesson.application.SharedLessonService;
import com.classhub.domain.sharedlesson.dto.request.SharedLessonCreateRequest;
import com.classhub.domain.sharedlesson.dto.request.SharedLessonUpdateRequest;
import com.classhub.domain.sharedlesson.dto.response.SharedLessonResponse;
import com.classhub.domain.sharedlesson.dto.response.SharedLessonSummary;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/v1/shared-lessons")
@RequiredArgsConstructor
@Tag(name = "SharedLesson API", description = "Course 공통 진도 CRUD API")
public class SharedLessonController {

    private final SharedLessonService sharedLessonService;

    @PostMapping
    @Operation(summary = "SharedLesson 생성", description = "Course에 공통 진도를 작성한다.")
    @PreAuthorize("hasAuthority('TEACHER')")
    public RsData<SharedLessonResponse> createSharedLesson(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody SharedLessonCreateRequest request
    ) {
        SharedLessonResponse response = sharedLessonService.createLesson(principal.id(), request);
        return RsData.from(RsCode.CREATED, response);
    }

    @GetMapping
    @Operation(summary = "SharedLesson 목록", description = "Course ID로 공통 진도 목록을 조회한다.")
    @PreAuthorize("hasAuthority('TEACHER')")
    public RsData<PageResponse<SharedLessonSummary>> getSharedLessons(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam("courseId") UUID courseId,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<SharedLessonSummary> body = PageResponse.from(
                sharedLessonService.getLessons(principal.id(), courseId, from, to, pageable)
        );
        return RsData.from(RsCode.SUCCESS, body);
    }

    @GetMapping("/{lessonId}")
    @Operation(summary = "SharedLesson 상세", description = "공통 진도 상세를 조회한다.")
    @PreAuthorize("hasAuthority('TEACHER')")
    public RsData<SharedLessonResponse> getSharedLesson(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID lessonId
    ) {
        SharedLessonResponse response = sharedLessonService.getLesson(principal.id(), lessonId);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PatchMapping("/{lessonId}")
    @Operation(summary = "SharedLesson 수정", description = "공통 진도의 날짜/제목/내용을 수정한다.")
    @PreAuthorize("hasAuthority('TEACHER')")
    public RsData<SharedLessonResponse> updateSharedLesson(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID lessonId,
            @Valid @RequestBody SharedLessonUpdateRequest request
    ) {
        SharedLessonResponse response = sharedLessonService.updateLesson(principal.id(), lessonId, request);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @DeleteMapping("/{lessonId}")
    @Operation(summary = "SharedLesson 삭제", description = "공통 진도를 삭제한다.")
    @PreAuthorize("hasAuthority('TEACHER')")
    public RsData<Void> deleteSharedLesson(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID lessonId
    ) {
        sharedLessonService.deleteLesson(principal.id(), lessonId);
        return RsData.from(RsCode.SUCCESS, null);
    }
}
