package com.classhub.domain.company.company.web;

import com.classhub.domain.company.company.application.CompanyCommandService;
import com.classhub.domain.company.company.application.CompanyQueryService;
import com.classhub.domain.company.company.dto.request.CompanyCreateRequest;
import com.classhub.domain.company.company.dto.request.CompanyVerifiedStatusRequest;
import com.classhub.domain.company.company.dto.response.CompanyResponse;
import com.classhub.domain.company.company.model.CompanyType;
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
@Tag(name = "Company API", description = "학원(Company) 등록 및 검증 API")
public class CompanyController {

    private final CompanyCommandService companyCommandService;
    private final CompanyQueryService companyQueryService;

    @PostMapping("/companies")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "학원 생성", description = "Teacher가 INDIVIDUAL 또는 ACADEMY 학원을 등록한다.")
    public RsData<CompanyResponse> createCompany(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody CompanyCreateRequest request
    ) {
        UUID teacherId = requireRole(principal, MemberRole.TEACHER);
        CompanyResponse response = companyCommandService.createCompany(teacherId, request);
        return RsData.from(RsCode.CREATED, response);
    }

    @GetMapping("/companies")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Teacher 학원 목록 조회", description = "교사가 접근 가능한 학원 목록을 상태/타입/키워드로 필터링해 조회한다.")
    public RsData<PageResponse<CompanyResponse>> getCompaniesForTeacher(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        UUID teacherId = requireRole(principal, MemberRole.TEACHER);
        VerifiedStatus verifiedStatus = parseVerifiedStatus(status);
        CompanyType companyType = parseCompanyType(type);
        Pageable pageable = buildPageable(page, size);

        PageResponse<CompanyResponse> response = companyQueryService.getCompaniesForTeacher(
                teacherId,
                verifiedStatus,
                companyType,
                keyword,
                pageable
        );
        return RsData.from(RsCode.SUCCESS, response);
    }

    @GetMapping("/admin/companies")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "SuperAdmin 학원 목록 조회", description = "SuperAdmin이 전체 학원 목록을 검증 상태로 필터링해 조회한다.")
    public RsData<PageResponse<CompanyResponse>> getCompaniesForAdmin(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        requireRole(principal, MemberRole.SUPER_ADMIN);
        VerifiedStatus verifiedStatus = parseVerifiedStatus(status);
        CompanyType companyType = parseCompanyType(type);
        Pageable pageable = buildPageable(page, size);

        PageResponse<CompanyResponse> response = companyQueryService.getCompaniesForAdmin(
                verifiedStatus,
                companyType,
                keyword,
                pageable
        );
        return RsData.from(RsCode.SUCCESS, response);
    }

    @GetMapping("/admin/companies/{companyId}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "학원 상세 조회", description = "SuperAdmin이 특정 학원 상세 정보를 조회한다.")
    public RsData<CompanyResponse> getCompanyDetail(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID companyId
    ) {
        requireRole(principal, MemberRole.SUPER_ADMIN);
        CompanyResponse response = companyQueryService.getCompany(companyId);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PatchMapping("/admin/companies/{companyId}/verified-status")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "학원 검증 상태 변경", description = "SuperAdmin이 학원의 검증/활성 상태를 변경한다.")
    public RsData<CompanyResponse> updateCompanyVerifiedStatus(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID companyId,
            @Valid @RequestBody CompanyVerifiedStatusRequest request
    ) {
        requireRole(principal, MemberRole.SUPER_ADMIN);
        CompanyResponse response = companyCommandService.updateCompanyVerifiedStatus(companyId, request);
        return RsData.from(RsCode.SUCCESS, response);
    }

    private UUID requireRole(MemberPrincipal principal, MemberRole requiredRole) {
        if (principal == null || principal.role() != requiredRole) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        return principal.id();
    }

    private VerifiedStatus parseVerifiedStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }
        try {
            return VerifiedStatus.valueOf(rawStatus.toUpperCase(Locale.US));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
    }

    private CompanyType parseCompanyType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return null;
        }
        try {
            return CompanyType.valueOf(rawType.toUpperCase(Locale.US));
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
