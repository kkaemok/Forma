package org.kkaemok.forma.item;

import java.util.Locale;

public enum FormaItemType {
    ITEM,
    WEAPON,
    TOOL,
    ARMOR,
    CONSUMABLE,
    BLOCK_ITEM;

    public boolean supportsDurability() {
        return this == WEAPON || this == TOOL || this == ARMOR;
    }

    public boolean usesHandheldModel() {
        return this == WEAPON || this == TOOL;
    }

    public static FormaItemType fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return FormaItemType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
