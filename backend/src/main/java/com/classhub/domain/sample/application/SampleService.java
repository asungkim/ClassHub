package com.classhub.domain.sample.application;

import com.classhub.domain.sample.dto.SampleResponse;
import org.springframework.stereotype.Service;

@Service
public class SampleService {

    public SampleResponse ping() {
        return new SampleResponse("pong", java.time.LocalDateTime.now());
    }
}
