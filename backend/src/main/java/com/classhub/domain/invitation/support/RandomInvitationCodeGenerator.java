package com.classhub.domain.invitation.support;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class RandomInvitationCodeGenerator implements InvitationCodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String generate() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}
