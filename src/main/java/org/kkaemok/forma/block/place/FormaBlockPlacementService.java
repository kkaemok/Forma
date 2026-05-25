package org.kkaemok.forma.block.place;

import org.bukkit.Bukkit;
import org.bukkit.GameEvent;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.kkaemok.forma.Forma;
import org.kkaemok.forma.block.FormaBlock;
import org.kkaemok.forma.block.FormaBlockSoundPlayer;
import org.kkaemok.forma.block.FormaBlockSounds.SoundAction;

public final class FormaBlockPlacementService {
    private final Forma plugin;
    private final FormaPlacementChecks checks;
    private final FormaBlockSoundPlayer soundPlayer;

    public FormaBlockPlacementService(Forma plugin) {
        this.plugin = plugin;
        this.checks = new FormaPlacementChecks(plugin);
        this.soundPlayer = new FormaBlockSoundPlayer(plugin);
    }

    public FormaPlacementResult place(String blockId, FormaUseOnContext useContext) {
        FormaBlock block = plugin.getBlockManager().getBlock(blockId);
        if (block == null) {
            plugin.getLogger().warning("[FormaBlock] 존재하지 않는 블럭 ID가 아이템에 저장되어 있습니다: " + blockId);
            debug("block id=" + blockId + ", result=UNKNOWN_BLOCK");
            return FormaPlacementResult.UNKNOWN_BLOCK;
        }
        if (useContext.clickedFace() == BlockFace.SELF) {
            debug("block id=" + blockId + ", result=FAIL, reason=invalid clicked face");
            return FormaPlacementResult.FAIL;
        }

        BlockData blockData;
        try {
            blockData = Bukkit.createBlockData(block.visualState().asString());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("[FormaBlock] 적용할 수 없는 visual state입니다: "
                    + block.id() + " -> " + block.visualState().asString());
            debug("block id=" + blockId + ", result=INVALID_BLOCKDATA");
            return FormaPlacementResult.INVALID_BLOCKDATA;
        }

        Block againstBlock = useContext.clickedBlock();
        boolean replaceClicked = checks.isReplaceable(againstBlock);
        Block targetBlock = replaceClicked
                ? againstBlock
                : againstBlock.getRelative(useContext.clickedFace());
        FormaBlockPlaceContext context = new FormaBlockPlaceContext(
                useContext,
                block,
                againstBlock,
                targetBlock,
                replaceClicked,
                block.visualState(),
                blockData
        );
        FormaPlacementPlan plan = FormaPlacementPlan.single(context);

        debugContext(blockId, context);
        if (!checks.isWithinWorldHeight(targetBlock)) {
            debugResult(FormaPlacementResult.TOO_HIGH, false, false, false, false, false, false);
            return FormaPlacementResult.TOO_HIGH;
        }
        if (!checks.isReplaceable(targetBlock) || !checks.canPlace(targetBlock, blockData)) {
            debugResult(FormaPlacementResult.CANNOT_REPLACE, false, false, false, false, false, false);
            return FormaPlacementResult.CANNOT_REPLACE;
        }
        if (checks.hasEntityCollision(useContext.player(), targetBlock, blockData)) {
            debugResult(FormaPlacementResult.COLLISION, false, false, false, false, false, false);
            return FormaPlacementResult.COLLISION;
        }

        boolean defaultCanBuild = true;
        FormaPlacementChecks.BlockCanBuildResult canBuildResult =
                checks.callBlockCanBuildEvent(context, defaultCanBuild);
        debug("defaultCanBuild=" + canBuildResult.defaultCanBuild()
                + ", BlockCanBuildEvent result=" + canBuildResult.allowed());
        if (!canBuildResult.allowed()) {
            debugResult(FormaPlacementResult.BLOCK_CAN_BUILD_DENIED, false, false, false, false, false, false);
            return FormaPlacementResult.BLOCK_CAN_BUILD_DENIED;
        }

        FormaBlockAttemptPlaceEvent attemptEvent = new FormaBlockAttemptPlaceEvent(
                useContext.player(), context, plan
        );
        plugin.getServer().getPluginManager().callEvent(attemptEvent);
        debug("FormaBlockAttemptPlaceEvent result=" + !attemptEvent.isCancelled());
        if (attemptEvent.isCancelled()) {
            debugResult(FormaPlacementResult.ATTEMPT_EVENT_CANCELLED, false, false, false, false, false, false);
            return FormaPlacementResult.ATTEMPT_EVENT_CANCELLED;
        }

        FormaPlacementRollback rollback = new FormaPlacementRollback();
        for (FormaPlacementPlan.BlockPlacementEntry entry : plan.entries()) {
            rollback.add(entry.block());
        }

        try {
            for (FormaPlacementPlan.BlockPlacementEntry entry : plan.entries()) {
                entry.block().setBlockData(entry.blockData(), false);
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            rollback.rollback();
            debugResult(FormaPlacementResult.FAIL, true, false, false, false, false, false);
            return FormaPlacementResult.FAIL;
        }

        if (plugin.getConfig().getBoolean("custom-blocks.call-bukkit-block-place-event", true)) {
            BlockPlaceEvent bukkitEvent = new BlockPlaceEvent(
                    targetBlock,
                    rollback.previousPrimaryState(),
                    againstBlock,
                    useContext.item().clone(),
                    useContext.player(),
                    true,
                    useContext.hand()
            );
            plugin.getServer().getPluginManager().callEvent(bukkitEvent);
            boolean bukkitAllowed = !bukkitEvent.isCancelled() && bukkitEvent.canBuild();
            debug("Bukkit BlockPlaceEvent result=" + bukkitAllowed);
            if (!bukkitAllowed) {
                rollback.rollback();
                debugResult(FormaPlacementResult.BLOCK_PLACE_EVENT_CANCELLED, true, false, false, false, false, false);
                return FormaPlacementResult.BLOCK_PLACE_EVENT_CANCELLED;
            }
        } else {
            debug("Bukkit BlockPlaceEvent result=skipped (config disabled)");
        }

        FormaBlockPlaceEvent formaEvent = new FormaBlockPlaceEvent(
                useContext.player(), context, plan, targetBlock
        );
        plugin.getServer().getPluginManager().callEvent(formaEvent);
        debug("FormaBlockPlaceEvent result=" + !formaEvent.isCancelled());
        if (formaEvent.isCancelled()) {
            rollback.rollback();
            debugResult(FormaPlacementResult.FORMA_PLACE_EVENT_CANCELLED, true, false, false, false, false, false);
            return FormaPlacementResult.FORMA_PLACE_EVENT_CANCELLED;
        }

        org.kkaemok.forma.api.event.FormaBlockPlaceEvent publicEvent =
                new org.kkaemok.forma.api.event.FormaBlockPlaceEvent(
                        useContext.player(),
                        block.id(),
                        targetBlock.getLocation(),
                        useContext.item().clone()
                );
        plugin.getServer().getPluginManager().callEvent(publicEvent);
        debug("API FormaBlockPlaceEvent result=" + !publicEvent.isCancelled());
        if (publicEvent.isCancelled()) {
            rollback.rollback();
            debugResult(FormaPlacementResult.FORMA_PLACE_EVENT_CANCELLED, true, false, false, false, false, false);
            return FormaPlacementResult.FORMA_PLACE_EVENT_CANCELLED;
        }

        if (!plugin.getBlockStorage().trySetBlock(targetBlock.getLocation(), block.id(), block.type())) {
            rollback.rollback();
            plugin.getLogger().warning("[FormaBlock] 블럭 위치 데이터를 저장할 수 없어 설치를 되돌렸습니다: "
                    + block.id());
            debugResult(FormaPlacementResult.STORAGE_SAVE_FAILED, true, false, false, false, false, false);
            return FormaPlacementResult.STORAGE_SAVE_FAILED;
        }
        boolean consumed = consumeOneItem(useContext);
        boolean swung = swingHand(useContext);
        boolean soundPlayed = playPlaceSound(context);
        boolean gameEventSent = sendGameEvent(context);

        debugResult(FormaPlacementResult.SUCCESS, false, true, consumed, soundPlayed, swung, gameEventSent);
        return FormaPlacementResult.SUCCESS;
    }

    private boolean consumeOneItem(FormaUseOnContext context) {
        PlayerInventoryAccess inventory = new PlayerInventoryAccess(context);
        GameMode gameMode = context.player().getGameMode();
        if (gameMode != GameMode.SURVIVAL && gameMode != GameMode.ADVENTURE) {
            return false;
        }

        ItemStack held = inventory.currentItem();
        if (held == null || held.isEmpty()) {
            return false;
        }

        if (held.getAmount() <= 1) {
            inventory.setItem(ItemStack.empty());
        } else {
            held.setAmount(held.getAmount() - 1);
            inventory.setItem(held);
        }
        return true;
    }

    private boolean swingHand(FormaUseOnContext context) {
        if (!plugin.getConfig().getBoolean("custom-blocks.swing-hand-on-place", true)) {
            return false;
        }
        try {
            context.player().swingHand(context.hand());
            return true;
        } catch (RuntimeException ex) {
            debug("swing failed=" + ex.getClass().getSimpleName());
            return false;
        }
    }

    private boolean playPlaceSound(FormaBlockPlaceContext context) {
        if (!plugin.getConfig().getBoolean("custom-blocks.play-place-sound", true)) {
            return false;
        }
        return soundPlayer.play(context.formaBlock(), SoundAction.PLACE, context.targetBlock().getLocation());
    }

    private boolean sendGameEvent(FormaBlockPlaceContext context) {
        if (!plugin.getConfig().getBoolean("custom-blocks.send-game-event", true)) {
            return false;
        }
        try {
            context.targetBlock().getWorld().sendGameEvent(
                    context.useContext().player(),
                    GameEvent.BLOCK_PLACE,
                    context.targetBlock().getLocation().toVector()
            );
            return true;
        } catch (RuntimeException ex) {
            debug("gameEvent failed=" + ex.getClass().getSimpleName());
            return false;
        }
    }

    private void debugContext(String blockId, FormaBlockPlaceContext context) {
        debug("block id=" + blockId
                + ", clicked block=" + formatBlock(context.againstBlock())
                + ", clicked face=" + context.useContext().clickedFace()
                + ", replaceClicked=" + context.replaceClicked()
                + ", target block=" + formatBlock(context.targetBlock())
                + ", visual state=" + context.visualState().asString()
                + ", blockDataToPlace=" + context.blockDataToPlace().getAsString());
    }

    private void debugResult(
            FormaPlacementResult result,
            boolean rollback,
            boolean storageSaved,
            boolean consumed,
            boolean soundPlayed,
            boolean swung,
            boolean gameEventSent
    ) {
        debug("placement result=" + result
                + ", rollback=" + rollback
                + ", storage saved=" + storageSaved
                + ", item consumed=" + consumed
                + ", sound=" + soundPlayed
                + ", swing=" + swung
                + ", gameEvent=" + gameEventSent);
    }

    private String formatBlock(Block block) {
        return block.getType() + "@" + block.getWorld().getName() + ":"
                + block.getX() + "," + block.getY() + "," + block.getZ();
    }

    private void debug(String message) {
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[DEBUG] PlacePipeline: " + message);
        }
    }

    private static final class PlayerInventoryAccess {
        private final FormaUseOnContext context;

        private PlayerInventoryAccess(FormaUseOnContext context) {
            this.context = context;
        }

        private ItemStack currentItem() {
            if (context.hand() == EquipmentSlot.OFF_HAND) {
                return context.player().getInventory().getItemInOffHand();
            }
            return context.player().getInventory().getItemInMainHand();
        }

        private void setItem(ItemStack item) {
            if (context.hand() == EquipmentSlot.OFF_HAND) {
                context.player().getInventory().setItemInOffHand(item);
            } else {
                context.player().getInventory().setItemInMainHand(item);
            }
        }
    }
}
