package org.kkaemok.forma.item;

import java.util.Locale;

public record FormaItemBehavior(
        FormaItemBehaviorType type,
        String sound,
        float volume,
        float pitch,
        String message,
        String command,
        CommandSenderType sender,
        long cooldownMillis
) {
    public enum CommandSenderType {
        CONSOLE,
        PLAYER;

        public static CommandSenderType fromString(String raw) {
            if (raw == null || raw.isBlank()) {
                return CONSOLE;
            }
            try {
                return valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }
}
