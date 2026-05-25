package org.kkaemok.forma.block;

public record FormaBlockSounds(
        String place,
        String breakSound,
        String step,
        String hit,
        String fall,
        float volume,
        float pitch
) {
    public FormaBlockSounds {
        volume = Math.max(0.0F, volume);
        pitch = Math.max(0.0F, pitch);
    }

    public static FormaBlockSounds defaults() {
        return new FormaBlockSounds(null, null, null, null, null, 1.0F, 1.0F);
    }

    public String get(SoundAction action) {
        return switch (action) {
            case PLACE -> place;
            case BREAK -> breakSound;
            case STEP -> step;
            case HIT -> hit;
            case FALL -> fall;
        };
    }

    public enum SoundAction {
        PLACE,
        BREAK,
        STEP,
        HIT,
        FALL
    }
}
