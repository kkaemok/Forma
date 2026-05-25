package org.kkaemok.forma.util;

import org.bukkit.Instrument;

import java.util.List;

public final class NoteBlockVariationUtil {
    private static final int NOTES_PER_POWER_STATE = 25;
    private static final int POWER_STATES = 2;
    private static final int VARIATIONS_PER_INSTRUMENT = NOTES_PER_POWER_STATE * POWER_STATES;

    // Keep this order stable. Each instrument owns 50 variations.
    private static final List<Instrument> ORDERED_INSTRUMENTS = List.of(
            Instrument.BASS_DRUM,
            Instrument.SNARE_DRUM,
            Instrument.STICKS,
            Instrument.BASS_GUITAR,
            Instrument.FLUTE,
            Instrument.BELL,
            Instrument.GUITAR,
            Instrument.CHIME,
            Instrument.XYLOPHONE,
            Instrument.IRON_XYLOPHONE,
            Instrument.COW_BELL,
            Instrument.DIDGERIDOO,
            Instrument.BIT,
            Instrument.BANJO,
            Instrument.PLING,
            Instrument.PIANO
    );

    private NoteBlockVariationUtil() {
    }

    public static NoteBlockState fromVariation(int variation) {
        if (variation < 1) {
            return null;
        }
        int zeroBased = variation - 1;
        int instrumentIndex = zeroBased / VARIATIONS_PER_INSTRUMENT;
        if (instrumentIndex < 0 || instrumentIndex >= ORDERED_INSTRUMENTS.size()) {
            return null;
        }

        int slotInInstrument = zeroBased % VARIATIONS_PER_INSTRUMENT;
        boolean powered = slotInInstrument >= NOTES_PER_POWER_STATE;
        int note = slotInInstrument % NOTES_PER_POWER_STATE;
        Instrument instrument = ORDERED_INSTRUMENTS.get(instrumentIndex);

        return new NoteBlockState(instrument, note, powered);
    }

    public static int maxVariations() {
        return ORDERED_INSTRUMENTS.size() * VARIATIONS_PER_INSTRUMENT;
    }

    public static List<Instrument> orderedInstruments() {
        return ORDERED_INSTRUMENTS;
    }

    public static String toDebugString(int variation) {
        NoteBlockState state = fromVariation(variation);
        if (state == null) {
            return "variation=" + variation + " -> invalid (max=" + maxVariations() + ")";
        }
        return "variation=" + variation
                + " -> instrument=" + state.instrument().name()
                + ", note=" + state.note()
                + ", powered=" + state.powered()
                + ", blockstate=" + NoteBlockInstrumentUtil.toBlockStateName(state.instrument())
                + "," + state.note()
                + "," + state.powered();
    }
}
