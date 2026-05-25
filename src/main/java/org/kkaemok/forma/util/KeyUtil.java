package org.kkaemok.forma.util;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.kkaemok.forma.Forma;

import java.util.Locale;
import java.util.Objects;

public final class KeyUtil {
    private KeyUtil() {
    }

    public static NamespacedKey itemId(Forma plugin) {
        return key(plugin, "item_id");
    }

    public static NamespacedKey itemType(Forma plugin) {
        return key(plugin, "item_type");
    }

    public static NamespacedKey durabilityMax(Forma plugin) {
        return key(plugin, "durability_max");
    }

    public static NamespacedKey durabilityCurrent(Forma plugin) {
        return key(plugin, "durability_current");
    }

    public static NamespacedKey blockId(Forma plugin) {
        return key(plugin, "block_id");
    }

    public static NamespacedKey blockType(Forma plugin) {
        return key(plugin, "block_type");
    }

    public static String readBlockId(Forma plugin, PersistentDataContainer container) {
        return readString(plugin, container, "block_id");
    }

    public static String readItemId(Forma plugin, PersistentDataContainer container) {
        return readString(plugin, container, "item_id");
    }

    private static NamespacedKey key(Forma plugin, String value) {
        String namespace = Objects.requireNonNullElse(plugin.getConfig().getString("namespace"), "forma");
        if (namespace.isBlank()) {
            namespace = "forma";
        }

        try {
            return new NamespacedKey(
                    namespace.toLowerCase(Locale.ROOT),
                    value.toLowerCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("namespace 값이 잘못되어 플러그인 기본 네임스페이스로 대체합니다: " + namespace);
            return new NamespacedKey(plugin, value.toLowerCase(Locale.ROOT));
        }
    }

    private static NamespacedKey legacyKey(String value) {
        return new NamespacedKey("leaftem", value.toLowerCase(Locale.ROOT));
    }

    private static String readString(Forma plugin, PersistentDataContainer container, String value) {
        String current = container.get(key(plugin, value), PersistentDataType.STRING);
        if (current != null) {
            return current;
        }
        return container.get(legacyKey(value), PersistentDataType.STRING);
    }
}
