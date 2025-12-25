package com.classhub.domain.studentcourse.web;

import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.studentcourse.application.StudentClinicContextQueryService;
import com.classhub.domain.studentcourse.dto.response.StudentClinicContextResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students/me")
@RequiredArgsConstructor
@Tag(name = "Student Clinic Context API", description = "학생 클리닉 컨텍스트 조회 API")
public class StudentClinicContextController {

    private final StudentClinicContextQueryService queryService;

    @GetMapping("/clinic-contexts")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "학생 클리닉 컨텍스트 조회")
    public RsData<List<StudentClinicContextResponse>> getClinicContexts(
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        List<StudentClinicContextResponse> response = queryService.getContexts(principal.id());
        return RsData.from(RsCode.SUCCESS, response);
    }
}
