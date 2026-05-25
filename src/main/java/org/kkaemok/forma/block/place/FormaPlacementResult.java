package org.kkaemok.forma.block.place;

public enum FormaPlacementResult {
    SUCCESS,
    PASS,
    FAIL,
    CANCELLED,
    TOO_HIGH,
    COLLISION,
    CANNOT_REPLACE,
    UNKNOWN_BLOCK,
    INVALID_BLOCKDATA,
    BLOCK_CAN_BUILD_DENIED,
    ATTEMPT_EVENT_CANCELLED,
    BLOCK_PLACE_EVENT_CANCELLED,
    FORMA_PLACE_EVENT_CANCELLED,
    STORAGE_SAVE_FAILED;

    public boolean successful() {
        return this == SUCCESS;
    }
}
