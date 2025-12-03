package com.classhub.domain.personallesson.web;

import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.personallesson.application.PersonalLessonService;
import com.classhub.domain.personallesson.dto.request.PersonalLessonCreateRequest;
import com.classhub.domain.personallesson.dto.request.PersonalLessonUpdateRequest;
import com.classhub.domain.personallesson.dto.response.PersonalLessonResponse;
import com.classhub.domain.personallesson.dto.response.PersonalLessonSummary;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1/personal-lessons")
@RequiredArgsConstructor
public class PersonalLessonController {

    private final PersonalLessonService personalLessonService;

    @PostMapping
    public RsData<PersonalLessonResponse> createPersonalLesson(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody PersonalLessonCreateRequest request
    ) {
        PersonalLessonResponse response = personalLessonService.createLesson(principal.id(), request);
        return RsData.from(RsCode.CREATED, response);
    }

    @GetMapping
    public RsData<PageResponse<PersonalLessonSummary>> getPersonalLessons(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam("studentProfileId") UUID studentProfileId,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<PersonalLessonSummary> body = PageResponse.from(
                personalLessonService.getLessons(principal.id(), studentProfileId, from, to, pageable)
        );
        return RsData.from(RsCode.SUCCESS, body);
    }

    @GetMapping("/{lessonId}")
    public RsData<PersonalLessonResponse> getPersonalLesson(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID lessonId
    ) {
        PersonalLessonResponse response = personalLessonService.getLesson(principal.id(), lessonId);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PatchMapping("/{lessonId}")
    public RsData<PersonalLessonResponse> updatePersonalLesson(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID lessonId,
            @Valid @RequestBody PersonalLessonUpdateRequest request
    ) {
        PersonalLessonResponse response = personalLessonService.updateLesson(principal.id(), lessonId, request);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @DeleteMapping("/{lessonId}")
    public RsData<Void> deletePersonalLesson(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID lessonId
    ) {
        personalLessonService.deleteLesson(principal.id(), lessonId);
        return RsData.from(RsCode.SUCCESS, null);
    }
}
