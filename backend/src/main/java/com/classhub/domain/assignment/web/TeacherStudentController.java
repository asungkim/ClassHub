package com.classhub.domain.assignment.web;

import com.classhub.domain.assignment.application.TeacherStudentService;
import com.classhub.domain.assignment.dto.response.TeacherStudentDetailResponse;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.dto.response.StudentSummaryResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teacher-students")
@Tag(name = "Teacher Student API", description = "선생님 학생 관리 API")
public class TeacherStudentController {

    private final TeacherStudentService teacherStudentService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('TEACHER','ASSISTANT')")
    @Operation(summary = "학생 목록 조회")
    public RsData<PageResponse<StudentSummaryResponse>> getTeacherStudents(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(name = "courseId", required = false) UUID courseId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        PageResponse<StudentSummaryResponse> response = teacherStudentService.getTeacherStudents(
                principal,
                courseId,
                keyword,
                page,
                size
        );
        return RsData.from(RsCode.SUCCESS, response);
    }

    @GetMapping("/{studentId}")
    @PreAuthorize("hasAnyAuthority('TEACHER','ASSISTANT')")
    @Operation(summary = "선생님 학생 상세 조회")
    public RsData<TeacherStudentDetailResponse> getTeacherStudentDetail(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID studentId
    ) {
        TeacherStudentDetailResponse response = teacherStudentService.getTeacherStudentDetail(principal, studentId);
        return RsData.from(RsCode.SUCCESS, response);
    }
}
