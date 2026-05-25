package org.kkaemok.forma.block.state;

import org.bukkit.Material;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record VisualBlockState(
        Material material,
        Map<String, String> properties
) {
    public VisualBlockState {
        Objects.requireNonNull(material, "material");
        properties = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNullElse(properties, Map.of())));
    }

    public String asString() {
        String materialKey = material.name().toLowerCase(Locale.ROOT);
        if (properties.isEmpty()) {
            return "minecraft:" + materialKey;
        }

        StringBuilder builder = new StringBuilder("minecraft:")
                .append(materialKey)
                .append('[');

        int index = 0;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue());
            index++;
        }
        builder.append(']');
        return builder.toString();
    }
}
