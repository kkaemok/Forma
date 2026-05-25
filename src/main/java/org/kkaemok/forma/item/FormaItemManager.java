package org.kkaemok.forma.item;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.kkaemok.forma.Forma;
import org.kkaemok.forma.item.FormaItemBehavior.CommandSenderType;
import org.kkaemok.forma.recipe.FormaRecipeDefinition;
import org.kkaemok.forma.recipe.FormaRecipeParser;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FormaItemManager {
    private static final Pattern COOLDOWN_PATTERN = Pattern.compile("^(\\d+)(ms|s|m|h)?$");

    private final Forma plugin;
    private final File itemsFile;
    private final Map<String, FormaItem> items = new LinkedHashMap<>();
    private final List<String> validationWarnings = new ArrayList<>();

    public FormaItemManager(Forma plugin) {
        this.plugin = plugin;
        this.itemsFile = new File(plugin.getDataFolder(), "items.yml");
    }

    public void loadItems() {
        if (!itemsFile.exists()) {
            plugin.saveResource("items.yml", false);
        }

        items.clear();
        validationWarnings.clear();
        YamlConfiguration itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
        for (String id : itemsConfig.getKeys(false)) {
            ConfigurationSection section = itemsConfig.getConfigurationSection(id);
            if (section == null) {
                warn(id, "아이템 섹션이 비어 있어 스킵합니다.");
                continue;
            }

            FormaItem item = loadItem(id, section);
            if (item != null) {
                items.put(id, item);
                debugItem(item);
            }
        }

        plugin.getLogger().info("items.yml 로드 완료: " + items.size() + "개");
    }

    private FormaItem loadItem(String id, ConfigurationSection section) {
        String typeRaw = section.getString("type");
        FormaItemType type = FormaItemType.fromString(typeRaw);
        if (type == null) {
            warn(id, typeRaw == null || typeRaw.isBlank()
                    ? "필수 필드 type이 누락되었습니다. 아이템을 스킵합니다."
                    : "잘못된 type 값입니다: " + typeRaw + ". 아이템을 스킵합니다.");
            return null;
        }

        ConfigurationSection itemSection = section.getConfigurationSection("item");
        ConfigurationSection settingsSection = section.getConfigurationSection("settings");
        ConfigurationSection attributesSection = section.getConfigurationSection("attributes");
        boolean legacyFormat = itemSection == null;

        String materialRaw = readString(itemSection, "material", section.getString("material"));
        if (materialRaw == null || materialRaw.isBlank()) {
            warn(id, "필수 필드 item.material 또는 material이 누락되었습니다. 아이템을 스킵합니다.");
            return null;
        }
        Material material = Material.matchMaterial(materialRaw);
        if (material == null) {
            warn(id, "잘못된 material 값입니다: " + materialRaw + ". 아이템을 스킵합니다.");
            return null;
        }

        String name = readString(itemSection, "name", section.getString("name"));
        String model = readString(itemSection, "model", section.getString("model"));
        if (model == null || model.isBlank()) {
            warn(id, "필수 필드 item.model 또는 model이 누락되었습니다. item_model과 리소스팩 모델을 적용할 수 없습니다.");
        }
        Integer customModelData = itemSection != null && itemSection.isSet("custom-model-data")
                ? readOptionalInteger(id, itemSection, "custom-model-data", "item.custom-model-data")
                : readOptionalInteger(id, section, "custom-model-data", "custom-model-data");
        List<String> lore = itemSection != null && itemSection.isList("lore")
                ? itemSection.getStringList("lore")
                : section.getStringList("lore");

        FormaItemSettings settings = loadSettings(id, type, section, settingsSection);
        Double legacyDamage = readOptionalDouble(id, section, "damage", "damage", null);
        FormaItemAttributes attributes = loadAttributes(id, attributesSection, legacyDamage);
        List<FormaItemBehavior> behaviors = loadRightClickBehaviors(id, section);
        FormaRecipeDefinition recipe = FormaRecipeParser.parse(
                section.getConfigurationSection("recipe"),
                message -> warn(id, message)
        );

        return new FormaItem(
                id,
                type,
                material,
                name,
                model,
                customModelData,
                lore,
                settings,
                attributes,
                behaviors,
                recipe,
                legacyDamage,
                legacyFormat
        );
    }

    private FormaItemSettings loadSettings(
            String id,
            FormaItemType type,
            ConfigurationSection legacy,
            ConfigurationSection settings
    ) {
        boolean glow = readBooleanWithLegacyFallback(id, settings, legacy, "glow", false);
        boolean unbreakable = readBooleanWithLegacyFallback(id, settings, legacy, "unbreakable", false);
        boolean hideTooltip = readOptionalBoolean(id, settings, "hide-tooltip", "settings.hide-tooltip", false);
        boolean hideAttributes = readOptionalBoolean(id, settings, "hide-attributes", "settings.hide-attributes", false);
        Integer maxStackSize = readOptionalInteger(id, settings, "max-stack-size", "settings.max-stack-size");
        if (maxStackSize != null && (maxStackSize < 1 || maxStackSize > 64)) {
            warn(id, "settings.max-stack-size는 1~64 범위의 정수여야 합니다. 해당 설정을 무시합니다.");
            maxStackSize = null;
        }

        ConfigurationSection durability = settings == null ? null : settings.getConfigurationSection("durability");
        String durabilityPath = "settings.durability";
        if (durability == null) {
            durability = legacy.getConfigurationSection("durability");
            durabilityPath = "durability";
        }
        Integer durabilityMax = null;
        Integer durabilityCurrent = null;
        if (type.supportsDurability() && durability != null) {
            if (!durability.isSet("max")) {
                warn(id, durabilityPath + ".max가 누락되었습니다. 내구도 설정을 무시합니다.");
            } else {
                durabilityMax = readOptionalInteger(id, durability, "max", durabilityPath + ".max");
            }
            if (!durability.isSet("current")) {
                warn(id, durabilityPath + ".current가 누락되었습니다. 내구도 설정을 무시합니다.");
            } else {
                durabilityCurrent = readOptionalInteger(id, durability, "current", durabilityPath + ".current");
            }
            if (durabilityMax != null && durabilityMax <= 0) {
                warn(id, durabilityPath + ".max는 1 이상의 정수여야 합니다. 내구도 설정을 무시합니다.");
                durabilityMax = null;
                durabilityCurrent = null;
            }
            if (durabilityMax == null || durabilityCurrent == null) {
                durabilityMax = null;
                durabilityCurrent = null;
            }
        }
        return new FormaItemSettings(
                glow,
                unbreakable,
                hideTooltip,
                hideAttributes,
                maxStackSize,
                durabilityMax,
                durabilityCurrent
        );
    }

    private FormaItemAttributes loadAttributes(String id, ConfigurationSection attributes, Double legacyDamage) {
        return new FormaItemAttributes(
                readOptionalDouble(id, attributes, "attack_damage", "attributes.attack_damage", legacyDamage),
                readOptionalDouble(id, attributes, "attack_speed", "attributes.attack_speed", null),
                readOptionalDouble(id, attributes, "armor", "attributes.armor", null),
                readOptionalDouble(id, attributes, "armor_toughness", "attributes.armor_toughness", null),
                readOptionalDouble(id, attributes, "knockback_resistance", "attributes.knockback_resistance", null),
                readOptionalDouble(id, attributes, "movement_speed", "attributes.movement_speed", null)
        );
    }

    private List<FormaItemBehavior> loadRightClickBehaviors(String id, ConfigurationSection section) {
        List<Map<?, ?>> definitions = section.getMapList("behaviors.right_click");
        List<FormaItemBehavior> behaviors = new ArrayList<>();
        for (int index = 0; index < definitions.size(); index++) {
            Map<?, ?> definition = definitions.get(index);
            FormaItemBehaviorType type = FormaItemBehaviorType.fromString(asString(definition.get("type")));
            if (type == null) {
                warn(id, "behaviors.right_click[" + index + "] type이 잘못되어 스킵합니다.");
                continue;
            }

            CommandSenderType sender = CommandSenderType.fromString(asString(definition.get("sender")));
            if (type == FormaItemBehaviorType.COMMAND && sender == null) {
                warn(id, "behaviors.right_click[" + index + "] sender가 잘못되어 스킵합니다.");
                continue;
            }

            Long cooldown = parseCooldown(asString(definition.get("cooldown")));
            if (cooldown == null) {
                warn(id, "behaviors.right_click[" + index + "] cooldown 형식이 잘못되어 스킵합니다.");
                continue;
            }
            behaviors.add(new FormaItemBehavior(
                    type,
                    asString(definition.get("sound")),
                    asFloat(definition.get("volume"), 1.0F),
                    asFloat(definition.get("pitch"), 1.0F),
                    asString(definition.get("message")),
                    asString(definition.get("command")),
                    sender == null ? CommandSenderType.CONSOLE : sender,
                    cooldown
            ));
        }
        return behaviors;
    }

    public void reload() {
        loadItems();
    }

    public FormaItem getItem(String id) {
        return items.get(id);
    }

    public Map<String, FormaItem> getItems() {
        return Collections.unmodifiableMap(items);
    }

    public Set<String> getIds() {
        return Collections.unmodifiableSet(items.keySet());
    }

    public List<String> getValidationWarnings() {
        return List.copyOf(validationWarnings);
    }

    private String readString(ConfigurationSection section, String key, String fallback) {
        if (section != null && section.isString(key)) {
            return section.getString(key);
        }
        return fallback;
    }

    private boolean readOptionalBoolean(
            String id,
            ConfigurationSection section,
            String key,
            String path,
            boolean fallback
    ) {
        if (section == null || !section.isSet(key)) {
            return fallback;
        }
        Object value = section.get(key);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String text
                && ("true".equalsIgnoreCase(text.trim()) || "false".equalsIgnoreCase(text.trim()))) {
            return Boolean.parseBoolean(text.trim());
        }
        warn(id, path + "에는 true 또는 false를 입력해야 합니다. 해당 설정을 무시합니다.");
        return fallback;
    }

    private boolean readBooleanWithLegacyFallback(
            String id,
            ConfigurationSection settings,
            ConfigurationSection legacy,
            String key,
            boolean fallback
    ) {
        if (settings != null && settings.isSet(key)) {
            return readOptionalBoolean(id, settings, key, "settings." + key, fallback);
        }
        return readOptionalBoolean(id, legacy, key, key, fallback);
    }

    private Integer readOptionalInteger(String id, ConfigurationSection section, String key, String path) {
        if (section == null || !section.isSet(key)) {
            return null;
        }

        Object value = section.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ex) {
                warn(id, path + "에는 정수를 입력해야 합니다. 해당 설정을 무시합니다.");
                return null;
            }
        }
        warn(id, path + "에는 정수를 입력해야 합니다. 해당 설정을 무시합니다.");
        return null;
    }

    private Double readOptionalDouble(
            String id,
            ConfigurationSection section,
            String key,
            String path,
            Double fallback
    ) {
        if (section == null || !section.isSet(key)) {
            return fallback;
        }

        Object value = section.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ex) {
                warn(id, path + "에는 숫자를 입력해야 합니다. 해당 설정을 무시합니다.");
                return fallback;
            }
        }
        warn(id, path + "에는 숫자를 입력해야 합니다. 해당 설정을 무시합니다.");
        return fallback;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private float asFloat(Object value, float fallback) {
        if (value instanceof Number number) {
            return Math.max(0.0F, number.floatValue());
        }
        return fallback;
    }

    private Long parseCooldown(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0L;
        }
        Matcher matcher = COOLDOWN_PATTERN.matcher(raw.toLowerCase());
        if (!matcher.matches()) {
            return null;
        }
        long value = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);
        return switch (unit == null ? "ms" : unit) {
            case "ms" -> value;
            case "s" -> value * 1000L;
            case "m" -> value * 60_000L;
            case "h" -> value * 3_600_000L;
            default -> null;
        };
    }

    private void debugItem(FormaItem item) {
        if (!plugin.getConfig().getBoolean("debug", false)) {
            return;
        }
        if (item.legacyFormat()) {
            plugin.getLogger().info("[DEBUG] legacy item format used: " + item.id());
        }
        plugin.getLogger().info("[DEBUG] item loaded: id=" + item.id()
                + ", type=" + item.type()
                + ", material=" + item.material()
                + ", model=" + item.model()
                + ", durability=" + item.settings().durabilityMax() + "/" + item.settings().durabilityCurrent()
                + ", attributes=" + !item.attributes().isEmpty()
                + ", behaviors=" + item.rightClickBehaviors().size()
                + ", recipe=" + (item.recipe() != null)
                + ", legacyDamage=" + item.damage());
    }

    private void warn(String id, String message) {
        String formatted = "[" + id + "] " + message;
        validationWarnings.add(formatted);
        plugin.getLogger().warning("items.yml " + formatted);
    }
}
