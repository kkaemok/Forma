package org.kkaemok.forma.block;

import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.kkaemok.forma.Forma;
import org.kkaemok.forma.block.state.NoteBlockVisualStateProvider;
import org.kkaemok.forma.block.state.VisualBlockState;
import org.kkaemok.forma.block.state.VisualStateProvider;
import org.kkaemok.forma.block.state.VisualStateProviderType;
import org.kkaemok.forma.block.state.VisualStateProviders;
import org.kkaemok.forma.recipe.FormaRecipeDefinition;
import org.kkaemok.forma.recipe.FormaRecipeParser;
import org.kkaemok.forma.util.NoteBlockInstrumentUtil;
import org.kkaemok.forma.util.NoteBlockState;
import org.kkaemok.forma.util.NoteBlockVariationUtil;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class FormaBlockManager {
    private final Forma plugin;
    private final File blocksFile;
    private final Map<String, FormaBlock> blocks = new LinkedHashMap<>();
    private final Map<String, String> reservedNoteStates = new LinkedHashMap<>();

    public FormaBlockManager(Forma plugin) {
        this.plugin = plugin;
        this.blocksFile = new File(plugin.getDataFolder(), "blocks.yml");
    }

    public void loadBlocks() {
        if (!blocksFile.exists()) {
            plugin.saveResource("blocks.yml", false);
        }

        blocks.clear();
        reservedNoteStates.clear();
        Set<String> usedVisualStates = new LinkedHashSet<>();
        NoteBlockVisualStateProvider noteBlockProvider = new NoteBlockVisualStateProvider();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(blocksFile);

        for (String id : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(id);
            if (section == null) {
                warn(id, "Section is missing.");
                continue;
            }

            ConfigurationSection itemSection = section.getConfigurationSection("item");
            if (itemSection == null) {
                warn(id, "item section is missing.");
                continue;
            }

            Material itemMaterial = parseMaterial(itemSection.getString("material"));
            if (itemMaterial == null) {
                warn(id, "Invalid item.material: " + itemSection.getString("material"));
                continue;
            }

            ConfigurationSection stateSection = section.getConfigurationSection("state");
            ConfigurationSection legacyBlockSection = section.getConfigurationSection("block");
            ConfigurationSection settingsSection = section.getConfigurationSection("settings");

            FormaBlockType type = resolveLegacyType(section.getString("type"));
            VisualStateProviderType providerType = resolveProviderType(stateSection, type);
            if (providerType == null) {
                warn(id, "Invalid state.provider: " + stateSection.getString("provider"));
                continue;
            }

            VisualStateProvider provider = VisualStateProviders.providerOf(providerType);
            String variationRaw = readVariationRaw(stateSection, legacyBlockSection);
            Integer variation = null;
            NoteBlockState legacyNoteState = null;
            VisualBlockState visualState;

            if (stateSection != null && stateSection.isSet("provider")) {
                if (variationRaw == null) {
                    warn(id, "state.variation is required when state.provider is used.");
                    continue;
                }
                if ("auto".equalsIgnoreCase(variationRaw)) {
                    visualState = plugin.getVisualBlockStateManager().resolveAutoState(id, provider, usedVisualStates);
                    if (visualState == null) {
                        warn(id, "No free visual state available for provider " + providerType + ".");
                        continue;
                    }
                } else {
                    variation = parsePositiveInt(variationRaw);
                    if (variation == null) {
                        warn(id, "Invalid state.variation value: " + variationRaw);
                        continue;
                    }
                    if (variation < 1 || variation > provider.maxVariations()) {
                        warn(id, "state.variation out of range: " + variation
                                + " (max=" + provider.maxVariations() + ")");
                        continue;
                    }
                    visualState = provider.fromVariation(variation);
                    if (visualState == null) {
                        warn(id, "Failed to build visual state for variation: " + variation);
                        continue;
                    }
                }

                if (providerType.isNoteBlockProvider()) {
                    legacyNoteState = toNoteBlockState(visualState);
                    if (legacyNoteState == null) {
                        warn(id, "Failed to map NOTE_BLOCK visual state: " + visualState.asString());
                        continue;
                    }
                }
            } else {
                if (providerType != VisualStateProviderType.NOTE_BLOCK) {
                    warn(id, "Legacy config without state.provider only supports NOTE_BLOCK.");
                    continue;
                }

                legacyNoteState = resolveLegacyNoteBlockState(id, legacyBlockSection);
                if (legacyNoteState == null) {
                    continue;
                }
                if (legacyBlockSection != null && legacyBlockSection.isInt("variation")) {
                    variation = legacyBlockSection.getInt("variation");
                }
                variationRaw = variation == null ? null : Integer.toString(variation);
                visualState = noteBlockProvider.fromNoteBlockState(legacyNoteState);
            }

            String visualStateString = visualState.asString();
            if (!usedVisualStates.add(visualStateString)) {
                warn(id, "Visual state collision: " + visualStateString);
                continue;
            }

            String blockModel = firstNonBlank(
                    stateSection == null ? null : stateSection.getString("model"),
                    legacyBlockSection == null ? null : legacyBlockSection.getString("model")
            );
            if (blockModel == null) {
                warn(id, "Missing state.model or block.model.");
                continue;
            }

            double hardness = readDouble(settingsSection, "hardness",
                    legacyBlockSection == null ? null : legacyBlockSection.getDouble("hardness", 1.0D), 1.0D);
            String tool = readString(settingsSection, "tool",
                    legacyBlockSection == null ? null : legacyBlockSection.getString("tool"), "HAND");
            FormaBlockSounds sounds = readSounds(settingsSection);

            Instrument instrument = legacyNoteState == null ? null : legacyNoteState.instrument();
            int note = legacyNoteState == null ? 0 : legacyNoteState.note();
            boolean powered = legacyNoteState != null && legacyNoteState.powered();
            FormaRecipeDefinition recipe = FormaRecipeParser.parse(
                    section.getConfigurationSection("recipe"),
                    message -> warnRecipe(id, message)
            );

            FormaBlock block = new FormaBlock(
                    id,
                    type,
                    section.getString("name"),
                    itemMaterial,
                    itemSection.getString("model"),
                    itemSection.isInt("custom-model-data") ? itemSection.getInt("custom-model-data") : null,
                    itemSection.getStringList("lore"),
                    blockModel,
                    hardness,
                    tool,
                    sounds,
                    providerType,
                    visualState,
                    instrument,
                    note,
                    powered,
                    variation,
                    variationRaw,
                    section.getStringList("drops"),
                    recipe
            );

            if (block.usesNoteBlockProvider()) {
                String stateKey = block.noteBlockStateKey();
                if (stateKey != null) {
                    String previousOwner = reservedNoteStates.putIfAbsent(stateKey, id);
                    if (previousOwner != null) {
                        warn(id, "NOTE_BLOCK state collision: " + stateKey + " (already used by " + previousOwner + ")");
                        continue;
                    }
                }
            }

            blocks.put(id, block);
            debug("Block loaded: id=" + id
                    + ", provider=" + block.providerType()
                    + ", variation=" + (block.variationRaw() == null ? "(none)" : block.variationRaw())
                    + ", visual=" + block.visualState().asString());
        }

        plugin.getLogger().info("Loaded " + blocks.size()
                + " custom blocks. NoteBlock variations available: " + NoteBlockVariationUtil.maxVariations());
    }

    public void reload() {
        loadBlocks();
    }

    public FormaBlock getBlock(String id) {
        return blocks.get(id);
    }

    public Map<String, FormaBlock> getBlocks() {
        return Collections.unmodifiableMap(blocks);
    }

    public Set<String> getIds() {
        return Collections.unmodifiableSet(blocks.keySet());
    }

    public boolean isReservedNoteBlockState(Instrument instrument, int note, boolean powered) {
        return reservedNoteStates.containsKey(noteStateKey(instrument, note, powered));
    }

    public Optional<String> getReservedStateOwner(Instrument instrument, int note, boolean powered) {
        return Optional.ofNullable(reservedNoteStates.get(noteStateKey(instrument, note, powered)));
    }

    private NoteBlockState resolveLegacyNoteBlockState(String id, ConfigurationSection blockSection) {
        if (blockSection == null) {
            warn(id, "Legacy NOTE_BLOCK config requires block section.");
            return null;
        }

        if (blockSection.isInt("variation")) {
            int variation = blockSection.getInt("variation");
            NoteBlockState state = NoteBlockVariationUtil.fromVariation(variation);
            if (state == null) {
                warn(id, "Invalid block.variation: " + variation + " (max=" + NoteBlockVariationUtil.maxVariations() + ")");
                return null;
            }
            debug(id + ": " + NoteBlockVariationUtil.toDebugString(variation));
            return state;
        }

        String instrumentRaw = blockSection.getString("instrument");
        Instrument instrument = NoteBlockInstrumentUtil.parse(instrumentRaw);
        if (instrument == null) {
            warn(id, "Invalid block.instrument: " + instrumentRaw);
            warn(id, "Available instruments: " + NoteBlockInstrumentUtil.availableInstrumentNames());
            return null;
        }

        if (!blockSection.isInt("note")) {
            warn(id, "block.note is required.");
            return null;
        }
        int note = blockSection.getInt("note");
        if (note < 0 || note > 24) {
            warn(id, "block.note must be in range 0..24: " + note);
            return null;
        }

        return new NoteBlockState(instrument, note, blockSection.getBoolean("powered", false));
    }

    private FormaBlockType resolveLegacyType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return FormaBlockType.NOTE_BLOCK;
        }
        FormaBlockType type = FormaBlockType.fromString(rawType);
        return type == null ? FormaBlockType.NOTE_BLOCK : type;
    }

    private VisualStateProviderType resolveProviderType(ConfigurationSection stateSection, FormaBlockType fallbackType) {
        if (stateSection != null && stateSection.isSet("provider")) {
            return VisualStateProviderType.fromString(stateSection.getString("provider"));
        }
        if (fallbackType == FormaBlockType.NOTE_BLOCK) {
            return VisualStateProviderType.NOTE_BLOCK;
        }
        return null;
    }

    private String readVariationRaw(ConfigurationSection stateSection, ConfigurationSection legacyBlockSection) {
        if (stateSection != null && stateSection.isSet("variation")) {
            Object value = stateSection.get("variation");
            return value == null ? null : String.valueOf(value).trim();
        }
        if (legacyBlockSection != null && legacyBlockSection.isSet("variation")) {
            Object value = legacyBlockSection.get("variation");
            return value == null ? null : String.valueOf(value).trim();
        }
        return null;
    }

    private Integer parsePositiveInt(String raw) {
        try {
            int value = Integer.parseInt(raw);
            return value >= 1 ? value : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Material parseMaterial(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Material.matchMaterial(raw.trim());
    }

    private String noteStateKey(Instrument instrument, int note, boolean powered) {
        return "instrument=" + NoteBlockInstrumentUtil.toBlockStateName(instrument)
                + ",note=" + note
                + ",powered=" + powered;
    }

    private NoteBlockState toNoteBlockState(VisualBlockState visualState) {
        if (visualState.material() != Material.NOTE_BLOCK) {
            return null;
        }

        String instrumentRaw = visualState.properties().get("instrument");
        String noteRaw = visualState.properties().get("note");
        String poweredRaw = visualState.properties().get("powered");
        if (instrumentRaw == null || noteRaw == null || poweredRaw == null) {
            return null;
        }

        Instrument instrument = NoteBlockInstrumentUtil.parse(instrumentRaw);
        if (instrument == null) {
            return null;
        }

        int note;
        try {
            note = Integer.parseInt(noteRaw);
        } catch (NumberFormatException ex) {
            return null;
        }
        if (note < 0 || note > 24) {
            return null;
        }

        if (!"true".equalsIgnoreCase(poweredRaw) && !"false".equalsIgnoreCase(poweredRaw)) {
            return null;
        }
        boolean powered = Boolean.parseBoolean(poweredRaw);

        return new NoteBlockState(instrument, note, powered);
    }

    private String readString(ConfigurationSection section, String key, String legacyValue, String defaultValue) {
        if (section != null && section.isString(key)) {
            String value = section.getString(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        if (legacyValue != null && !legacyValue.isBlank()) {
            return legacyValue;
        }
        return defaultValue;
    }

    private double readDouble(ConfigurationSection section, String key, Double legacyValue, double defaultValue) {
        if (section != null && section.isSet(key)) {
            return section.getDouble(key, defaultValue);
        }
        return legacyValue == null ? defaultValue : legacyValue;
    }

    private FormaBlockSounds readSounds(ConfigurationSection settingsSection) {
        if (settingsSection == null) {
            return FormaBlockSounds.defaults();
        }
        ConfigurationSection soundsSection = settingsSection.getConfigurationSection("sounds");
        if (soundsSection == null) {
            return FormaBlockSounds.defaults();
        }
        return new FormaBlockSounds(
                soundsSection.getString("place"),
                soundsSection.getString("break"),
                soundsSection.getString("step"),
                soundsSection.getString("hit"),
                soundsSection.getString("fall"),
                (float) soundsSection.getDouble("volume", 1.0D),
                (float) soundsSection.getDouble("pitch", 1.0D)
        );
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    private void warn(String id, String message) {
        plugin.getLogger().warning("blocks.yml [" + id + "] " + message + " -> skipped.");
    }

    private void warnRecipe(String id, String message) {
        plugin.getLogger().warning("blocks.yml [" + id + "] " + message);
    }

    private void debug(String message) {
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
}
