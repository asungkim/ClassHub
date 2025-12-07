package com.classhub.domain.studentprofile.web;

import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.studentprofile.application.StudentProfileService;
import com.classhub.domain.studentprofile.dto.request.StudentProfileCreateRequest;
import com.classhub.domain.studentprofile.dto.request.StudentProfileSearchCondition;
import com.classhub.domain.studentprofile.dto.request.StudentProfileUpdateRequest;
import com.classhub.domain.studentprofile.dto.response.StudentProfileResponse;
import com.classhub.domain.studentprofile.dto.response.StudentProfileSummary;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/v1/student-profiles")
@RequiredArgsConstructor
@Tag(name = "StudentProfile API", description = "학생 프로필 CRUD API")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    @PostMapping
    @Operation(summary = "학생 프로필 생성", description = "Teacher 소유 Course에 학생 프로필을 등록한다.")
    @PreAuthorize("hasAuthority('TEACHER')")
    public RsData<StudentProfileResponse> createStudentProfile(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody StudentProfileCreateRequest request
    ) {
        StudentProfileResponse response = studentProfileService.createProfile(principal.id(), request);
        return RsData.from(RsCode.CREATED, response);
    }

    @GetMapping
    @Operation(summary = "학생 프로필 목록", description = "Teacher가 소유한 학생 프로필 목록을 페이징 조회한다.")
    @PreAuthorize("hasAnyAuthority('TEACHER','ASSISTANT')")
    public RsData<PageResponse<StudentProfileSummary>> getStudentProfiles(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(value = "courseId", required = false) UUID courseId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "active", required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        StudentProfileSearchCondition condition = new StudentProfileSearchCondition(courseId, name, active);
        PageResponse<StudentProfileSummary> body = PageResponse.from(
                studentProfileService.getProfiles(principal.id(), condition, pageable)
        );
        return RsData.from(RsCode.SUCCESS, body);
    }

    @GetMapping("/{profileId}")
    @Operation(summary = "학생 프로필 상세 조회", description = "프로필 ID로 학생 정보를 조회한다.")
    @PreAuthorize("hasAnyAuthority('TEACHER','ASSISTANT')")
    public RsData<StudentProfileResponse> getStudentProfile(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID profileId
    ) {
        StudentProfileResponse response = studentProfileService.getProfile(principal.id(), profileId);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PatchMapping("/{profileId}")
    @Operation(summary = "학생 프로필 수정", description = "학생 정보 및 담당 조교, 연락처 등을 수정한다.")
    @PreAuthorize("hasAuthority('TEACHER')")
    public RsData<StudentProfileResponse> updateStudentProfile(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID profileId,
            @Valid @RequestBody StudentProfileUpdateRequest request
    ) {
        StudentProfileResponse response = studentProfileService.updateProfile(principal.id(), profileId, request);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @DeleteMapping("/{profileId}")
    @Operation(summary = "학생 프로필 비활성화", description = "학생 프로필을 비활성 상태로 변경한다.")
    @PreAuthorize("hasAuthority('TEACHER')")
    public RsData<Void> deleteStudentProfile(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID profileId
    ) {
        studentProfileService.deleteProfile(principal.id(), profileId);
        return RsData.from(RsCode.SUCCESS, null);
    }
}
