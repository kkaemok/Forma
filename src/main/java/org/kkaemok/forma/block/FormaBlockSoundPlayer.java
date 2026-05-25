package org.kkaemok.forma.block;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.SoundGroup;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.kkaemok.forma.Forma;
import org.kkaemok.forma.block.FormaBlockSounds.SoundAction;

import java.util.Locale;

public final class FormaBlockSoundPlayer {
    private final Forma plugin;

    public FormaBlockSoundPlayer(Forma plugin) {
        this.plugin = plugin;
    }

    public boolean play(FormaBlock block, SoundAction action, Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        SoundPlayback playback = resolve(block, action);
        try {
            if (playback.sound() != null) {
                world.playSound(location, playback.sound(), block.sounds().volume(), block.sounds().pitch());
            } else {
                world.playSound(location, playback.key(), block.sounds().volume(), block.sounds().pitch());
            }
            debug("block id=" + block.id()
                    + ", sound action=" + action
                    + ", sound key=" + playback.key()
                    + ", source=" + playback.source());
            return true;
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("[FormaBlock] 사운드를 재생할 수 없습니다: "
                    + playback.key() + " (block=" + block.id() + ")");
            return false;
        }
    }

    private SoundPlayback resolve(FormaBlock block, SoundAction action) {
        String configured = block.sounds().get(action);
        if (configured != null && !configured.isBlank()) {
            String key = configured.trim();
            if (key.contains(":")) {
                return new SoundPlayback(null, key, "settings.sounds");
            }
            Sound sound = Registry.SOUND_EVENT.get(NamespacedKey.minecraft(
                    key.toLowerCase(Locale.ROOT).replace('_', '.')
            ));
            if (sound != null) {
                return new SoundPlayback(sound, keyFor(sound), "settings.sounds");
            }
            plugin.getLogger().warning("[FormaBlock] 잘못된 사운드 이름입니다. 기본 사운드를 사용합니다: "
                    + key + " (block=" + block.id() + ")");
        }

        Sound providerSound = providerSound(block, action);
        if (providerSound != null) {
            return new SoundPlayback(providerSound, keyFor(providerSound), "provider");
        }

        Sound fallback = fallbackSound(block, action);
        return new SoundPlayback(fallback, keyFor(fallback), "fallback");
    }

    private Sound providerSound(FormaBlock block, SoundAction action) {
        try {
            BlockData data = Bukkit.createBlockData(block.visualState().asString());
            SoundGroup soundGroup = data.getSoundGroup();
            return switch (action) {
                case PLACE -> soundGroup.getPlaceSound();
                case BREAK -> soundGroup.getBreakSound();
                case STEP -> soundGroup.getStepSound();
                case HIT -> soundGroup.getHitSound();
                case FALL -> soundGroup.getFallSound();
            };
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Sound fallbackSound(FormaBlock block, SoundAction action) {
        if (block.usesNoteBlockProvider()) {
            return woodSound(action);
        }
        if (block.providerType() == org.kkaemok.forma.block.state.VisualStateProviderType.SOLID
                || block.providerType() == org.kkaemok.forma.block.state.VisualStateProviderType.BROWN_MUSHROOM_BLOCK) {
            return stoneSound(action);
        }
        return grassSound(action);
    }

    private Sound woodSound(SoundAction action) {
        return switch (action) {
            case PLACE -> Sound.BLOCK_WOOD_PLACE;
            case BREAK -> Sound.BLOCK_WOOD_BREAK;
            case STEP -> Sound.BLOCK_WOOD_STEP;
            case HIT -> Sound.BLOCK_WOOD_HIT;
            case FALL -> Sound.BLOCK_WOOD_FALL;
        };
    }

    private Sound stoneSound(SoundAction action) {
        return switch (action) {
            case PLACE -> Sound.BLOCK_STONE_PLACE;
            case BREAK -> Sound.BLOCK_STONE_BREAK;
            case STEP -> Sound.BLOCK_STONE_STEP;
            case HIT -> Sound.BLOCK_STONE_HIT;
            case FALL -> Sound.BLOCK_STONE_FALL;
        };
    }

    private Sound grassSound(SoundAction action) {
        return switch (action) {
            case PLACE -> Sound.BLOCK_GRASS_PLACE;
            case BREAK -> Sound.BLOCK_GRASS_BREAK;
            case STEP -> Sound.BLOCK_GRASS_STEP;
            case HIT -> Sound.BLOCK_GRASS_HIT;
            case FALL -> Sound.BLOCK_GRASS_FALL;
        };
    }

    private String keyFor(Sound sound) {
        NamespacedKey key = Registry.SOUND_EVENT.getKey(sound);
        return key == null ? sound.toString() : key.toString();
    }

    private void debug(String message) {
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[DEBUG] Sound: " + message);
        }
    }

    private record SoundPlayback(Sound sound, String key, String source) {
    }
}
