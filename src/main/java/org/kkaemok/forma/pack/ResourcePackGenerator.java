package org.kkaemok.forma.pack;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.kkaemok.forma.Forma;
import org.kkaemok.forma.block.FormaBlock;
import org.kkaemok.forma.block.state.VisualStateProviderType;
import org.kkaemok.forma.item.FormaItem;
import org.kkaemok.forma.item.FormaItemType;
import org.kkaemok.forma.util.NoteBlockInstrumentUtil;
import org.kkaemok.forma.util.NoteBlockVariationUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ResourcePackGenerator {
    private static final String VANILLA_NOTE_BLOCK_MODEL = "minecraft:block/note_block";
    private static final String VANILLA_BROWN_MUSHROOM_BLOCK_MODEL = "minecraft:block/brown_mushroom_block";
    private static final List<String> BROWN_STATE_ORDER = List.of(
            "down",
            "east",
            "north",
            "south",
            "up",
            "west"
    );

    private final Forma plugin;
    private final Path modelsItemDir;
    private final Path texturesItemDir;
    private final Path modelsBlockDir;
    private final Path texturesBlockDir;

    public ResourcePackGenerator(
            Forma plugin,
            Path modelsItemDir,
            Path texturesItemDir,
            Path modelsBlockDir,
            Path texturesBlockDir
    ) {
        this.plugin = plugin;
        this.modelsItemDir = modelsItemDir;
        this.texturesItemDir = texturesItemDir;
        this.modelsBlockDir = modelsBlockDir;
        this.texturesBlockDir = texturesBlockDir;
    }

    public GenerationResult generate(
            Path outputZip,
            Map<String, FormaItem> items,
            Map<String, FormaBlock> blocks,
            PackSettings settings,
            boolean generateBlocks
    ) throws IOException {
        Objects.requireNonNull(outputZip, "outputZip");
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(blocks, "blocks");
        Objects.requireNonNull(settings, "settings");

        Path parent = outputZip.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.deleteIfExists(outputZip);

        LinkedHashSet<String> missingTextures = new LinkedHashSet<>();
        LinkedHashSet<String> defaultModels = new LinkedHashSet<>();
        Set<String> writtenEntries = new LinkedHashSet<>();

        debug("resource-pack generate start: pack=" + settings.packName()
                + ", items=" + items.size()
                + ", blocks=" + blocks.size()
                + ", generateBlocks=" + generateBlocks);

        try (ZipOutputStream zip = new ZipOutputStream(
                Files.newOutputStream(outputZip, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
        )) {
            writeTextEntry(zip, writtenEntries, "pack.mcmeta", buildPackMcmeta(settings));
            writeItemAssets(zip, writtenEntries, items.values(), settings, missingTextures, defaultModels);
            if (generateBlocks) {
                writeBlockAssets(zip, writtenEntries, blocks.values(), settings, missingTextures, defaultModels);
            } else {
                debug("resource-pack.generate-blocks=false, block assets skipped.");
            }
        }

        if (!missingTextures.isEmpty()) {
            plugin.getLogger().warning("[ResourcePack] Missing textures: " + String.join(", ", missingTextures));
        }
        if (!defaultModels.isEmpty()) {
            plugin.getLogger().info("[ResourcePack] Generated default models: " + String.join(", ", defaultModels));
        }

        return new GenerationResult(
                outputZip.toFile(),
                new ArrayList<>(missingTextures),
                new ArrayList<>(defaultModels)
        );
    }

    private void writeItemAssets(
            ZipOutputStream zip,
            Set<String> writtenEntries,
            Collection<FormaItem> items,
            PackSettings settings,
            Set<String> missingTextures,
            Set<String> defaultModels
    ) throws IOException {
        for (FormaItem item : items) {
            NamespacedKey modelKey = parseModelKey(item.model(), settings.namespace());
            if (modelKey == null) {
                plugin.getLogger().warning("[ResourcePack] Invalid item model key: "
                        + item.id() + " -> " + item.model());
                continue;
            }

            String namespace = modelKey.getNamespace();
            String modelId = modelKey.getKey();
            if (!namespace.equals(settings.namespace())) {
                plugin.getLogger().warning("[ResourcePack] Namespace mismatch: id=" + item.id()
                        + ", model=" + namespace + ", config=" + settings.namespace());
            }

            writeTextEntry(
                    zip,
                    writtenEntries,
                    "assets/" + namespace + "/items/" + modelId + ".json",
                    buildItemDefinitionJson(namespace, modelId)
            );

            Path sourceModel = modelsItemDir.resolve(modelId + ".json");
            String modelTarget = "assets/" + namespace + "/models/item/" + modelId + ".json";
            if (Files.isRegularFile(sourceModel)) {
                writeFileEntry(zip, writtenEntries, modelTarget, sourceModel);
            } else {
                defaultModels.add(item.id());
                writeTextEntry(zip, writtenEntries, modelTarget, buildDefaultItemModelJson(item.type(), namespace, modelId));
            }

            Path sourceTexture = texturesItemDir.resolve(modelId + ".png");
            String textureTarget = "assets/" + namespace + "/textures/item/" + modelId + ".png";
            if (Files.isRegularFile(sourceTexture)) {
                writeFileEntry(zip, writtenEntries, textureTarget, sourceTexture);
            } else {
                missingTextures.add(item.id());
                plugin.getLogger().warning("[ResourcePack] Missing item texture: " + item.id()
                        + " -> " + sourceTexture);
            }
        }
    }

    private void writeBlockAssets(
            ZipOutputStream zip,
            Set<String> writtenEntries,
            Collection<FormaBlock> blocks,
            PackSettings settings,
            Set<String> missingTextures,
            Set<String> defaultModels
    ) throws IOException {
        Map<String, String> noteBlockVariants = new LinkedHashMap<>();
        Map<String, String> brownMushroomVariants = new LinkedHashMap<>();

        for (FormaBlock block : blocks) {
            NamespacedKey itemModelKey = parseModelKey(block.itemModel(), settings.namespace());
            if (itemModelKey == null) {
                plugin.getLogger().warning("[ResourcePack] Invalid block item model key: "
                        + block.id() + " -> " + block.itemModel());
                continue;
            }
            NamespacedKey blockModelKey = parseModelKey(block.blockModel(), settings.namespace());
            if (blockModelKey == null) {
                plugin.getLogger().warning("[ResourcePack] Invalid block model key: "
                        + block.id() + " -> " + block.blockModel());
                continue;
            }

            String itemNamespace = itemModelKey.getNamespace();
            String itemModelId = itemModelKey.getKey();
            String blockNamespace = blockModelKey.getNamespace();
            String blockModelPath = blockModelKey.getKey();
            String blockModelRef = blockNamespace + ":" + blockModelPath;

            if (!itemNamespace.equals(settings.namespace())) {
                plugin.getLogger().warning("[ResourcePack] Block item namespace mismatch: id=" + block.id()
                        + ", model=" + itemNamespace + ", config=" + settings.namespace());
            }
            if (!blockNamespace.equals(settings.namespace())) {
                plugin.getLogger().warning("[ResourcePack] Block namespace mismatch: id=" + block.id()
                        + ", model=" + blockNamespace + ", config=" + settings.namespace());
            }

            writeTextEntry(
                    zip,
                    writtenEntries,
                    "assets/" + itemNamespace + "/items/" + itemModelId + ".json",
                    buildItemDefinitionJson(itemNamespace, itemModelId)
            );

            Path sourceItemModel = modelsItemDir.resolve(itemModelId + ".json");
            String itemModelTarget = "assets/" + itemNamespace + "/models/item/" + itemModelId + ".json";
            if (Files.isRegularFile(sourceItemModel)) {
                writeFileEntry(zip, writtenEntries, itemModelTarget, sourceItemModel);
            } else {
                defaultModels.add("block-item:" + block.id());
                writeTextEntry(
                        zip,
                        writtenEntries,
                        itemModelTarget,
                        buildBlockItemModelJson(blockModelRef)
                );
            }

            Path itemTextureSource = texturesItemDir.resolve(itemModelId + ".png");
            String itemTextureTarget = "assets/" + itemNamespace + "/textures/item/" + itemModelId + ".png";
            if (Files.isRegularFile(itemTextureSource)) {
                writeFileEntry(zip, writtenEntries, itemTextureTarget, itemTextureSource);
            } else {
                Path fallback = resolveBlockTextureSource(block.id());
                if (fallback != null) {
                    writeFileEntry(zip, writtenEntries, itemTextureTarget, fallback);
                } else {
                    missingTextures.add("block-item:" + block.id());
                    plugin.getLogger().warning("[ResourcePack] Missing block item texture: " + block.id());
                }
            }

            String blockModelTarget = "assets/" + blockNamespace + "/models/" + blockModelPath + ".json";
            Path sourceBlockModel = modelsBlockDir.resolve(block.id() + ".json");
            if (Files.isRegularFile(sourceBlockModel)) {
                writeFileEntry(zip, writtenEntries, blockModelTarget, sourceBlockModel);
            } else {
                defaultModels.add("block:" + block.id());
                writeTextEntry(
                        zip,
                        writtenEntries,
                        blockModelTarget,
                        buildDefaultBlockModelJson(blockNamespace, block.id())
                );
            }

            Path blockTextureSource = resolveBlockTextureSource(block.id());
            if (blockTextureSource != null) {
                writeFileEntry(
                        zip,
                        writtenEntries,
                        "assets/" + blockNamespace + "/textures/block/" + block.id() + ".png",
                        blockTextureSource
                );
            } else {
                missingTextures.add("block:" + block.id());
                plugin.getLogger().warning("[ResourcePack] Missing block texture: " + block.id());
            }

            registerProviderVariant(block, blockModelRef, noteBlockVariants, brownMushroomVariants);
        }

        writeProviderBlockstateFiles(zip, writtenEntries, noteBlockVariants, brownMushroomVariants);
    }

    private void registerProviderVariant(
            FormaBlock block,
            String blockModelRef,
            Map<String, String> noteBlockVariants,
            Map<String, String> brownMushroomVariants
    ) {
        VisualStateProviderType providerType = block.providerType();
        if (providerType == VisualStateProviderType.NOTE_BLOCK) {
            String key = block.noteBlockStateKey();
            if (key == null || key.isBlank()) {
                plugin.getLogger().warning("[ResourcePack] NOTE_BLOCK provider key is missing: " + block.id());
                return;
            }
            if (!isValidStateKey(Material.NOTE_BLOCK, key)) {
                plugin.getLogger().warning("[ResourcePack] Invalid note_block state key skipped: "
                        + block.id() + " -> " + key);
                return;
            }
            noteBlockVariants.put(key, blockModelRef);
            return;
        }

        if (providerType == VisualStateProviderType.BROWN_MUSHROOM_BLOCK
                || providerType == VisualStateProviderType.SOLID) {
            String key = buildBrownMushroomVariantKey(block);
            if (key == null) {
                plugin.getLogger().warning("[ResourcePack] Invalid brown_mushroom_block state skipped: " + block.id()
                        + " -> " + block.visualState().asString());
                return;
            }
            if (!isValidStateKey(Material.BROWN_MUSHROOM_BLOCK, key)) {
                plugin.getLogger().warning("[ResourcePack] Invalid brown_mushroom_block variant key skipped: "
                        + block.id() + " -> " + key);
                return;
            }
            brownMushroomVariants.put(key, blockModelRef);
        }
    }

    private void writeProviderBlockstateFiles(
            ZipOutputStream zip,
            Set<String> writtenEntries,
            Map<String, String> noteBlockVariants,
            Map<String, String> brownMushroomVariants
    ) throws IOException {
        if (!noteBlockVariants.isEmpty()) {
            writeTextEntry(
                    zip,
                    writtenEntries,
                    "assets/minecraft/blockstates/note_block.json",
                    buildNoteBlockStateJson(noteBlockVariants)
            );
            debug("Generated note_block.json variants=" + noteBlockVariants.size());
        }

        if (!brownMushroomVariants.isEmpty()) {
            writeTextEntry(
                    zip,
                    writtenEntries,
                    "assets/minecraft/blockstates/brown_mushroom_block.json",
                    buildBrownMushroomBlockStateJson(brownMushroomVariants)
            );
            debug("Generated brown_mushroom_block.json variants=" + brownMushroomVariants.size());
        }
    }

    private String buildBrownMushroomVariantKey(FormaBlock block) {
        if (block.visualState().material() != Material.BROWN_MUSHROOM_BLOCK) {
            return null;
        }

        StringBuilder key = new StringBuilder();
        for (int index = 0; index < BROWN_STATE_ORDER.size(); index++) {
            String property = BROWN_STATE_ORDER.get(index);
            String value = block.visualState().properties().get(property);
            if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                return null;
            }
            if (index > 0) {
                key.append(',');
            }
            key.append(property).append('=').append(value.toLowerCase(Locale.ROOT));
        }
        return key.toString();
    }

    private boolean isValidStateKey(Material material, String stateKey) {
        String full = "minecraft:" + material.name().toLowerCase(Locale.ROOT) + "[" + stateKey + "]";
        try {
            Bukkit.createBlockData(full);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private Path resolveBlockTextureSource(String blockId) {
        Path primary = texturesBlockDir.resolve(blockId + ".png");
        if (Files.isRegularFile(primary)) {
            return primary;
        }
        Path fallback = texturesItemDir.resolve(blockId + ".png");
        if (Files.isRegularFile(fallback)) {
            return fallback;
        }
        return null;
    }

    private NamespacedKey parseModelKey(String rawModel, String defaultNamespace) {
        if (rawModel == null || rawModel.isBlank()) {
            return null;
        }

        String input = rawModel.trim();
        if (!input.contains(":")) {
            input = defaultNamespace + ":" + input;
        }

        String[] split = input.split(":", 2);
        if (split.length != 2 || split[0].isBlank() || split[1].isBlank()) {
            return null;
        }

        try {
            return new NamespacedKey(
                    split[0].trim().toLowerCase(Locale.ROOT),
                    split[1].trim().toLowerCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String buildPackMcmeta(PackSettings settings) {
        return "{\n"
                + "  \"pack\": {\n"
                + "    \"min_format\": [" + settings.minMajor() + ", " + settings.minMinor() + "],\n"
                + "    \"max_format\": [" + settings.maxMajor() + ", " + settings.maxMinor() + "],\n"
                + "    \"description\": {\n"
                + "      \"text\": " + quoteJson(settings.packDescription()) + "\n"
                + "    }\n"
                + "  }\n"
                + "}\n";
    }

    private String buildItemDefinitionJson(String namespace, String modelId) {
        return "{\n"
                + "  \"model\": {\n"
                + "    \"type\": \"minecraft:model\",\n"
                + "    \"model\": " + quoteJson(namespace + ":item/" + modelId) + "\n"
                + "  }\n"
                + "}\n";
    }

    private String buildDefaultItemModelJson(FormaItemType type, String namespace, String modelId) {
        String parent = type.usesHandheldModel() ? "minecraft:item/handheld" : "minecraft:item/generated";
        return "{\n"
                + "  \"parent\": " + quoteJson(parent) + ",\n"
                + "  \"textures\": {\n"
                + "    \"layer0\": " + quoteJson(namespace + ":item/" + modelId) + "\n"
                + "  }\n"
                + "}\n";
    }

    private String buildBlockItemModelJson(String parentModel) {
        return "{\n"
                + "  \"parent\": " + quoteJson(parentModel) + "\n"
                + "}\n";
    }

    private String buildDefaultBlockModelJson(String namespace, String blockId) {
        return "{\n"
                + "  \"parent\": \"minecraft:block/cube_all\",\n"
                + "  \"textures\": {\n"
                + "    \"all\": " + quoteJson(namespace + ":block/" + blockId) + "\n"
                + "  }\n"
                + "}\n";
    }

    private String buildNoteBlockStateJson(Map<String, String> customVariants) {
        Map<String, String> variants = new LinkedHashMap<>();
        for (String instrument : supportedInstrumentStates()) {
            for (int note = 0; note <= 24; note++) {
                variants.put("instrument=" + instrument + ",note=" + note + ",powered=false", VANILLA_NOTE_BLOCK_MODEL);
                variants.put("instrument=" + instrument + ",note=" + note + ",powered=true", VANILLA_NOTE_BLOCK_MODEL);
            }
        }
        variants.putAll(customVariants);
        return buildVariantsJson(variants);
    }

    private String buildBrownMushroomBlockStateJson(Map<String, String> customVariants) {
        Map<String, String> variants = new LinkedHashMap<>();
        int max = 1 << BROWN_STATE_ORDER.size();
        for (int variation = 1; variation <= max; variation++) {
            int value = variation - 1;
            StringBuilder key = new StringBuilder();
            for (int index = 0; index < BROWN_STATE_ORDER.size(); index++) {
                boolean enabled = (value & (1 << index)) != 0;
                if (index > 0) {
                    key.append(',');
                }
                key.append(BROWN_STATE_ORDER.get(index)).append('=').append(enabled);
            }
            variants.put(key.toString(), VANILLA_BROWN_MUSHROOM_BLOCK_MODEL);
        }
        variants.putAll(customVariants);
        return buildVariantsJson(variants);
    }

    private String buildVariantsJson(Map<String, String> variants) {
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"variants\": {\n");

        int index = 0;
        int size = variants.size();
        for (Map.Entry<String, String> entry : variants.entrySet()) {
            json.append("    ")
                    .append(quoteJson(entry.getKey()))
                    .append(": { \"model\": ")
                    .append(quoteJson(entry.getValue()))
                    .append(" }");
            if (index < size - 1) {
                json.append(',');
            }
            json.append('\n');
            index++;
        }

        json.append("  }\n}\n");
        return json.toString();
    }

    private Collection<String> supportedInstrumentStates() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        NoteBlockVariationUtil.orderedInstruments().forEach(instrument ->
                names.add(NoteBlockInstrumentUtil.toBlockStateName(instrument)));
        return names;
    }

    private void writeTextEntry(
            ZipOutputStream zip,
            Set<String> writtenEntries,
            String entryPath,
            String content
    ) throws IOException {
        String normalized = normalizeEntry(entryPath);
        if (!writtenEntries.add(normalized)) {
            plugin.getLogger().warning("[ResourcePack] Duplicate ZIP entry ignored: " + normalized);
            return;
        }

        zip.putNextEntry(new ZipEntry(normalized));
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        zip.write(bytes);
        zip.closeEntry();
    }

    private void writeFileEntry(
            ZipOutputStream zip,
            Set<String> writtenEntries,
            String entryPath,
            Path source
    ) throws IOException {
        String normalized = normalizeEntry(entryPath);
        if (!writtenEntries.add(normalized)) {
            plugin.getLogger().warning("[ResourcePack] Duplicate ZIP entry ignored: " + normalized);
            return;
        }

        zip.putNextEntry(new ZipEntry(normalized));
        try (InputStream in = Files.newInputStream(source, StandardOpenOption.READ)) {
            in.transferTo(zip);
        }
        zip.closeEntry();
    }

    private String normalizeEntry(String path) {
        return path.replace('\\', '/');
    }

    private String quoteJson(String value) {
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
        return "\"" + escaped + "\"";
    }

    private void debug(String message) {
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }

    public record PackSettings(
            String namespace,
            String packName,
            String packDescription,
            int minMajor,
            int minMinor,
            int maxMajor,
            int maxMinor
    ) {
    }

    public record GenerationResult(
            File outputFile,
            List<String> missingTextureItems,
            List<String> defaultModelItems
    ) {
    }
}
