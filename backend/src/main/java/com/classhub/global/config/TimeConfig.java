package com.classhub.global.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.classhub.global.util.KstTime;

@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return KstTime.clock();
    }
}
