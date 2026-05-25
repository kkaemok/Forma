package org.kkaemok.forma.item;

public record FormaItemAttributes(
        Double attackDamage,
        Double attackSpeed,
        Double armor,
        Double armorToughness,
        Double knockbackResistance,
        Double movementSpeed
) {
    public boolean isEmpty() {
        return attackDamage == null
                && attackSpeed == null
                && armor == null
                && armorToughness == null
                && knockbackResistance == null
                && movementSpeed == null;
    }
}
