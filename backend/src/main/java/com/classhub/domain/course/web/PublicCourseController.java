package com.classhub.domain.course.web;

import com.classhub.domain.course.application.PublicCourseService;
import com.classhub.domain.course.dto.response.PublicCourseResponse;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courses/public")
@RequiredArgsConstructor
@Tag(name = "Public Course API")
public class PublicCourseController {

    private final PublicCourseService publicCourseService;

    @GetMapping
    @Operation(summary = "공개 Course 검색")
    public RsData<PageResponse<PublicCourseResponse>> getPublicCourses(
            @RequestParam(name = "companyId", required = false) UUID companyId,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "teacherId", required = false) UUID teacherId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "onlyVerified", defaultValue = "true") boolean onlyVerified,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        PageResponse<PublicCourseResponse> response = publicCourseService.searchCourses(
                companyId,
                branchId,
                teacherId,
                keyword,
                onlyVerified,
                page,
                size
        );
        return RsData.from(RsCode.SUCCESS, response);
    }
}
