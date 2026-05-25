package org.kkaemok.forma.api.item;

import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.Set;

public interface FormaItemAPI {
    Optional<FormaItemData> getItem(String id);

    boolean hasItem(String id);

    Set<String> getItemIds();

    ItemStack createItem(String id);

    ItemStack createItem(String id, int amount);

    Optional<String> getItemId(ItemStack item);

    boolean isFormaItem(ItemStack item);
}
