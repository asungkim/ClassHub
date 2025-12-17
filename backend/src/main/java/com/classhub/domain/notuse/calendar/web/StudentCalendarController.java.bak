package com.classhub.domain.calendar.web;

import com.classhub.domain.calendar.application.StudentCalendarQueryService;
import com.classhub.domain.calendar.dto.response.StudentCalendarResponse;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Tag(name = "Student Calendar API", description = "학생별 월간 캘린더 조회 API")
public class StudentCalendarController {

    private final StudentCalendarQueryService studentCalendarQueryService;

    @GetMapping("/{studentId}/calendar")
    @Operation(summary = "학생 캘린더 조회", description = "SharedLesson과 PersonalLesson을 통합한 학생 월간 캘린더를 조회한다.")
    @PreAuthorize("hasAnyAuthority('TEACHER','ASSISTANT')")
    public RsData<StudentCalendarResponse> getStudentCalendar(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable("studentId") UUID studentId,
            @RequestParam("year") int year,
            @RequestParam("month") int month
    ) {
        if (principal == null) {
            throw RsCode.UNAUTHORIZED.toException();
        }
        StudentCalendarResponse response = studentCalendarQueryService.getMonthlyCalendar(
                principal.id(),
                studentId,
                year,
                month
        );
        return RsData.from(RsCode.SUCCESS, response);
    }
}
