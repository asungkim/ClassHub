package com.classhub.global.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class KstTime {

    public static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.system(ZONE_ID);

    private KstTime() {
    }

    public static Clock clock() {
        return CLOCK;
    }

    public static LocalDate nowDate() {
        return LocalDate.now(CLOCK);
    }

    public static LocalDateTime nowDateTime() {
        return LocalDateTime.now(CLOCK);
    }
}
