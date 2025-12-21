package com.classhub.domain.progress.personal.web;

import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.progress.dto.ProgressSliceResponse;
import com.classhub.domain.progress.personal.application.PersonalProgressService;
import com.classhub.domain.progress.personal.dto.request.PersonalProgressCreateRequest;
import com.classhub.domain.progress.personal.dto.request.PersonalProgressUpdateRequest;
import com.classhub.domain.progress.personal.dto.response.PersonalProgressResponse;
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
@Tag(name = "Personal Progress API", description = "학생 개별 진도 관리 API")
public class PersonalProgressController {

    private final PersonalProgressService personalProgressService;

    @PostMapping("/student-courses/{recordId}/personal-progress")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "개별 진도 생성")
    public RsData<PersonalProgressResponse> createPersonalProgress(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID recordId,
            @Valid @RequestBody PersonalProgressCreateRequest request
    ) {
        PersonalProgressResponse response = personalProgressService.createPersonalProgress(principal, recordId, request);
        return RsData.from(RsCode.CREATED, response);
    }

    @GetMapping("/student-courses/{recordId}/personal-progress")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT')")
    @Operation(summary = "개별 진도 목록 조회")
    public RsData<ProgressSliceResponse<PersonalProgressResponse>> getPersonalProgresses(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID recordId,
            @RequestParam(name = "cursorCreatedAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursorCreatedAt,
            @RequestParam(name = "cursorId", required = false) UUID cursorId,
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        ProgressSliceResponse<PersonalProgressResponse> response = personalProgressService.getPersonalProgresses(
                principal,
                recordId,
                cursorCreatedAt,
                cursorId,
                limit
        );
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PatchMapping("/personal-progress/{progressId}")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "개별 진도 수정")
    public RsData<PersonalProgressResponse> updatePersonalProgress(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID progressId,
            @Valid @RequestBody PersonalProgressUpdateRequest request
    ) {
        PersonalProgressResponse response = personalProgressService.updatePersonalProgress(principal, progressId, request);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @DeleteMapping("/personal-progress/{progressId}")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "개별 진도 삭제")
    public RsData<Void> deletePersonalProgress(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID progressId
    ) {
        personalProgressService.deletePersonalProgress(principal, progressId);
        return RsData.from(RsCode.SUCCESS, null);
    }
}
