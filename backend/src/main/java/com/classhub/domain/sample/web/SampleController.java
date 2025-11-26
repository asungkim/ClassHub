package com.classhub.domain.sample.web;

import com.classhub.domain.sample.application.SampleService;
import com.classhub.domain.sample.dto.SampleResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sample")
public class SampleController {

    private final SampleService sampleService;

    @GetMapping("/ping")
    public RsData<SampleResponse> ping() {
        return RsData.from(RsCode.SUCCESS, sampleService.ping());
    }
}
