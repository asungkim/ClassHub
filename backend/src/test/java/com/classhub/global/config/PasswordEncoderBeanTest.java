package com.classhub.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PLAN 요구사항: BCrypt PasswordEncoder Bean을 노출하고 encode/matches가 동작해야 한다.
 */
@SpringBootTest
class PasswordEncoderBeanTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void passwordEncoderBeanShouldEncodeAndMatch() {
        String raw = "classhub-secret";
        String encoded = passwordEncoder.encode(raw);

        assertThat(encoded).isNotEqualTo(raw);
        assertThat(passwordEncoder.matches(raw, encoded)).isTrue();
    }
}
