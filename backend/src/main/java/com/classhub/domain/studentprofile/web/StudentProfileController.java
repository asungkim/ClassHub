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
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    @PostMapping
    public RsData<StudentProfileResponse> createStudentProfile(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody StudentProfileCreateRequest request
    ) {
        StudentProfileResponse response = studentProfileService.createProfile(principal.id(), request);
        return RsData.from(RsCode.CREATED, response);
    }

    @GetMapping
    public RsData<PageResponse<StudentProfileSummary>> getStudentProfiles(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(value = "courseId", required = false) UUID courseId,
            @RequestParam(value = "name", required = false) String name,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        StudentProfileSearchCondition condition = new StudentProfileSearchCondition(courseId, name);
        PageResponse<StudentProfileSummary> body = PageResponse.from(
                studentProfileService.getProfiles(principal.id(), condition, pageable)
        );
        return RsData.from(RsCode.SUCCESS, body);
    }

    @GetMapping("/{profileId}")
    public RsData<StudentProfileResponse> getStudentProfile(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID profileId
    ) {
        StudentProfileResponse response = studentProfileService.getProfile(principal.id(), profileId);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PatchMapping("/{profileId}")
    public RsData<StudentProfileResponse> updateStudentProfile(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID profileId,
            @Valid @RequestBody StudentProfileUpdateRequest request
    ) {
        StudentProfileResponse response = studentProfileService.updateProfile(principal.id(), profileId, request);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @DeleteMapping("/{profileId}")
    public RsData<Void> deleteStudentProfile(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID profileId
    ) {
        studentProfileService.deleteProfile(principal.id(), profileId);
        return RsData.from(RsCode.SUCCESS, null);
    }
}
