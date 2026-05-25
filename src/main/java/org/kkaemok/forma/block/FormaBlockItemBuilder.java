package org.kkaemok.forma.block;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
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

public final class FormaBlockItemBuilder {
    private final Forma plugin;

    public FormaBlockItemBuilder(Forma plugin) {
        this.plugin = plugin;
    }

    public ItemStack build(FormaBlock block) {
        ItemStack stack = ItemStack.of(block.itemMaterial());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        if (block.name() != null && !block.name().isBlank()) {
            meta.displayName(TextUtil.parse(block.name()));
        }

        if (!block.lore().isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : block.lore()) {
                lore.add(TextUtil.parse(line));
            }
            meta.lore(lore);
        }

        applyItemModel(meta, block);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KeyUtil.blockId(plugin), PersistentDataType.STRING, block.id());
        pdc.set(KeyUtil.blockType(plugin), PersistentDataType.STRING, block.type().name());

        stack.setItemMeta(meta);
        return stack;
    }

    private void applyItemModel(ItemMeta meta, FormaBlock block) {
        String mode = Objects.requireNonNullElse(plugin.getConfig().getString("model-mode"), "ITEM_MODEL");
        Integer customModelData = block.customModelData();

        if ("ITEM_MODEL".equalsIgnoreCase(mode)) {
            NamespacedKey key = parseItemModelKey(block.itemModel());
            boolean applied = key != null && applyItemModelReflectively(meta, key);
            if (!applied && customModelData != null) {
                applyCustomModelData(meta, customModelData);
            }
            return;
        }

        if (customModelData != null) {
            applyCustomModelData(meta, customModelData);
        }
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
            return new NamespacedKey(split[0].toLowerCase(Locale.ROOT), split[1].toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean applyItemModelReflectively(ItemMeta meta, NamespacedKey key) {
        try {
            meta.setItemModel(key);
            return true;
        } catch (Throwable ex) {
            // fallback to reflection
        }

        try {
            Method method = ItemMeta.class.getMethod("setItemModel", NamespacedKey.class);
            method.invoke(meta, key);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return false;
        }
    }

    private void applyCustomModelData(ItemMeta meta, int customModelData) {
        try {
            Method getComponent = meta.getClass().getMethod("getCustomModelDataComponent");
            Object component = getComponent.invoke(meta);
            if (component != null) {
                Method setFloats = component.getClass().getMethod("setFloats", List.class);
                setFloats.invoke(component, List.of((float) customModelData));
                Method setComponent = findSingleArgMethod(meta.getClass(), "setCustomModelDataComponent");
                if (setComponent != null) {
                    setComponent.invoke(meta, component);
                    return;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // fallback to legacy method
        }

        try {
            Method legacyMethod = findSingleArgMethod(meta.getClass(), "setCustomModelData");
            if (legacyMethod != null) {
                legacyMethod.invoke(meta, customModelData);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // no-op
        }
    }

    private Method findSingleArgMethod(Class<?> type, String methodName) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                return method;
            }
        }
        return null;
    }
}
