package org.kkaemok.forma.item;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.kkaemok.forma.Forma;
import org.kkaemok.forma.api.event.FormaItemUseEvent;
import org.kkaemok.forma.util.KeyUtil;

public final class FormaItemListener implements Listener {
    private final Forma plugin;
    private final FormaItemBehaviorManager behaviorManager;

    public FormaItemListener(Forma plugin) {
        this.plugin = plugin;
        this.behaviorManager = new FormaItemBehaviorManager(plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        ItemStack stack = event.getItem();
        if (stack == null || stack.getType() == Material.AIR) {
            return;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null || KeyUtil.readBlockId(plugin, meta.getPersistentDataContainer()) != null) {
            return;
        }
        String itemId = KeyUtil.readItemId(plugin, meta.getPersistentDataContainer());
        if (itemId == null) {
            return;
        }
        FormaItem item = plugin.getItemManager().getItem(itemId);
        if (item == null || item.rightClickBehaviors().isEmpty()) {
            return;
        }
        FormaItemUseEvent useEvent = new FormaItemUseEvent(
                event.getPlayer(), item.id(), stack, event.getHand(), event.getAction()
        );
        plugin.getServer().getPluginManager().callEvent(useEvent);
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[DEBUG] API event: FormaItemUseEvent, item="
                    + item.id() + ", cancelled=" + useEvent.isCancelled());
        }
        if (useEvent.isCancelled()) {
            return;
        }
        behaviorManager.executeRightClick(event.getPlayer(), item);
    }
}
