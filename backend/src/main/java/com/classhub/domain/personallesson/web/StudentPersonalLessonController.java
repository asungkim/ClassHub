package com.classhub.domain.personallesson.web;

import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.personallesson.application.PersonalLessonService;
import com.classhub.domain.personallesson.dto.response.PersonalLessonSummary;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student-profiles/{profileId}/personal-lessons")
@RequiredArgsConstructor
@Tag(name = "Student PersonalLesson API", description = "학생별 개별 진도 조회 API")
public class StudentPersonalLessonController {

    private final PersonalLessonService personalLessonService;

    @GetMapping
    @Operation(summary = "학생별 PersonalLesson 목록", description = "특정 학생 프로필에 대한 개별 진도를 조회한다.")
    public RsData<PageResponse<PersonalLessonSummary>> getLessonsByProfile(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable("profileId") UUID profileId,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<PersonalLessonSummary> body = PageResponse.from(
                personalLessonService.getLessons(principal.id(), profileId, from, to, pageable)
        );
        return RsData.from(RsCode.SUCCESS, body);
    }
}
