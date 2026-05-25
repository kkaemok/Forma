package org.kkaemok.forma.item;

import java.util.Locale;

public enum FormaItemBehaviorType {
    SOUND,
    MESSAGE,
    COMMAND;

    public static FormaItemBehaviorType fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
