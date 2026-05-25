package org.kkaemok.forma.block.state;

import org.bukkit.Material;
import org.kkaemok.forma.Forma;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VisualBlockStateManager {
    private static final Pattern ENTRY_PATTERN = Pattern.compile("\"((?:\\\\.|[^\\\\\"])*)\"\\s*:\\s*\"((?:\\\\.|[^\\\\\"])*)\"");
    private static final Pattern STATE_PATTERN = Pattern.compile("^minecraft:([a-z0-9_]+)(?:\\[(.*)])?$");

    private final Forma plugin;
    private final Path cacheFile;
    private final Map<String, String> cachedStates = new LinkedHashMap<>();

    public VisualBlockStateManager(Forma plugin) {
        this.plugin = plugin;
        this.cacheFile = plugin.getDataFolder().toPath().resolve("cache").resolve("visual_block_states.json");
    }

    public void loadCache() {
        cachedStates.clear();
        ensureCacheFile();
        String content;
        try {
            content = Files.readString(cacheFile, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to read visual block state cache: " + ex.getMessage());
            return;
        }

        Matcher matcher = ENTRY_PATTERN.matcher(content);
        while (matcher.find()) {
            String key = unescapeJson(matcher.group(1));
            String value = unescapeJson(matcher.group(2));
            if (!key.isBlank() && !value.isBlank()) {
                cachedStates.put(key, value);
            }
        }
    }

    public void saveCache() {
        ensureCacheFile();
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        int index = 0;
        int size = cachedStates.size();
        for (Map.Entry<String, String> entry : cachedStates.entrySet()) {
            builder.append("  \"")
                    .append(escapeJson(entry.getKey()))
                    .append("\": \"")
                    .append(escapeJson(entry.getValue()))
                    .append("\"");
            if (index < size - 1) {
                builder.append(',');
            }
            builder.append('\n');
            index++;
        }
        builder.append("}\n");

        try {
            Files.writeString(cacheFile, builder.toString(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to write visual block state cache: " + ex.getMessage());
        }
    }

    public VisualBlockState resolveAutoState(
            String blockId,
            VisualStateProvider provider,
            Set<String> currentlyUsedStates
    ) {
        String cached = cachedStates.get(blockId);
        if (cached != null) {
            VisualBlockState parsed = parseStateString(cached);
            if (parsed == null) {
                plugin.getLogger().warning("Invalid cached visual state, re-allocating: " + blockId + " -> " + cached);
            } else if (parsed.material() == provider.getBaseMaterial()) {
                if (!currentlyUsedStates.contains(cached)) {
                    return parsed;
                }
                plugin.getLogger().warning("Cached visual state already used by another block, re-allocating: "
                        + blockId + " -> " + cached);
            } else {
                plugin.getLogger().warning("Cached visual state provider mismatch, re-allocating: "
                        + blockId + " -> " + cached);
            }
        }

        Set<String> reservedStates = new LinkedHashSet<>(cachedStates.values());
        reservedStates.addAll(currentlyUsedStates);

        for (VisualBlockState candidate : provider.getAllStates()) {
            String key = candidate.asString();
            if (!reservedStates.contains(key)) {
                cachedStates.put(blockId, key);
                saveCache();
                return candidate;
            }
        }
        return null;
    }

    public VisualBlockState parseStateString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        Matcher matcher = STATE_PATTERN.matcher(raw.trim());
        if (!matcher.matches()) {
            return null;
        }

        String materialKey = matcher.group(1).toUpperCase();
        Material material = Material.matchMaterial(materialKey);
        if (material == null) {
            return null;
        }

        Map<String, String> properties = new LinkedHashMap<>();
        String props = matcher.group(2);
        if (props != null && !props.isBlank()) {
            String[] parts = props.split(",");
            for (String part : parts) {
                String[] kv = part.split("=", 2);
                if (kv.length != 2 || kv[0].isBlank() || kv[1].isBlank()) {
                    return null;
                }
                properties.put(kv[0].trim(), kv[1].trim());
            }
        }
        return new VisualBlockState(material, properties);
    }

    private void ensureCacheFile() {
        try {
            Files.createDirectories(cacheFile.getParent());
            if (!Files.exists(cacheFile)) {
                Files.writeString(cacheFile, "{\n}\n", StandardCharsets.UTF_8);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to prepare visual block state cache file", ex);
        }
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String unescapeJson(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
