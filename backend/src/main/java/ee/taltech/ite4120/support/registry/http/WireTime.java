package ee.taltech.ite4120.support.registry.http;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/** Parses a registry's "data as of" text (ISO-8601 with offset). Missing or malformed → unknown (null). */
public final class WireTime {

    private WireTime() {}

    public static OffsetDateTime parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(text);
        } catch (DateTimeParseException e) {
            return null; // an unreadable timestamp is reported as unknown, never guessed
        }
    }
}
