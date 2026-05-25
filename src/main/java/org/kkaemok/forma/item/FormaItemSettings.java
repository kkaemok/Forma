package org.kkaemok.forma.item;

public record FormaItemSettings(
        boolean glow,
        boolean unbreakable,
        boolean hideTooltip,
        boolean hideAttributes,
        Integer maxStackSize,
        Integer durabilityMax,
        Integer durabilityCurrent
) {
}
