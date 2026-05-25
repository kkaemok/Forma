package org.kkaemok.forma.util;

import org.bukkit.Instrument;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class NoteBlockInstrumentUtil {
    private static final Map<String, Instrument> STATE_AND_ALIAS_MAP = createStateAndAliasMap();

    private NoteBlockInstrumentUtil() {
    }

    public static Instrument parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String trimmed = raw.trim();

        Instrument parsed = parseByEnumName(trimmed);
        if (parsed != null) {
            return parsed;
        }

        String enumStyle = trimmed.replace('-', '_').replace(' ', '_');
        parsed = parseByEnumName(enumStyle);
        if (parsed != null) {
            return parsed;
        }

        String collapsed = collapse(trimmed);
        return STATE_AND_ALIAS_MAP.get(collapsed);
    }

    public static String toBlockStateName(Instrument instrument) {
        return switch (instrument) {
            case PIANO -> "harp";
            case BASS_DRUM -> "basedrum";
            case SNARE_DRUM -> "snare";
            case STICKS -> "hat";
            case BASS_GUITAR -> "bass";
            default -> instrument.name().toLowerCase(Locale.ROOT);
        };
    }

    public static String availableInstrumentNames() {
        return Arrays.stream(Instrument.values())
                .map(Instrument::name)
                .sorted()
                .reduce((left, right) -> left + ", " + right)
                .orElse("(none)");
    }

    public static String bassDrumExamples() {
        return "BASS_DRUM | BASEDRUM | basedrum | bass_drum | bass-drum | bass drum";
    }

    private static Instrument parseByEnumName(String raw) {
        try {
            return Instrument.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String collapse(String raw) {
        return raw.toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }

    private static Map<String, Instrument> createStateAndAliasMap() {
        Map<String, Instrument> map = new LinkedHashMap<>();

        // blockstate ?쒗쁽 諛?蹂꾩묶
        map.put("harp", Instrument.PIANO);
        map.put("basedrum", Instrument.BASS_DRUM);
        map.put("bassdrum", Instrument.BASS_DRUM);
        map.put("snare", Instrument.SNARE_DRUM);
        map.put("hat", Instrument.STICKS);
        map.put("bass", Instrument.BASS_GUITAR);

        // 그 외 값은 enum 이름을 공백 없는 별칭 형태로 허용한다.
        for (Instrument instrument : Instrument.values()) {
            map.putIfAbsent(collapse(instrument.name()), instrument);
        }
        return Map.copyOf(map);
    }
}
