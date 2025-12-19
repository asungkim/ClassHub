package com.classhub.domain.company.branch.web;

import com.classhub.domain.company.branch.application.BranchCommandService;
import com.classhub.domain.company.branch.application.BranchQueryService;
import com.classhub.domain.company.branch.dto.request.BranchCreateRequest;
import com.classhub.domain.company.branch.dto.request.BranchUpdateRequest;
import com.classhub.domain.company.branch.dto.request.BranchVerifiedStatusRequest;
import com.classhub.domain.company.branch.dto.response.BranchResponse;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Branch API", description = "지점(Branch) 생성 및 검증 API")
public class BranchController {

    private final BranchCommandService branchCommandService;
    private final BranchQueryService branchQueryService;

    @PostMapping("/branches")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "지점 생성", description = "Teacher가 기존 학원에 새로운 지점을 추가한다.")
    public RsData<BranchResponse> createBranch(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody BranchCreateRequest request
    ) {
        BranchResponse response = branchCommandService.createBranch(principal.id(), request);
        return RsData.from(RsCode.CREATED, response);
    }

    @GetMapping("/branches")
    @PreAuthorize("hasAnyAuthority('TEACHER','SUPER_ADMIN')")
    @Operation(summary = "지점 목록 조회", description = "Teacher 또는 SuperAdmin이 상태/키워드/회사 기준으로 지점을 조회한다.")
    public RsData<PageResponse<BranchResponse>> getBranches(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(name = "companyId", required = false) UUID companyId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Pageable pageable = buildPageable(page, size);
        VerifiedStatus verifiedStatus = parseStatus(status);

        PageResponse<BranchResponse> response;
        if (principal.role() == MemberRole.SUPER_ADMIN) {
            response = branchQueryService.getBranchesForAdmin(companyId, verifiedStatus, keyword, pageable);
        } else if (principal.role() == MemberRole.TEACHER) {
            response = branchQueryService.getBranchesForTeacher(principal.id(), companyId, verifiedStatus, keyword, pageable);
        } else {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PatchMapping("/branches/{branchId}")
    @PreAuthorize("hasAnyAuthority('TEACHER','SUPER_ADMIN')")
    @Operation(summary = "지점 정보 수정", description = "교사(OWNER) 또는 SuperAdmin이 지점 이름/활성 여부를 수정한다.")
    public RsData<BranchResponse> updateBranch(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID branchId,
            @Valid @RequestBody BranchUpdateRequest request
    ) {
        BranchResponse response = branchCommandService.updateBranch(principal.id(), principal.role(), branchId, request);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PatchMapping("/branches/{branchId}/verified-status")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "지점 검증 상태 변경", description = "SuperAdmin이 지점의 verified 상태 및 활성화를 토글한다.")
    public RsData<BranchResponse> updateBranchVerifiedStatus(
            @PathVariable UUID branchId,
            @Valid @RequestBody BranchVerifiedStatusRequest request
    ) {
        BranchResponse response = branchCommandService.updateBranchVerifiedStatus(branchId, request);
        return RsData.from(RsCode.SUCCESS, response);
    }

    private VerifiedStatus parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }
        try {
            return VerifiedStatus.valueOf(rawStatus.toUpperCase(Locale.US));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
    }

    private Pageable buildPageable(int page, int size) {
        if (page < 0 || size <= 0) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        return PageRequest.of(page, size);
    }
}
