package org.kkaemok.forma.util;

import org.bukkit.Instrument;

public record NoteBlockState(
        Instrument instrument,
        int note,
        boolean powered
) {
}
