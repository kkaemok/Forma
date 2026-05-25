package org.kkaemok.forma.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtil {
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)[&\\u00A7]#([0-9a-f]{6})");
    private static final Pattern LEGACY_PATTERN = Pattern.compile("(?i)[&\\u00A7]([0-9a-fk-or])");

    public static final String PREFIX_RAW = "<gradient:#A7F85B:#16A34A>Forma</gradient> &7\u00BB ";

    private TextUtil() {
    }

    public static Component prefixed(String message) {
        return parse(PREFIX_RAW + message);
    }

    public static Component parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Component.empty();
        }

        String normalized = toMiniMessage(raw);
        try {
            return MINI.deserialize(normalized);
        } catch (Exception ex) {
            return Component.text(raw);
        }
    }

    // Accept both ampersand and section-sign legacy color codes.
    private static String toMiniMessage(String raw) {
        Matcher hexMatcher = HEX_PATTERN.matcher(raw);
        StringBuilder hexBuffer = new StringBuilder();
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1).toUpperCase(Locale.ROOT);
            hexMatcher.appendReplacement(hexBuffer, "<#" + hex + ">");
        }
        hexMatcher.appendTail(hexBuffer);

        Matcher codeMatcher = LEGACY_PATTERN.matcher(hexBuffer.toString());
        StringBuilder result = new StringBuilder();
        while (codeMatcher.find()) {
            String replacement = legacyCodeToTag(codeMatcher.group(1).toLowerCase(Locale.ROOT));
            codeMatcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        codeMatcher.appendTail(result);
        return result.toString();
    }

    private static String legacyCodeToTag(String code) {
        return switch (code) {
            case "0" -> "<black>";
            case "1" -> "<dark_blue>";
            case "2" -> "<dark_green>";
            case "3" -> "<dark_aqua>";
            case "4" -> "<dark_red>";
            case "5" -> "<dark_purple>";
            case "6" -> "<gold>";
            case "7" -> "<gray>";
            case "8" -> "<dark_gray>";
            case "9" -> "<blue>";
            case "a" -> "<green>";
            case "b" -> "<aqua>";
            case "c" -> "<red>";
            case "d" -> "<light_purple>";
            case "e" -> "<yellow>";
            case "f" -> "<white>";
            case "k" -> "<obfuscated>";
            case "l" -> "<bold>";
            case "m" -> "<strikethrough>";
            case "n" -> "<underlined>";
            case "o" -> "<italic>";
            case "r" -> "<reset>";
            default -> "";
        };
    }
}
