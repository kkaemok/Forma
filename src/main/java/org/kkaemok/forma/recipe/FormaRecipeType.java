package org.kkaemok.forma.recipe;

import java.util.Locale;

public enum FormaRecipeType {
    SHAPED,
    SHAPELESS;

    public static FormaRecipeType fromString(String raw) {
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
