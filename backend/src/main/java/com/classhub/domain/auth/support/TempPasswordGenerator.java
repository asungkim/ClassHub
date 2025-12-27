package com.classhub.domain.auth.support;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class TempPasswordGenerator {

    private static final String PREFIX = "Classmate";
    private static final String SUFFIX = "!";
    private static final int DIGIT_BOUND = 10_000;
    private static final int DIGIT_LENGTH = 4;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        int number = secureRandom.nextInt(DIGIT_BOUND);
        return PREFIX + String.format("%0" + DIGIT_LENGTH + "d", number) + SUFFIX;
    }
}
