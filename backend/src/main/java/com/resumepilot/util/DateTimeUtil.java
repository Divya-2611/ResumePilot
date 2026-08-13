package com.resumepilot.util;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Timestamp serialization helpers.
 * <p>The app stores times in UTC (see {@code hibernate.jdbc.time_zone=UTC}).
 * Plain {@code LocalDateTime.toString()} omits the zone, so browsers treat the
 * value as local time and relative timestamps ("5h ago") are wrong. Always
 * serialize with an explicit UTC marker ({@code Z}) instead.</p>
 */
public final class DateTimeUtil {

    private DateTimeUtil() {
    }

    /** Returns an ISO-8601 UTC string (e.g. {@code 2026-08-13T07:39:36.925Z}). */
    public static String toIsoString(LocalDateTime value) {
        return value != null ? value.toInstant(ZoneOffset.UTC).toString() : null;
    }
}
