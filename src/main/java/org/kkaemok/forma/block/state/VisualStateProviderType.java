package org.kkaemok.forma.block.state;

import java.util.Locale;

public enum VisualStateProviderType {
    NOTE_BLOCK,
    BROWN_MUSHROOM_BLOCK,
    SOLID;

    public static VisualStateProviderType fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public boolean isNoteBlockProvider() {
        return this == NOTE_BLOCK;
    }
}
