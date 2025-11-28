package com.classhub.domain.auth.token;

import java.time.LocalDateTime;
import java.util.UUID;

public interface RefreshTokenStore {

    void blacklist(String token, LocalDateTime expiresAt);

    boolean isBlacklisted(String token);

    void blacklistAllForMember(UUID memberId);
}
