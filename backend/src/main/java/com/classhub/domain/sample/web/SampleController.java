package com.classhub.domain.sample.web;

import com.classhub.domain.sample.application.SampleService;
import com.classhub.domain.sample.dto.SampleResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sample")
@Tag(name = "Sample API", description = "샘플 핑 테스트 API")
public class SampleController {

    private final SampleService sampleService;

    @GetMapping("/ping")
    @Operation(summary = "샘플 핑", description = "샘플 헬스 체크 API")
    public RsData<SampleResponse> ping() {
        return RsData.from(RsCode.SUCCESS, sampleService.ping());
    }
}
