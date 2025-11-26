package com.classhub.global.config;

import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    public WebConfig() {
        Locale.setDefault(Locale.KOREA);
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Asia/Seoul")));
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000", "https://*.classhub" )
                .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true);
    }
}
