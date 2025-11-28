package com.classhub.domain.auth.token;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private final Map<String, LocalDateTime> blacklist = new ConcurrentHashMap<>();

    @Override
    public void blacklist(String token, LocalDateTime expiresAt) {
        evictExpired();
        blacklist.put(token, expiresAt);
    }

    @Override
    public boolean isBlacklisted(String token) {
        evictExpired();
        LocalDateTime expiresAt = blacklist.get(token);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    @Override
    public void blacklistAllForMember(UUID memberId) {
        // TODO: Redis 기반 저장소 도입 시 실제 토큰들을 추적해 전체 로그아웃을 구현한다.
    }

    public void clear() {
        blacklist.clear();
    }

    private void evictExpired() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        blacklist.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}
