package org.kkaemok.forma.block.place;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.util.BoundingBox;
import org.kkaemok.forma.Forma;

import java.util.EnumSet;
import java.util.Set;

public final class FormaPlacementChecks {
    private static final Set<Material> REPLACEABLE_MATERIALS = EnumSet.of(
            Material.SHORT_GRASS,
            Material.TALL_GRASS,
            Material.FERN,
            Material.LARGE_FERN,
            Material.SNOW,
            Material.VINE,
            Material.GLOW_LICHEN
    );

    private final Forma plugin;

    public FormaPlacementChecks(Forma plugin) {
        this.plugin = plugin;
    }

    public boolean isReplaceable(Block block) {
        Material material = block.getType();
        if (material.isAir() || material == Material.WATER || material == Material.LAVA) {
            return true;
        }
        return plugin.getConfig().getBoolean("custom-blocks.allow-replaceable-placement", true)
                && REPLACEABLE_MATERIALS.contains(material);
    }

    public boolean isWithinWorldHeight(Block block) {
        return block.getY() >= block.getWorld().getMinHeight()
                && block.getY() < block.getWorld().getMaxHeight();
    }

    public boolean hasEntityCollision(Player player, Block target, BlockData data) {
        if (!data.getMaterial().isSolid()) {
            return false;
        }

        BoundingBox bounds = BoundingBox.of(target);
        return target.getWorld().getNearbyEntities(bounds).stream().anyMatch(entity -> {
            if (!entity.getBoundingBox().overlaps(bounds)) {
                return false;
            }
            if (entity instanceof LivingEntity livingEntity) {
                return livingEntity.isCollidable();
            }
            return entity instanceof Vehicle;
        });
    }

    public boolean canPlace(Block target, BlockData data) {
        if (!isReplaceable(target)) {
            return false;
        }
        if (target.getType() == Material.WATER || target.getType() == Material.LAVA) {
            return true;
        }
        return target.canPlace(data);
    }

    public BlockCanBuildResult callBlockCanBuildEvent(FormaBlockPlaceContext context, boolean defaultCanBuild) {
        FormaUseOnContext useContext = context.useContext();
        BlockCanBuildEvent event = new BlockCanBuildEvent(
                context.targetBlock(),
                useContext.player(),
                context.blockDataToPlace(),
                defaultCanBuild,
                useContext.hand()
        );
        plugin.getServer().getPluginManager().callEvent(event);
        return new BlockCanBuildResult(defaultCanBuild, event.isBuildable());
    }

    public record BlockCanBuildResult(boolean defaultCanBuild, boolean allowed) {
    }
}
