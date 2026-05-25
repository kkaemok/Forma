package org.kkaemok.forma.block;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.kkaemok.forma.Forma;
import org.kkaemok.forma.api.event.FormaBlockBreakEvent;
import org.kkaemok.forma.block.FormaBlockSounds.SoundAction;
import org.kkaemok.forma.block.place.FormaBlockPlacementService;
import org.kkaemok.forma.block.place.FormaPlacementResult;
import org.kkaemok.forma.block.place.FormaUseOnContext;
import org.kkaemok.forma.block.state.VisualBlockState;
import org.kkaemok.forma.item.FormaItem;
import org.kkaemok.forma.item.FormaItemBuilder;
import org.kkaemok.forma.util.KeyUtil;
import org.kkaemok.forma.util.TextUtil;

import java.util.ArrayList;
import java.util.List;

public final class FormaBlockListener implements Listener {
    private static final int NOTES_PER_OCTAVE = 12;

    private final Forma plugin;
    private final FormaBlockItemBuilder blockItemBuilder;
    private final FormaItemBuilder itemBuilder;
    private final FormaBlockPlacementService placementService;
    private final FormaBlockSoundPlayer soundPlayer;

    public FormaBlockListener(Forma plugin) {
        this.plugin = plugin;
        this.blockItemBuilder = new FormaBlockItemBuilder(plugin);
        this.itemBuilder = new FormaItemBuilder(plugin);
        this.placementService = new FormaBlockPlacementService(plugin);
        this.soundPlayer = new FormaBlockSoundPlayer(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (customBlocksDisabled() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        EquipmentSlot hand = event.getHand();
        ItemStack itemInHand = event.getItem();
        if (clickedBlock == null || hand == null) {
            return;
        }

        String heldBlockId = getBlockIdFromItem(itemInHand);
        if (heldBlockId != null && itemInHand != null) {
            FormaUseOnContext context = new FormaUseOnContext(
                    event.getPlayer(),
                    hand,
                    itemInHand,
                    clickedBlock,
                    event.getBlockFace(),
                    clickedBlock.getLocation()
            );
            FormaPlacementResult result = placementService.place(heldBlockId, context);
            debugLog("PlayerInteractEvent custom item: id=" + heldBlockId
                    + ", hand=" + hand
                    + ", clicked=" + compact(clickedBlock)
                    + ", face=" + event.getBlockFace()
                    + ", result=" + result);
            if (result.successful()) {
                event.setCancelled(true);
                return;
            }
            sendPlacementFailure(event, heldBlockId, result);
        }

        // NOTE_BLOCK 상태 보호는 설치 pipeline과 별개이며, legacy provider에만 적용한다.
        if (hand != EquipmentSlot.HAND
                || clickedBlock.getType() != Material.NOTE_BLOCK
                || !plugin.getConfig().getBoolean("custom-blocks.prevent-note-block-interaction", true)) {
            return;
        }

        Location location = clickedBlock.getLocation();
        FormaBlock customBlock = getCustomBlockAt(location);
        boolean scheduledRestore = false;
        if (customBlock != null && customBlock.usesNoteBlockProvider()) {
            scheduleRestoreCustomState(location, "PlayerInteractEvent-custom");
            scheduledRestore = true;
        } else if (plugin.getConfig().getBoolean("custom-blocks.prevent-reserved-note-block-states", true)) {
            if (isReservedNoteBlock(clickedBlock)) {
                scheduleResetReservedUnregistered(location, "PlayerInteractEvent-reserved-before");
            } else {
                scheduleResetIfBecomesReserved(location);
            }
            scheduledRestore = true;
        }

        debugLog("PlayerInteractEvent protection: clicked=" + compact(clickedBlock)
                + ", item=" + materialName(itemInHand)
                + ", customBlock=" + (customBlock != null)
                + ", scheduledRestore=" + scheduledRestore);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCustomBlockBreak(BlockBreakEvent event) {
        if (customBlocksDisabled()) {
            return;
        }

        Block block = event.getBlock();
        PlacedFormaBlock placed = plugin.getBlockStorage().getPlacedBlock(block.getLocation());
        if (placed == null) {
            return;
        }

        FormaBlock customBlock = plugin.getBlockManager().getBlock(placed.id());
        List<ItemStack> drops = createDrops(placed.id(), customBlock);
        FormaBlockBreakEvent publicEvent = new FormaBlockBreakEvent(
                event.getPlayer(), placed.id(), block.getLocation(), drops
        );
        plugin.getServer().getPluginManager().callEvent(publicEvent);
        debugLog("API event: FormaBlockBreakEvent, block=" + placed.id()
                + ", cancelled=" + publicEvent.isCancelled()
                + ", dropItems=" + publicEvent.isDropItems()
                + ", drops=" + publicEvent.getDrops().size());
        if (publicEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        if (!plugin.getBlockStorage().tryRemoveBlock(block.getLocation())) {
            event.setCancelled(true);
            plugin.getLogger().warning("[FormaBlock] 저장 데이터를 제거하지 못해 파괴를 취소했습니다: " + placed.id());
            return;
        }

        event.setDropItems(false);
        event.setExpToDrop(0);
        if (publicEvent.isDropItems()) {
            for (ItemStack drop : publicEvent.getDrops()) {
                block.getWorld().dropItemNaturally(block.getLocation(), drop.clone());
            }
        }
        block.setType(Material.AIR, false);
        boolean breakSoundPlayed = customBlock != null
                && plugin.getConfig().getBoolean("custom-blocks.play-break-sound", true)
                && soundPlayer.play(customBlock, SoundAction.BREAK, block.getLocation());
        debugLog("BlockBreakEvent: id=" + placed.id() + ", loc=" + compact(block)
                + ", drops=" + publicEvent.getDrops().size() + ", breakSound=" + breakSoundPlayed);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCustomNotePlay(NotePlayEvent event) {
        if (customBlocksDisabled()
                || !plugin.getConfig().getBoolean("custom-blocks.prevent-note-block-interaction", true)) {
            return;
        }

        Block block = event.getBlock();
        FormaBlock customBlock = getCustomBlockAt(block.getLocation());
        if (customBlock != null && customBlock.usesNoteBlockProvider()) {
            event.setCancelled(true);
            scheduleRestoreCustomState(block.getLocation(), "NotePlayEvent");
            return;
        }

        if (isReservedNoteBlock(block)
                && plugin.getConfig().getBoolean("custom-blocks.prevent-reserved-note-block-states", true)) {
            event.setCancelled(true);
            scheduleResetReservedUnregistered(block.getLocation(), "NotePlayEvent");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCustomBlockPhysics(BlockPhysicsEvent event) {
        if (customBlocksDisabled() || !plugin.getConfig().getBoolean("custom-blocks.prevent-redstone-update", true)) {
            return;
        }

        Block block = event.getBlock();
        FormaBlock customBlock = getCustomBlockAt(block.getLocation());
        if (customBlock != null && customBlock.usesNoteBlockProvider()) {
            Bukkit.getScheduler().runTask(plugin, () -> restoreCustomNoteBlockData(block, "BlockPhysicsEvent"));
            return;
        }

        if (block.getType() == Material.NOTE_BLOCK
                && plugin.getConfig().getBoolean("custom-blocks.reset-unregistered-reserved-note-blocks", true)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (isReservedNoteBlock(block) && getCustomBlockAt(block.getLocation()) == null) {
                    resetToDefaultNoteBlockState(block, "BlockPhysicsEvent");
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCustomBlockRedstone(BlockRedstoneEvent event) {
        if (customBlocksDisabled() || !plugin.getConfig().getBoolean("custom-blocks.prevent-redstone-update", true)) {
            return;
        }

        Block block = event.getBlock();
        FormaBlock customBlock = getCustomBlockAt(block.getLocation());
        if (customBlock != null && customBlock.usesNoteBlockProvider()) {
            event.setNewCurrent(event.getOldCurrent());
            Bukkit.getScheduler().runTask(plugin, () -> restoreCustomNoteBlockData(block, "BlockRedstoneEvent"));
            return;
        }

        if (block.getType() == Material.NOTE_BLOCK
                && plugin.getConfig().getBoolean("custom-blocks.reset-unregistered-reserved-note-blocks", true)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (isReservedNoteBlock(block) && getCustomBlockAt(block.getLocation()) == null) {
                    resetToDefaultNoteBlockState(block, "BlockRedstoneEvent");
                }
            });
        }
    }

    private void sendPlacementFailure(PlayerInteractEvent event, String blockId, FormaPlacementResult result) {
        switch (result) {
            case UNKNOWN_BLOCK -> event.getPlayer().sendMessage(TextUtil.prefixed(
                    "&c등록되지 않은 커스텀 블럭 ID입니다: &f" + blockId));
            case TOO_HIGH -> event.getPlayer().sendMessage(TextUtil.prefixed("&c월드 높이 제한 밖에는 설치할 수 없습니다."));
            case COLLISION -> event.getPlayer().sendMessage(TextUtil.prefixed("&c플레이어나 엔티티와 겹치는 위치에는 설치할 수 없습니다."));
            case CANNOT_REPLACE -> event.getPlayer().sendMessage(TextUtil.prefixed("&c해당 위치에는 블럭을 설치할 수 없습니다."));
            case INVALID_BLOCKDATA -> event.getPlayer().sendMessage(TextUtil.prefixed("&c블럭 표시 상태를 적용할 수 없습니다."));
            case BLOCK_CAN_BUILD_DENIED, ATTEMPT_EVENT_CANCELLED, BLOCK_PLACE_EVENT_CANCELLED,
                 FORMA_PLACE_EVENT_CANCELLED, CANCELLED ->
                    event.getPlayer().sendMessage(TextUtil.prefixed("&c다른 플러그인 또는 이벤트에 의해 설치가 취소되었습니다."));
            case STORAGE_SAVE_FAILED -> event.getPlayer().sendMessage(TextUtil.prefixed(
                    "&c블럭 데이터를 저장하지 못해 설치를 취소했습니다."));
            default -> {
            }
        }
    }

    private List<ItemStack> createDrops(String blockId, FormaBlock customBlock) {
        List<ItemStack> drops = new ArrayList<>();
        if (customBlock != null) {
            for (String dropId : customBlock.drops()) {
                FormaItem itemDrop = plugin.getItemManager().getItem(dropId);
                if (itemDrop != null) {
                    drops.add(itemBuilder.buildWithDebug(itemDrop).itemStack());
                    continue;
                }

                FormaBlock blockDrop = plugin.getBlockManager().getBlock(dropId);
                if (blockDrop != null) {
                    drops.add(blockItemBuilder.build(blockDrop));
                    continue;
                }

                plugin.getLogger().warning("[FormaBlock] 알 수 없는 드롭 ID를 무시합니다: "
                        + dropId + " (block=" + blockId + ")");
            }
        }
        if (drops.isEmpty() && customBlock != null) {
            drops.add(blockItemBuilder.build(customBlock));
        }
        return drops;
    }

    private void scheduleRestoreCustomState(Location location, String reason) {
        Location target = location.clone();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Block block = target.getBlock();
            FormaBlock customBlock = getCustomBlockAt(target);
            if (customBlock == null || !customBlock.usesNoteBlockProvider()
                    || block.getType() != Material.NOTE_BLOCK) {
                return;
            }
            restoreCustomNoteBlockData(block, reason);
            debugLog("NOTE_BLOCK custom state restored: loc=" + compact(block) + ", reason=" + reason);
        }, 1L);
    }

    private void scheduleResetReservedUnregistered(Location location, String reason) {
        Location target = location.clone();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Block block = target.getBlock();
            if (getCustomBlockAt(target) != null
                    || block.getType() != Material.NOTE_BLOCK
                    || !isReservedNoteBlock(block)) {
                return;
            }
            resetToDefaultNoteBlockState(block, reason);
            debugLog("Unregistered reserved NOTE_BLOCK reset: loc=" + compact(block) + ", reason=" + reason);
        }, 1L);
    }

    private void scheduleResetIfBecomesReserved(Location location) {
        Location target = location.clone();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Block block = target.getBlock();
            if (getCustomBlockAt(target) != null
                    || block.getType() != Material.NOTE_BLOCK
                    || !isReservedNoteBlock(block)) {
                return;
            }
            resetToDefaultNoteBlockState(block, "PlayerInteractEvent-post-check");
            debugLog("Reserved NOTE_BLOCK post-check reset: loc=" + compact(block));
        }, 1L);
    }

    private boolean customBlocksDisabled() {
        return !plugin.getConfig().getBoolean("custom-blocks.enabled", true);
    }

    private String getBlockIdFromItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return KeyUtil.readBlockId(plugin, meta.getPersistentDataContainer());
    }

    private FormaBlock getCustomBlockAt(Location location) {
        String id = plugin.getBlockStorage().getBlockId(location);
        return id == null ? null : plugin.getBlockManager().getBlock(id);
    }

    private void restoreCustomNoteBlockData(Block block, String reason) {
        FormaBlock customBlock = getCustomBlockAt(block.getLocation());
        if (customBlock == null || !customBlock.usesNoteBlockProvider()) {
            return;
        }

        if (applyVisualState(block, customBlock.visualState())) {
            debugLog("Custom NOTE_BLOCK state restored (" + reason + "): id="
                    + customBlock.id() + ", loc=" + compact(block));
        }
    }

    private void resetToDefaultNoteBlockState(Block block, String reason) {
        if (block.getType() != Material.NOTE_BLOCK || getCustomBlockAt(block.getLocation()) != null) {
            return;
        }
        String ownerBeforeReset = reservedOwner(block);
        NoteBlock data = (NoteBlock) Bukkit.createBlockData(Material.NOTE_BLOCK);
        data.setInstrument(org.bukkit.Instrument.PIANO);
        data.setNote(new org.bukkit.Note(0));
        data.setPowered(false);
        block.setBlockData(data, false);
        debugLog("Reserved NOTE_BLOCK reset (" + reason + "): loc=" + compact(block)
                + ", previousOwner=" + ownerBeforeReset);
    }

    private boolean isReservedNoteBlock(Block block) {
        if (block.getType() != Material.NOTE_BLOCK || !(block.getBlockData() instanceof NoteBlock noteBlock)) {
            return false;
        }

        return plugin.getBlockManager().isReservedNoteBlockState(
                noteBlock.getInstrument(),
                noteValue(noteBlock.getNote()),
                noteBlock.isPowered()
        );
    }

    private String reservedOwner(Block block) {
        if (block.getType() != Material.NOTE_BLOCK || !(block.getBlockData() instanceof NoteBlock noteBlock)) {
            return "(none)";
        }
        return plugin.getBlockManager()
                .getReservedStateOwner(noteBlock.getInstrument(), noteValue(noteBlock.getNote()), noteBlock.isPowered())
                .orElse("(none)");
    }

    private int noteValue(org.bukkit.Note note) {
        int toneValue = switch (note.getTone()) {
            case G -> 1;
            case A -> 3;
            case B -> 5;
            case C -> 6;
            case D -> 8;
            case E -> 10;
            case F -> 11;
        };
        if (note.isSharped()) {
            toneValue = (toneValue + 1) % NOTES_PER_OCTAVE;
        }
        return note.getOctave() * NOTES_PER_OCTAVE + toneValue;
    }

    private boolean applyVisualState(Block block, VisualBlockState visualState) {
        try {
            BlockData data = Bukkit.createBlockData(visualState.asString());
            block.setBlockData(data, false);
            return true;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            plugin.getLogger().warning("[FormaBlock] 블럭 상태를 복구하지 못했습니다: " + visualState.asString());
            return false;
        }
    }

    private String materialName(ItemStack item) {
        return item == null ? "AIR" : item.getType().name();
    }

    private void debugLog(String message) {
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }

    private String compact(Block block) {
        return block.getWorld().getName() + " "
                + block.getX() + "," + block.getY() + "," + block.getZ();
    }
}
