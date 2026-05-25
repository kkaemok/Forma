package org.kkaemok.forma.item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FormaItemCooldownManager {
    private final Map<CooldownKey, Long> cooldownEndTimes = new HashMap<>();

    public long remainingMillis(UUID playerId, String itemId, int behaviorIndex) {
        CooldownKey key = new CooldownKey(playerId, itemId, behaviorIndex);
        Long end = cooldownEndTimes.get(key);
        if (end == null) {
            return 0L;
        }
        long remaining = end - System.currentTimeMillis();
        if (remaining <= 0L) {
            cooldownEndTimes.remove(key);
            return 0L;
        }
        return remaining;
    }

    public void start(UUID playerId, String itemId, int behaviorIndex, long durationMillis) {
        if (durationMillis <= 0L) {
            return;
        }
        cooldownEndTimes.put(
                new CooldownKey(playerId, itemId, behaviorIndex),
                System.currentTimeMillis() + durationMillis
        );
    }

    private record CooldownKey(UUID playerId, String itemId, int behaviorIndex) {
    }
}
