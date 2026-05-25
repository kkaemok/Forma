package org.kkaemok.forma.block;

import java.util.Locale;

public enum FormaBlockType {
    NOTE_BLOCK,
    TRIPWIRE,
    DISPLAY;

    public static FormaBlockType fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return FormaBlockType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
