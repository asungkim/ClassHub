package com.classhub.domain.studentcourse.web;

import com.classhub.domain.course.application.CourseAssignmentService;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.studentcourse.dto.request.StudentCourseAssignmentCreateRequest;
import com.classhub.domain.studentcourse.dto.response.StudentCourseAssignmentResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/student-course-assignments")
@Tag(name = "Student Course Assignment API", description = "학생 반 배치 API")
public class StudentCourseAssignmentController {

    private final CourseAssignmentService courseAssignmentService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('TEACHER','ASSISTANT')")
    @Operation(summary = "학생 반 배치 생성")
    public RsData<StudentCourseAssignmentResponse> createAssignment(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody StudentCourseAssignmentCreateRequest request
    ) {
        StudentCourseAssignmentResponse response = courseAssignmentService.createAssignment(principal, request);
        return RsData.from(RsCode.CREATED, response);
    }
}
