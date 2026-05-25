package org.kkaemok.forma.pack;

import org.kkaemok.forma.Forma;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class ResourcePackManager {
    private final Forma plugin;
    private final Path resourcePackRoot;
    private final Path modelsItemDir;
    private final Path texturesItemDir;
    private final Path modelsBlockDir;
    private final Path texturesBlockDir;
    private final Path generatedDir;

    public ResourcePackManager(Forma plugin) {
        this.plugin = plugin;
        this.resourcePackRoot = plugin.getDataFolder().toPath().resolve("resourcepack");
        this.modelsItemDir = resourcePackRoot.resolve("models").resolve("item");
        this.texturesItemDir = resourcePackRoot.resolve("textures").resolve("item");
        this.modelsBlockDir = resourcePackRoot.resolve("models").resolve("block");
        this.texturesBlockDir = resourcePackRoot.resolve("textures").resolve("block");
        this.generatedDir = plugin.getDataFolder().toPath().resolve("generated");
    }

    public void ensureDirectories() {
        try {
            Files.createDirectories(resourcePackRoot);
            Files.createDirectories(modelsItemDir);
            Files.createDirectories(texturesItemDir);
            Files.createDirectories(modelsBlockDir);
            Files.createDirectories(texturesBlockDir);
            Files.createDirectories(generatedDir);
        } catch (IOException ex) {
            throw new IllegalStateException("리소스팩 폴더를 생성하지 못했습니다.", ex);
        }
    }

    public ResourcePackGenerator.GenerationResult generatePack() throws IOException {
        ensureDirectories();
        ResourcePackGenerator.PackSettings settings = readSettings();
        ResourcePackGenerator generator = new ResourcePackGenerator(
                plugin,
                modelsItemDir,
                texturesItemDir,
                modelsBlockDir,
                texturesBlockDir
        );
        boolean generateBlocks = plugin.getConfig().getBoolean("resource-pack.generate-blocks", true);
        return generator.generate(
                getOutputFile().toPath(),
                plugin.getItemManager().getItems(),
                plugin.getBlockManager().getBlocks(),
                settings,
                generateBlocks
        );
    }

    public File getOutputFile() {
        String fileName = readTrimmedOrDefault("resource-pack.output-file", "Forma-Pack.zip");
        return generatedDir.resolve(fileName).toFile();
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("resource-pack.enabled", true);
    }

    private ResourcePackGenerator.PackSettings readSettings() {
        String namespace = readTrimmedOrDefault("namespace", "forma").toLowerCase(Locale.ROOT);
        String packName = readTrimmedOrDefault("resource-pack.pack-name", "Forma-Pack");
        String description = readTrimmedOrDefault("resource-pack.pack-description", "Forma Resource Pack");
        int[] minVersion = readPackVersion("resource-pack.pack-version.min", 75, 0);
        int[] maxVersion = readPackVersion("resource-pack.pack-version.max", 75, 0);

        return new ResourcePackGenerator.PackSettings(
                namespace,
                packName,
                description,
                minVersion[0],
                minVersion[1],
                maxVersion[0],
                maxVersion[1]
        );
    }

    private String readTrimmedOrDefault(String path, String defaultValue) {
        String value = plugin.getConfig().getString(path);
        if (value == null) {
            return defaultValue;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    private int[] readPackVersion(String path, int defaultMajor, int defaultMinor) {
        List<Integer> values = plugin.getConfig().getIntegerList(path);
        int major = !values.isEmpty() ? values.get(0) : defaultMajor;
        int minor = values.size() > 1 ? values.get(1) : defaultMinor;
        return new int[]{major, minor};
    }
}
