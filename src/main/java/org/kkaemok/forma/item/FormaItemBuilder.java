package org.kkaemok.forma.item;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.kkaemok.forma.Forma;
import org.kkaemok.forma.util.KeyUtil;
import org.kkaemok.forma.util.TextUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class FormaItemBuilder {
    private final Forma plugin;

    public FormaItemBuilder(Forma plugin) {
        this.plugin = plugin;
    }

    public BuildResult buildWithDebug(FormaItem item) {
        ItemStack stack = ItemStack.of(item.material());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            ModelMode mode = readModelMode();
            return new BuildResult(
                    stack,
                    new ModelDebugInfo(
                            item.id(),
                            mode.name(),
                            "(none)",
                            false,
                            false,
                            "ItemMeta가 null이라 item_model 적용 불가",
                            false
                    )
            );
        }

        if (item.name() != null && !item.name().isBlank()) {
            meta.displayName(TextUtil.parse(item.name()));
        }

        if (!item.lore().isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : item.lore()) {
                lore.add(TextUtil.parse(line));
            }
            meta.lore(lore);
        }

        ModelApplyState modelState = applyModel(meta, item);

        FormaItemSettings settings = item.settings();
        if (settings.glow()) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        if (settings.unbreakable()) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }
        if (settings.hideTooltip()) {
            meta.setHideTooltip(true);
        }
        if (settings.hideAttributes()) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }
        if (settings.maxStackSize() != null) {
            meta.setMaxStackSize(settings.maxStackSize());
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KeyUtil.itemId(plugin), PersistentDataType.STRING, item.id());
        pdc.set(KeyUtil.itemType(plugin), PersistentDataType.STRING, item.type().name());

        applyDurability(meta, item);
        boolean attributesApplied = applyAttributes(meta, item);
        stack.setItemMeta(meta);

        return new BuildResult(
                stack,
                new ModelDebugInfo(
                        item.id(),
                        modelState.mode().name(),
                        modelState.parsedKey() == null ? "(none)" : modelState.parsedKey().toString(),
                        modelState.itemModelApplied(),
                        modelState.fallbackUsed(),
                        modelState.failureReason(),
                        attributesApplied
                )
        );
    }

    private ModelApplyState applyModel(ItemMeta meta, FormaItem item) {
        ModelMode mode = readModelMode();
        NamespacedKey parsedKey = parseItemModelKey(item.model());

        if (mode == ModelMode.ITEM_MODEL) {
            if (parsedKey == null) {
                boolean fallbackUsed = applyCustomModelData(meta, item.customModelData());
                ModelApplyState state = new ModelApplyState(
                        mode,
                        null,
                        false,
                        fallbackUsed,
                        "model 문자열 형식 파싱 실패: " + item.model()
                );
                logItemModelFailureIfDebug(item, state);
                return state;
            }

            ItemModelApplyResult applyResult = applyItemModelReflectively(meta, parsedKey);
            boolean fallbackUsed = !applyResult.success() && applyCustomModelData(meta, item.customModelData());
            ModelApplyState state = new ModelApplyState(
                    mode,
                    parsedKey,
                    applyResult.success(),
                    fallbackUsed,
                    applyResult.failureReason()
            );
            logItemModelFailureIfDebug(item, state);
            return state;
        }

        applyCustomModelData(meta, item.customModelData());
        return new ModelApplyState(mode, parsedKey, false, false, null);
    }

    private ItemModelApplyResult applyItemModelReflectively(ItemMeta meta, NamespacedKey itemModel) {
        List<Throwable> failures = new ArrayList<>(3);

        // Paper 1.21.11의 직접 API를 우선 사용한다.
        try {
            meta.setItemModel(itemModel);
            return new ItemModelApplyResult(true, null);
        } catch (Throwable ex) {
            failures.add(ex);
        }

        // 다른 API 버전에서는 ItemMeta 선언 메서드로 다시 시도한다.
        try {
            Method method = ItemMeta.class.getMethod("setItemModel", NamespacedKey.class);
            method.invoke(meta, itemModel);
            return new ItemModelApplyResult(true, null);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            failures.add(ex);
        }

        // 구현 클래스에만 메서드가 노출된 서버도 마지막으로 지원한다.
        try {
            Method method = meta.getClass().getMethod("setItemModel", NamespacedKey.class);
            method.invoke(meta, itemModel);
            return new ItemModelApplyResult(true, null);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            failures.add(ex);
        }

        Throwable failure = failures.getLast();
        return new ItemModelApplyResult(false, toFailureReason(failure));
    }

    private String toFailureReason(Throwable throwable) {
        if (throwable == null) {
            return "알 수 없는 원인";
        }

        Throwable root = throwable.getCause() == null ? throwable : throwable.getCause();
        return root.getClass().getSimpleName() + ": " + Objects.toString(root.getMessage(), "(no message)");
    }

    private void logItemModelFailureIfDebug(FormaItem item, ModelApplyState state) {
        if (!plugin.getConfig().getBoolean("debug", false)) {
            return;
        }
        if (state.mode() != ModelMode.ITEM_MODEL || state.itemModelApplied()) {
            return;
        }

        plugin.getLogger().info("[DEBUG] item_model 적용 실패 - id=" + item.id()
                + ", model=" + item.model()
                + ", reason=" + Objects.toString(state.failureReason(), "(no reason)"));
    }

    private ModelMode readModelMode() {
        String raw = Objects.requireNonNullElse(plugin.getConfig().getString("model-mode"), ModelMode.ITEM_MODEL.name());
        if (raw.isBlank()) {
            return ModelMode.ITEM_MODEL;
        }
        try {
            return ModelMode.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ModelMode.ITEM_MODEL;
        }
    }

    private boolean applyCustomModelData(ItemMeta meta, Integer customModelData) {
        if (customModelData == null) {
            return false;
        }

        if (applyCustomModelDataComponentReflectively(meta, customModelData)) {
            return true;
        }

        return applyLegacyCustomModelDataReflectively(meta, customModelData);
    }

    private boolean applyCustomModelDataComponentReflectively(ItemMeta meta, int customModelData) {
        try {
            Method getComponent = meta.getClass().getMethod("getCustomModelDataComponent");
            Object component = getComponent.invoke(meta);
            if (component == null) {
                return false;
            }

            Method setFloats = component.getClass().getMethod("setFloats", List.class);
            setFloats.invoke(component, List.of((float) customModelData));

            Method setComponent = findSingleArgMethod(meta.getClass(), "setCustomModelDataComponent");
            if (setComponent == null) {
                return false;
            }
            setComponent.invoke(meta, component);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return false;
        }
    }

    private boolean applyLegacyCustomModelDataReflectively(ItemMeta meta, int customModelData) {
        try {
            Method legacyMethod = findSingleArgMethod(meta.getClass(), "setCustomModelData");
            if (legacyMethod != null) {
                legacyMethod.invoke(meta, customModelData);
                return true;
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            // no-op
        }
        return false;
    }

    private Method findSingleArgMethod(Class<?> targetClass, String methodName) {
        for (Method method : targetClass.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                return method;
            }
        }
        return null;
    }

    private NamespacedKey parseItemModelKey(String rawModel) {
        if (rawModel == null || rawModel.isBlank()) {
            return null;
        }

        String input = rawModel.trim();
        if (!input.contains(":")) {
            String namespace = Objects.requireNonNullElse(plugin.getConfig().getString("namespace"), "forma");
            if (namespace.isBlank()) {
                namespace = "forma";
            }
            input = namespace + ":" + input;
        }

        String[] split = input.split(":", 2);
        if (split.length != 2 || split[0].isBlank() || split[1].isBlank()) {
            return null;
        }

        try {
            return new NamespacedKey(
                    split[0].toLowerCase(Locale.ROOT),
                    split[1].toLowerCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void applyDurability(ItemMeta meta, FormaItem item) {
        if (!item.type().supportsDurability()) {
            return;
        }

        Integer configuredMax = item.settings().durabilityMax();
        Integer configuredCurrent = item.settings().durabilityCurrent();
        if (configuredMax == null || configuredCurrent == null || configuredMax <= 0) {
            return;
        }

        int max = configuredMax;
        int current = configuredCurrent;
        current = Math.clamp(current, 0, max);
        int calculatedDamage = Math.clamp(max - current, 0, max);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KeyUtil.durabilityMax(plugin), PersistentDataType.INTEGER, max);
        pdc.set(KeyUtil.durabilityCurrent(plugin), PersistentDataType.INTEGER, current);

        if (item.settings().unbreakable() || !(meta instanceof Damageable damageable)) {
            return;
        }

        boolean maxDamageApplied = applyMaxDamageReflectively(damageable, max);
        boolean fallbackUsed = false;

        if (maxDamageApplied) {
            damageable.setDamage(calculatedDamage);
            meta.setMaxStackSize(1);
        } else {
            fallbackUsed = true;
            int vanillaMax = item.material().getMaxDurability();
            if (vanillaMax > 0) {
                double ratio = (double) current / (double) max;
                int visualDamage = (int) Math.round(vanillaMax - (ratio * vanillaMax));
                visualDamage = Math.clamp(visualDamage, 0, vanillaMax);
                damageable.setDamage(visualDamage);
            }
        }

        logDurabilityDebug(item, max, current, calculatedDamage, maxDamageApplied, fallbackUsed);
    }

    private boolean applyAttributes(ItemMeta meta, FormaItem item) {
        FormaItemAttributes attributes = item.attributes();
        if (attributes.isEmpty()) {
            return false;
        }

        if (plugin.getConfig().getBoolean("items.replace-vanilla-attributes", true)) {
            meta.setAttributeModifiers(null);
        }
        EquipmentSlotGroup slot = inferEquipmentSlot(item.material());
        int applied = 0;
        applied += addAttribute(meta, item, "attack_damage", Attribute.ATTACK_DAMAGE, attributes.attackDamage(), slot);
        applied += addAttribute(meta, item, "attack_speed", Attribute.ATTACK_SPEED, attributes.attackSpeed(), slot);
        applied += addAttribute(meta, item, "armor", Attribute.ARMOR, attributes.armor(), slot);
        applied += addAttribute(meta, item, "armor_toughness", Attribute.ARMOR_TOUGHNESS, attributes.armorToughness(), slot);
        applied += addAttribute(meta, item, "knockback_resistance", Attribute.KNOCKBACK_RESISTANCE,
                attributes.knockbackResistance(), slot);
        applied += addAttribute(meta, item, "movement_speed", Attribute.MOVEMENT_SPEED, attributes.movementSpeed(), slot);

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[DEBUG] attributes item id=" + item.id()
                    + ", applied=" + applied + ", slot=" + slot);
        }
        return applied > 0;
    }

    private int addAttribute(
            ItemMeta meta,
            FormaItem item,
            String key,
            Attribute attribute,
            Double value,
            EquipmentSlotGroup slot
    ) {
        if (value == null) {
            return 0;
        }
        NamespacedKey modifierKey = new NamespacedKey(
                plugin,
                ("item_" + item.id() + "_" + key).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_")
        );
        AttributeModifier modifier = new AttributeModifier(
                modifierKey,
                value,
                AttributeModifier.Operation.ADD_NUMBER,
                slot
        );
        return meta.addAttributeModifier(attribute, modifier) ? 1 : 0;
    }

    private EquipmentSlotGroup inferEquipmentSlot(org.bukkit.Material material) {
        String name = material.name();
        if (name.endsWith("_HELMET")) {
            return EquipmentSlotGroup.HEAD;
        }
        if (name.endsWith("_CHESTPLATE")) {
            return EquipmentSlotGroup.CHEST;
        }
        if (name.endsWith("_LEGGINGS")) {
            return EquipmentSlotGroup.LEGS;
        }
        if (name.endsWith("_BOOTS")) {
            return EquipmentSlotGroup.FEET;
        }
        return EquipmentSlotGroup.HAND;
    }

    private boolean applyMaxDamageReflectively(Damageable damageable, int maxDamage) {
        return invokeSingleArgMethod(Damageable.class, damageable, "setMaxDamage", int.class, maxDamage)
                || invokeSingleArgMethod(Damageable.class, damageable, "setMaxDamage", Integer.class, maxDamage)
                || invokeSingleArgMethod(damageable.getClass(), damageable, "setMaxDamage", int.class, maxDamage)
                || invokeSingleArgMethod(damageable.getClass(), damageable, "setMaxDamage", Integer.class, maxDamage);
    }

    private boolean invokeSingleArgMethod(
            Class<?> methodOwner,
            Object target,
            String methodName,
            Class<?> paramType,
            Object arg
    ) {
        try {
            Method method = methodOwner.getMethod(methodName, paramType);
            method.invoke(target, arg);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return false;
        }
    }

    private void logDurabilityDebug(
            FormaItem item,
            int max,
            int current,
            int calculatedDamage,
            boolean maxDamageApplied,
            boolean fallbackUsed
    ) {
        if (!plugin.getConfig().getBoolean("debug", false)) {
            return;
        }

        plugin.getLogger().info("[DEBUG] durability item id: " + item.id());
        plugin.getLogger().info("[DEBUG] durability max/current: " + max + "/" + current);
        plugin.getLogger().info("[DEBUG] calculated damage: " + calculatedDamage);
        plugin.getLogger().info("[DEBUG] max_damage 적용 성공 여부: " + maxDamageApplied);
        plugin.getLogger().info("[DEBUG] fallback 사용 여부: " + fallbackUsed);
    }

    private enum ModelMode {
        ITEM_MODEL,
        CUSTOM_MODEL_DATA
    }

    private record ItemModelApplyResult(boolean success, String failureReason) {
    }

    private record ModelApplyState(
            ModelMode mode,
            NamespacedKey parsedKey,
            boolean itemModelApplied,
            boolean fallbackUsed,
            String failureReason
    ) {
    }

    public record ModelDebugInfo(
            String itemId,
            String modelMode,
            String parsedModelKey,
            boolean itemModelApplied,
            boolean fallbackUsed,
            String failureReason,
            boolean attributesApplied
    ) {
    }

    public record BuildResult(ItemStack itemStack, ModelDebugInfo debugInfo) {
    }
}
