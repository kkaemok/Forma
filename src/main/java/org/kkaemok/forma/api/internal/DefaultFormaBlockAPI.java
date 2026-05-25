package org.kkaemok.forma.api.internal;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.kkaemok.forma.Forma;
import org.kkaemok.forma.api.block.FormaBlockAPI;
import org.kkaemok.forma.api.block.FormaBlockData;
import org.kkaemok.forma.api.block.PlacedFormaBlockData;
import org.kkaemok.forma.block.FormaBlock;
import org.kkaemok.forma.block.FormaBlockItemBuilder;
import org.kkaemok.forma.block.PlacedFormaBlock;
import org.kkaemok.forma.item.FormaItem;
import org.kkaemok.forma.item.FormaItemBuilder;
import org.kkaemok.forma.util.KeyUtil;
import org.kkaemok.forma.util.TextUtil;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class DefaultFormaBlockAPI implements FormaBlockAPI {
    private final Forma plugin;
    private final FormaBlockItemBuilder blockItemBuilder;
    private final FormaItemBuilder itemBuilder;

    public DefaultFormaBlockAPI(Forma plugin) {
        this.plugin = plugin;
        this.blockItemBuilder = new FormaBlockItemBuilder(plugin);
        this.itemBuilder = new FormaItemBuilder(plugin);
    }

    @Override
    public Optional<FormaBlockData> getBlock(String id) {
        requireActive();
        FormaBlock block = plugin.getBlockManager().getBlock(id);
        return block == null ? Optional.empty() : Optional.of(toData(block));
    }

    @Override
    public boolean hasBlock(String id) {
        requireActive();
        return plugin.getBlockManager().getBlock(id) != null;
    }

    @Override
    public Set<String> getBlockIds() {
        requireActive();
        return Set.copyOf(plugin.getBlockManager().getIds());
    }

    @Override
    public ItemStack createBlockItem(String id) {
        return createBlockItem(id, 1);
    }

    @Override
    public ItemStack createBlockItem(String id, int amount) {
        requireActive();
        ItemStack stack = blockItemBuilder.build(requireBlock(id)).clone();
        if (amount < 1 || amount > stack.getMaxStackSize()) {
            throw new IllegalArgumentException("블럭 아이템 수량은 1~" + stack.getMaxStackSize() + " 범위여야 합니다.");
        }
        stack.setAmount(amount);
        return stack;
    }

    @Override
    public Optional<String> getBlockId(ItemStack item) {
        requireActive();
        if (item == null || item.getType() == Material.AIR) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        return meta == null
                ? Optional.empty()
                : Optional.ofNullable(KeyUtil.readBlockId(plugin, meta.getPersistentDataContainer()));
    }

    @Override
    public boolean isFormaBlockItem(ItemStack item) {
        return getBlockId(item).isPresent();
    }

    @Override
    public Optional<PlacedFormaBlockData> getPlacedBlock(Location location) {
        requireActive();
        PlacedFormaBlock placed = plugin.getBlockStorage().getPlacedBlock(location);
        World world = location.getWorld();
        return placed == null || world == null
                ? Optional.empty()
                : Optional.of(toPlacedData(placed, world));
    }

    @Override
    public boolean isFormaBlock(Location location) {
        requireActive();
        return plugin.getBlockStorage().isCustomBlock(location);
    }

    @Override
    public boolean removePlacedBlock(Location location, boolean drop) {
        requireActive();
        PlacedFormaBlock placed = plugin.getBlockStorage().getPlacedBlock(location);
        if (placed == null) {
            return false;
        }
        FormaBlock block = plugin.getBlockManager().getBlock(placed.id());
        if (!plugin.getBlockStorage().tryRemoveBlock(location)) {
            return false;
        }
        Block worldBlock = location.getBlock();
        worldBlock.setType(Material.AIR, false);
        if (drop && block != null) {
            for (ItemStack stack : createDrops(block)) {
                worldBlock.getWorld().dropItemNaturally(worldBlock.getLocation(), stack);
            }
        }
        return true;
    }

    @Override
    public boolean setPlacedBlock(Location location, String blockId) {
        return forceSetPlacedBlock(location, blockId);
    }

    @Override
    public boolean forceSetPlacedBlock(Location location, String blockId) {
        requireActive();
        FormaBlock block = plugin.getBlockManager().getBlock(blockId);
        if (block == null || location.getWorld() == null) {
            return false;
        }

        Block target = location.getBlock();
        BlockState previous = target.getState(true);
        try {
            BlockData data = Bukkit.createBlockData(block.visualState().asString());
            target.setBlockData(data, false);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return false;
        }
        if (!plugin.getBlockStorage().trySetBlock(location, block.id(), block.type())) {
            previous.update(true, false);
            return false;
        }
        return true;
    }

    @Override
    public Set<PlacedFormaBlockData> getPlacedBlocks(World world) {
        requireActive();
        if (world == null) {
            return Set.of();
        }
        Map<String, PlacedFormaBlock> worldBlocks = plugin.getBlockStorage().getAllBlocks().get(world.getName());
        if (worldBlocks == null) {
            return Set.of();
        }
        Set<PlacedFormaBlockData> result = new LinkedHashSet<>();
        for (PlacedFormaBlock placed : worldBlocks.values()) {
            result.add(toPlacedData(placed, world));
        }
        return Set.copyOf(result);
    }

    private FormaBlock requireBlock(String id) {
        FormaBlock block = plugin.getBlockManager().getBlock(id);
        if (block == null) {
            throw new IllegalArgumentException("등록되지 않은 Forma 블럭 ID입니다: " + id);
        }
        return block;
    }

    private FormaBlockData toData(FormaBlock block) {
        return new FormaBlockData(
                block.id(),
                block.providerType().name(),
                block.visualState().asString(),
                block.placedMaterial(),
                block.name() == null ? Component.empty() : TextUtil.parse(block.name()),
                block.blockModel(),
                block.hardness(),
                block.tool(),
                block.drops()
        );
    }

    private PlacedFormaBlockData toPlacedData(PlacedFormaBlock placed, World world) {
        FormaBlock block = plugin.getBlockManager().getBlock(placed.id());
        String visualState = block == null ? "" : block.visualState().asString();
        return new PlacedFormaBlockData(
                placed.id(),
                world,
                placed.x(),
                placed.y(),
                placed.z(),
                new Location(world, placed.x(), placed.y(), placed.z()),
                visualState
        );
    }

    private List<ItemStack> createDrops(FormaBlock block) {
        List<ItemStack> drops = new ArrayList<>();
        for (String dropId : block.drops()) {
            FormaItem itemDrop = plugin.getItemManager().getItem(dropId);
            if (itemDrop != null) {
                drops.add(itemBuilder.buildWithDebug(itemDrop).itemStack());
                continue;
            }
            FormaBlock blockDrop = plugin.getBlockManager().getBlock(dropId);
            if (blockDrop != null) {
                drops.add(blockItemBuilder.build(blockDrop));
            }
        }
        if (drops.isEmpty()) {
            drops.add(blockItemBuilder.build(block));
        }
        return drops;
    }

    private void requireActive() {
        if (!plugin.isEnabled()) {
            throw new IllegalStateException("Forma API is not active.");
        }
    }
}
