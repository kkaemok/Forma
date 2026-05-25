package org.kkaemok.forma.api.internal;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.kkaemok.forma.Forma;
import org.kkaemok.forma.api.item.FormaItemAPI;
import org.kkaemok.forma.api.item.FormaItemData;
import org.kkaemok.forma.item.FormaItem;
import org.kkaemok.forma.item.FormaItemBuilder;
import org.kkaemok.forma.util.KeyUtil;
import org.kkaemok.forma.util.TextUtil;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

public final class DefaultFormaItemAPI implements FormaItemAPI {
    private final Forma plugin;
    private final FormaItemBuilder builder;

    public DefaultFormaItemAPI(Forma plugin) {
        this.plugin = plugin;
        this.builder = new FormaItemBuilder(plugin);
    }

    @Override
    public Optional<FormaItemData> getItem(String id) {
        requireActive();
        FormaItem item = plugin.getItemManager().getItem(id);
        return item == null ? Optional.empty() : Optional.of(toData(item));
    }

    @Override
    public boolean hasItem(String id) {
        requireActive();
        return plugin.getItemManager().getItem(id) != null;
    }

    @Override
    public Set<String> getItemIds() {
        requireActive();
        return Set.copyOf(plugin.getItemManager().getIds());
    }

    @Override
    public ItemStack createItem(String id) {
        return createItem(id, 1);
    }

    @Override
    public ItemStack createItem(String id, int amount) {
        requireActive();
        FormaItem item = requireItem(id);
        ItemStack stack = builder.buildWithDebug(item).itemStack().clone();
        setAmount(stack, amount);
        return stack;
    }

    @Override
    public Optional<String> getItemId(ItemStack item) {
        requireActive();
        if (item == null || item.getType() == Material.AIR) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        return meta == null
                ? Optional.empty()
                : Optional.ofNullable(KeyUtil.readItemId(plugin, meta.getPersistentDataContainer()));
    }

    @Override
    public boolean isFormaItem(ItemStack item) {
        return getItemId(item).isPresent();
    }

    private FormaItem requireItem(String id) {
        FormaItem item = plugin.getItemManager().getItem(id);
        if (item == null) {
            throw new IllegalArgumentException("등록되지 않은 Forma 아이템 ID입니다: " + id);
        }
        return item;
    }

    private void setAmount(ItemStack stack, int amount) {
        if (amount < 1 || amount > stack.getMaxStackSize()) {
            throw new IllegalArgumentException("아이템 수량은 1~" + stack.getMaxStackSize() + " 범위여야 합니다.");
        }
        stack.setAmount(amount);
    }

    private FormaItemData toData(FormaItem item) {
        return new FormaItemData(
                item.id(),
                item.type().name(),
                item.material(),
                item.name() == null ? Component.empty() : TextUtil.parse(item.name()),
                item.lore().stream().map(TextUtil::parse).toList(),
                item.model() == null ? "" : item.model(),
                item.customModelData() == null ? 0 : item.customModelData(),
                item.settings().glow(),
                item.settings().unbreakable(),
                optionalInt(item.settings().durabilityMax()),
                optionalInt(item.settings().durabilityCurrent())
        );
    }

    private OptionalInt optionalInt(Integer value) {
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }

    private void requireActive() {
        if (!plugin.isEnabled()) {
            throw new IllegalStateException("Forma API is not active.");
        }
    }
}
