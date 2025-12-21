package com.classhub.domain.calendar.web;

import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.calendar.application.StudentCalendarService;
import com.classhub.domain.calendar.dto.StudentCalendarResponse;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Student Calendar API", description = "학생 월간 캘린더 조회 API")
public class StudentCalendarController {

    private final StudentCalendarService studentCalendarService;

    @GetMapping("/students/{studentId}/calendar")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ASSISTANT')")
    @Operation(summary = "학생 월간 캘린더 조회")
    public RsData<StudentCalendarResponse> getStudentCalendar(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID studentId,
            @RequestParam("year") int year,
            @RequestParam("month") int month
    ) {
        StudentCalendarResponse response = studentCalendarService.getStudentCalendar(principal, studentId, year, month);
        return RsData.from(RsCode.SUCCESS, response);
    }
}
