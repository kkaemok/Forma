package org.kkaemok.forma.block;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.kkaemok.forma.Forma;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

public final class FormaBlockStorage {
    private final Forma plugin;
    private final File dataFile;
    private final Map<String, String> worldUuids = new LinkedHashMap<>();
    private final Map<String, Map<String, PlacedFormaBlock>> blocksByWorld = new LinkedHashMap<>();

    public FormaBlockStorage(Forma plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "blocks-data.yml");
    }

    public void load() {
        ensureDataFile();

        worldUuids.clear();
        blocksByWorld.clear();

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection worldsSection = config.getConfigurationSection("worlds");
        if (worldsSection == null) {
            return;
        }

        for (String worldName : worldsSection.getKeys(false)) {
            ConfigurationSection worldSection = worldsSection.getConfigurationSection(worldName);
            if (worldSection == null) {
                continue;
            }

            String uuid = worldSection.getString("uuid", "");
            worldUuids.put(worldName, uuid);

            ConfigurationSection blocksSection = worldSection.getConfigurationSection("blocks");
            if (blocksSection == null) {
                continue;
            }

            Map<String, PlacedFormaBlock> worldEntries = new LinkedHashMap<>();
            for (String coordinate : blocksSection.getKeys(false)) {
                ConfigurationSection entry = blocksSection.getConfigurationSection(coordinate);
                if (entry == null) {
                    continue;
                }

                String id = entry.getString("id");
                FormaBlockType type = FormaBlockType.fromString(entry.getString("type"));
                if (id == null || id.isBlank() || type == null) {
                    plugin.getLogger().warning("[FormaBlockStorage] Invalid block entry: " + worldName + " " + coordinate);
                    continue;
                }

                int[] xyz = parseCoordinate(coordinate);
                if (xyz == null) {
                    plugin.getLogger().warning("[FormaBlockStorage] Invalid coordinate format: " + worldName + " " + coordinate);
                    continue;
                }

                worldEntries.put(coordinate, new PlacedFormaBlock(
                        worldName, uuid, xyz[0], xyz[1], xyz[2], id, type
                ));
            }

            if (!worldEntries.isEmpty()) {
                blocksByWorld.put(worldName, worldEntries);
            }

            if (Bukkit.getWorld(worldName) == null) {
                plugin.getLogger().warning("[FormaBlockStorage] World is not loaded, keeping entries: " + worldName);
            }
        }

        debug("blocks-data.yml loaded worlds=" + blocksByWorld.size());
    }

    public void save() {
        saveInternal();
    }

    private boolean saveInternal() {
        YamlConfiguration output = new YamlConfiguration();
        for (Map.Entry<String, Map<String, PlacedFormaBlock>> worldEntry : blocksByWorld.entrySet()) {
            String worldName = worldEntry.getKey();
            String worldPath = "worlds." + worldName;
            output.set(worldPath + ".uuid", worldUuids.getOrDefault(worldName, ""));

            for (Map.Entry<String, PlacedFormaBlock> blockEntry : worldEntry.getValue().entrySet()) {
                PlacedFormaBlock placed = blockEntry.getValue();
                String blockPath = worldPath + ".blocks." + blockEntry.getKey();
                output.set(blockPath + ".id", placed.id());
                output.set(blockPath + ".type", placed.type().name());
            }
        }

        try {
            output.save(dataFile);
            debug("blocks-data.yml saved.");
            return true;
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save blocks-data.yml", ex);
            return false;
        }
    }

    public boolean trySetBlock(Location location, String id, FormaBlockType type) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        String worldName = world.getName();
        String coordinate = coordinateKey(location);
        String previousUuid = worldUuids.get(worldName);
        Map<String, PlacedFormaBlock> worldBlocks = blocksByWorld.get(worldName);
        boolean previouslyHadWorld = worldBlocks != null;
        if (worldBlocks == null) {
            worldBlocks = new LinkedHashMap<>();
            blocksByWorld.put(worldName, worldBlocks);
        }
        PlacedFormaBlock previousBlock = worldBlocks.get(coordinate);

        worldUuids.put(worldName, world.getUID().toString());
        worldBlocks.put(coordinate, new PlacedFormaBlock(
                worldName,
                world.getUID().toString(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                id,
                type
        ));
        if (saveInternal()) {
            return true;
        }

        if (previousBlock == null) {
            worldBlocks.remove(coordinate);
        } else {
            worldBlocks.put(coordinate, previousBlock);
        }
        if (!previouslyHadWorld && worldBlocks.isEmpty()) {
            blocksByWorld.remove(worldName);
        }
        if (previousUuid == null) {
            worldUuids.remove(worldName);
        } else {
            worldUuids.put(worldName, previousUuid);
        }
        return false;
    }

    public void removeBlock(Location location) {
        tryRemoveBlock(location);
    }

    public boolean tryRemoveBlock(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        String worldName = world.getName();
        Map<String, PlacedFormaBlock> worldBlocks = blocksByWorld.get(worldName);
        if (worldBlocks == null) {
            return false;
        }

        String coordinate = coordinateKey(location);
        PlacedFormaBlock removed = worldBlocks.remove(coordinate);
        if (removed == null) {
            return false;
        }
        String previousUuid = worldUuids.get(worldName);
        boolean removedWorld = false;
        if (worldBlocks.isEmpty()) {
            blocksByWorld.remove(worldName);
            worldUuids.remove(worldName);
            removedWorld = true;
        }
        if (saveInternal()) {
            return true;
        }
        if (removedWorld) {
            worldBlocks = new LinkedHashMap<>();
            blocksByWorld.put(worldName, worldBlocks);
        }
        worldBlocks.put(coordinate, removed);
        if (previousUuid != null) {
            worldUuids.put(worldName, previousUuid);
        }
        return false;
    }

    public String getBlockId(Location location) {
        PlacedFormaBlock placed = getPlacedBlock(location);
        return placed == null ? null : placed.id();
    }

    public PlacedFormaBlock getPlacedBlock(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }

        Map<String, PlacedFormaBlock> worldBlocks = blocksByWorld.get(world.getName());
        if (worldBlocks == null) {
            return null;
        }
        return worldBlocks.get(coordinateKey(location));
    }

    public boolean isCustomBlock(Location location) {
        return getPlacedBlock(location) != null;
    }

    public Map<String, Map<String, PlacedFormaBlock>> getAllBlocks() {
        Map<String, Map<String, PlacedFormaBlock>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, PlacedFormaBlock>> entry : blocksByWorld.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableMap(new LinkedHashMap<>(entry.getValue())));
        }
        return Collections.unmodifiableMap(copy);
    }

    public void validateLoadedBlocks() {
        boolean removeMissing = plugin.getConfig().getBoolean("custom-blocks.remove-missing-block-data", true);
        boolean changed = false;

        for (String worldName : new LinkedHashMap<>(blocksByWorld).keySet()) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("[FormaBlockStorage] World is not loaded, skip validation: " + worldName);
                continue;
            }

            Map<String, PlacedFormaBlock> worldBlocks = blocksByWorld.get(worldName);
            if (worldBlocks == null) {
                continue;
            }

            for (String coordinate : new LinkedHashMap<>(worldBlocks).keySet()) {
                PlacedFormaBlock placed = worldBlocks.get(coordinate);
                if (placed == null) {
                    continue;
                }

                FormaBlock blockDef = plugin.getBlockManager().getBlock(placed.id());
                Material expected = blockDef == null ? Material.NOTE_BLOCK : blockDef.placedMaterial();
                Material current = world.getBlockAt(placed.x(), placed.y(), placed.z()).getType();
                if (current == expected) {
                    continue;
                }

                plugin.getLogger().warning("[FormaBlockStorage] Stored block mismatch: "
                        + worldName + " " + coordinate
                        + " expected=" + expected
                        + ", actual=" + current
                        + ", id=" + placed.id());
                if (removeMissing) {
                    worldBlocks.remove(coordinate);
                    changed = true;
                }
            }

            if (worldBlocks.isEmpty()) {
                blocksByWorld.remove(worldName);
                worldUuids.remove(worldName);
                changed = true;
            }
        }

        if (changed) {
            save();
        }
    }

    private void ensureDataFile() {
        if (dataFile.exists()) {
            return;
        }
        try {
            if (!dataFile.createNewFile()) {
                plugin.getLogger().warning("Could not create blocks-data.yml: " + dataFile.getAbsolutePath());
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to create blocks-data.yml: " + ex.getMessage());
        }
    }

    private String coordinateKey(Location location) {
        return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private int[] parseCoordinate(String coordinate) {
        String[] split = coordinate.split(",", 3);
        if (split.length != 3) {
            return null;
        }
        try {
            return new int[]{
                    Integer.parseInt(split[0].trim()),
                    Integer.parseInt(split[1].trim()),
                    Integer.parseInt(split[2].trim())
            };
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void debug(String message) {
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
}
