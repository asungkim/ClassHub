package com.classhub.domain.auth.token;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryRefreshTokenStoreTest {

    private InMemoryRefreshTokenStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryRefreshTokenStore();
    }

    @Test
    void blacklistAndCheck() {
        LocalDateTime expires = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10);
        store.blacklist("token-1", expires);

        assertThat(store.isBlacklisted("token-1")).isTrue();
        assertThat(store.isBlacklisted("token-2")).isFalse();
    }

    @Test
    void evictExpiredTokens() {
        LocalDateTime expires = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1);
        store.blacklist("expired", expires);

        assertThat(store.isBlacklisted("expired")).isFalse();
    }

    @Test
    void clearRemovesAllTokens() {
        LocalDateTime expires = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5);
        store.blacklist("token-1", expires);
        store.clear();

        assertThat(store.isBlacklisted("token-1")).isFalse();
    }
}
