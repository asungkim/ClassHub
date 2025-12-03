package com.classhub.domain.studentprofile.web;

import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.studentprofile.application.StudentProfileService;
import com.classhub.domain.studentprofile.dto.response.StudentProfileSummary;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/students")
@RequiredArgsConstructor
public class CourseStudentProfileController {

    private final StudentProfileService studentProfileService;

    @GetMapping
    public RsData<List<StudentProfileSummary>> getStudentsByCourse(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID courseId
    ) {
        List<StudentProfileSummary> summaries =
                studentProfileService.getCourseStudents(principal.id(), courseId);
        return RsData.from(RsCode.SUCCESS, summaries);
    }
}
