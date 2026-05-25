package org.kkaemok.forma.item;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.kkaemok.forma.Forma;
import org.kkaemok.forma.item.FormaItemBehavior.CommandSenderType;
import org.kkaemok.forma.util.TextUtil;

import java.util.Locale;

public final class FormaItemBehaviorManager {
    private final Forma plugin;
    private final FormaItemCooldownManager cooldownManager = new FormaItemCooldownManager();

    public FormaItemBehaviorManager(Forma plugin) {
        this.plugin = plugin;
    }

    public void executeRightClick(Player player, FormaItem item) {
        boolean cooldownMessageSent = false;
        for (int index = 0; index < item.rightClickBehaviors().size(); index++) {
            FormaItemBehavior behavior = item.rightClickBehaviors().get(index);
            long remaining = cooldownManager.remainingMillis(player.getUniqueId(), item.id(), index);
            if (remaining > 0L) {
                if (!cooldownMessageSent) {
                    sendCooldownMessage(player, remaining);
                    cooldownMessageSent = true;
                }
                debug(player, item, behavior, "cooldown " + remaining + "ms");
                continue;
            }

            boolean executed = execute(player, item, behavior);
            if (executed) {
                cooldownManager.start(player.getUniqueId(), item.id(), index, behavior.cooldownMillis());
            }
            debug(player, item, behavior, executed ? "success" : "failed");
        }
    }

    private boolean execute(Player player, FormaItem item, FormaItemBehavior behavior) {
        return switch (behavior.type()) {
            case SOUND -> playSound(player, item, behavior);
            case MESSAGE -> sendMessage(player, item, behavior);
            case COMMAND -> runCommand(player, item, behavior);
        };
    }

    private boolean playSound(Player player, FormaItem item, FormaItemBehavior behavior) {
        String key = replacePlaceholders(behavior.sound(), player, item);
        if (key == null || key.isBlank()) {
            plugin.getLogger().warning("[FormaItem] sound behavior에 sound 값이 없습니다: " + item.id());
            return false;
        }

        Location location = player.getLocation();
        if (key.contains(":")) {
            player.getWorld().playSound(location, key, behavior.volume(), behavior.pitch());
            return true;
        }

        Sound sound = Registry.SOUND_EVENT.get(NamespacedKey.minecraft(
                key.toLowerCase(Locale.ROOT).replace('_', '.')
        ));
        if (sound == null) {
            plugin.getLogger().warning("[FormaItem] 알 수 없는 sound behavior 사운드입니다: "
                    + key + " (item=" + item.id() + ")");
            return false;
        }
        player.getWorld().playSound(location, sound, behavior.volume(), behavior.pitch());
        return true;
    }

    private boolean sendMessage(Player player, FormaItem item, FormaItemBehavior behavior) {
        String message = replacePlaceholders(behavior.message(), player, item);
        if (message == null || message.isBlank()) {
            plugin.getLogger().warning("[FormaItem] message behavior에 message 값이 없습니다: " + item.id());
            return false;
        }
        player.sendMessage(TextUtil.parse(message));
        return true;
    }

    private boolean runCommand(Player player, FormaItem item, FormaItemBehavior behavior) {
        String command = replacePlaceholders(behavior.command(), player, item);
        if (command == null || command.isBlank()) {
            plugin.getLogger().warning("[FormaItem] command behavior에 command 값이 없습니다: " + item.id());
            return false;
        }
        if (behavior.sender() == CommandSenderType.PLAYER) {
            return Bukkit.dispatchCommand(player, command);
        }
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    private String replacePlaceholders(String input, Player player, FormaItem item) {
        if (input == null) {
            return null;
        }
        return input
                .replace("%player%", player.getName())
                .replace("%item_id%", item.id());
    }

    private void sendCooldownMessage(Player player, long remainingMillis) {
        String configured = plugin.getConfig().getString(
                "items.behavior-cooldown-message",
                "<red>아직 사용할 수 없습니다. 남은 시간: %time%"
        );
        String seconds = String.format(Locale.ROOT, "%.1f초", remainingMillis / 1000.0D);
        player.sendMessage(TextUtil.parse(configured.replace("%time%", seconds)));
    }

    private void debug(Player player, FormaItem item, FormaItemBehavior behavior, String result) {
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[DEBUG] behavior: player=" + player.getName()
                    + ", item=" + item.id()
                    + ", type=" + behavior.type()
                    + ", result=" + result);
        }
    }
}
