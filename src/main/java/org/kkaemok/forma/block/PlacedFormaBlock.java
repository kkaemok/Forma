package org.kkaemok.forma.block;

public record PlacedFormaBlock(
        String worldName,
        String worldUuid,
        int x,
        int y,
        int z,
        String id,
        FormaBlockType type
) {
    public String coordinateKey() {
        return x + "," + y + "," + z;
    }
}
