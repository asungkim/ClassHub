package com.classhub.domain.member.web;

import com.classhub.domain.member.application.TeacherSearchService;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.dto.response.TeacherSearchResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
@Tag(name = "Teacher Search API", description = "학생 선생님 검색 API")
public class TeacherSearchController {

    private final TeacherSearchService teacherSearchService;

    @GetMapping
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "학생 선생님 검색")
    public RsData<PageResponse<TeacherSearchResponse>> searchTeachers(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "companyId", required = false) UUID companyId,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        PageResponse<TeacherSearchResponse> response = teacherSearchService.searchTeachers(
                principal.id(),
                keyword,
                companyId,
                branchId,
                page,
                size
        );
        return RsData.from(RsCode.SUCCESS, response);
    }
}
