package org.kkaemok.forma.block.state;

public final class VisualStateProviders {
    private static final NoteBlockVisualStateProvider NOTE_BLOCK_PROVIDER = new NoteBlockVisualStateProvider();
    private static final BrownMushroomBlockStateProvider BROWN_MUSHROOM_PROVIDER = new BrownMushroomBlockStateProvider();

    private VisualStateProviders() {
    }

    public static VisualStateProvider providerOf(VisualStateProviderType type) {
        return switch (type) {
            case NOTE_BLOCK -> NOTE_BLOCK_PROVIDER;
            case BROWN_MUSHROOM_BLOCK, SOLID -> BROWN_MUSHROOM_PROVIDER;
        };
    }
}
