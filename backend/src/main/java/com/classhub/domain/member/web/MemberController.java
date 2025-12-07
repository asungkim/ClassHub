package com.classhub.domain.member.web;

import com.classhub.domain.member.application.MemberService;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.dto.MemberSummary;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Tag(name = "Member API", description = "조교 목록 조회 및 비활성화 API")
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    @Operation(summary = "멤버 목록 조회 (조교 전용)", description = "Teacher가 소속 조교 목록을 조회한다.")    
    @PreAuthorize("hasAuthority('TEACHER')")
    public RsData<PageResponse<MemberSummary>> getAssistants(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam("role") String role,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "active", required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        if (!"ASSISTANT".equalsIgnoreCase(role)) {
            return RsData.from(RsCode.BAD_REQUEST, null);
        }
        PageResponse<MemberSummary> body = PageResponse.from(
                memberService.getAssistants(principal.id(), name, active, pageable)
        );
        return RsData.from(RsCode.SUCCESS, body);
    }

    @PatchMapping("/{memberId}/deactivate")
    @Operation(summary = "조교 비활성화", description = "Teacher가 소속 조교를 비활성화한다.")
    @PreAuthorize("hasAuthority('TEACHER')")
    public RsData<Void> deactivateAssistant(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID memberId
    ) {
        memberService.deactivateAssistant(principal.id(), memberId);
        return RsData.from(RsCode.SUCCESS, null);
    }

    @PatchMapping("/{memberId}/activate")
    @Operation(summary = "조교 활성화", description = "Teacher가 소속 조교를 활성 상태로 변경한다.")
    @PreAuthorize("hasAuthority('TEACHER')")
    public RsData<Void> activateAssistant(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID memberId
    ) {
        memberService.activateAssistant(principal.id(), memberId);
        return RsData.from(RsCode.SUCCESS, null);
    }
}
