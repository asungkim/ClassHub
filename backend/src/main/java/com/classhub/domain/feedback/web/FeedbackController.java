package com.classhub.domain.feedback.web;

import com.classhub.domain.feedback.application.FeedbackService;
import com.classhub.domain.feedback.dto.request.FeedbackCreateRequest;
import com.classhub.domain.feedback.dto.response.FeedbackResponse;
import com.classhub.domain.feedback.model.FeedbackStatus;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Locale;
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
@RequiredArgsConstructor
@RequestMapping("/api/v1/feedback")
@Tag(name = "Feedback API", description = "피드백 작성 및 관리 API")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('TEACHER','ASSISTANT','STUDENT')")
    @Operation(summary = "피드백 작성", description = "Teacher/Assistant/Student가 피드백을 등록한다.")
    public RsData<FeedbackResponse> createFeedback(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody FeedbackCreateRequest request
    ) {
        FeedbackResponse response = feedbackService.createFeedback(principal.id(), request);
        return RsData.from(RsCode.CREATED, response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "피드백 목록 조회", description = "SuperAdmin이 전체 피드백을 상태로 필터링해 조회한다.")
    public RsData<PageResponse<FeedbackResponse>> getFeedbacksForAdmin(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        FeedbackStatus feedbackStatus = parseStatus(status);
        PageResponse<FeedbackResponse> response = feedbackService.getFeedbacksForAdmin(feedbackStatus, page, size);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyAuthority('TEACHER','ASSISTANT','STUDENT')")
    @Operation(summary = "내 피드백 목록 조회", description = "작성자가 본인 피드백 목록을 상태로 필터링해 조회한다.")
    public RsData<PageResponse<FeedbackResponse>> getMyFeedbacks(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        FeedbackStatus feedbackStatus = parseStatus(status);
        PageResponse<FeedbackResponse> response = feedbackService.getMyFeedbacks(principal.id(), feedbackStatus, page,
                size);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PatchMapping("/{feedbackId}/resolve")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "피드백 해결 처리", description = "SuperAdmin이 피드백을 해결 상태로 전환한다.")
    public RsData<FeedbackResponse> resolveFeedback(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID feedbackId
    ) {
        FeedbackResponse response = feedbackService.resolveFeedback(feedbackId, principal.id());
        return RsData.from(RsCode.SUCCESS, response);
    }

    private FeedbackStatus parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }
        if ("ALL".equalsIgnoreCase(rawStatus)) {
            return null;
        }
        try {
            return FeedbackStatus.valueOf(rawStatus.toUpperCase(Locale.US));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
    }
}
