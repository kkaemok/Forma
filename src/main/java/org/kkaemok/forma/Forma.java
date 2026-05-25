package org.kkaemok.forma;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.kkaemok.forma.api.FormaAPI;
import org.kkaemok.forma.block.FormaBlockListener;
import org.kkaemok.forma.block.FormaBlockManager;
import org.kkaemok.forma.block.FormaBlockStorage;
import org.kkaemok.forma.block.state.VisualBlockStateManager;
import org.kkaemok.forma.command.FormaCommand;
import org.kkaemok.forma.item.FormaItemManager;
import org.kkaemok.forma.item.FormaItemListener;
import org.kkaemok.forma.pack.ResourcePackManager;
import org.kkaemok.forma.recipe.FormaRecipeListener;
import org.kkaemok.forma.recipe.FormaRecipeManager;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public final class Forma extends JavaPlugin {
    private FormaItemManager itemManager;
    private FormaBlockManager blockManager;
    private FormaBlockStorage blockStorage;
    private VisualBlockStateManager visualBlockStateManager;
    private ResourcePackManager resourcePackManager;
    private FormaRecipeManager recipeManager;
    private FormaAPI api;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureItemsFile();
        ensureBlocksFile();

        itemManager = new FormaItemManager(this);
        itemManager.loadItems();

        visualBlockStateManager = new VisualBlockStateManager(this);
        visualBlockStateManager.loadCache();

        blockManager = new FormaBlockManager(this);
        blockManager.loadBlocks();

        blockStorage = new FormaBlockStorage(this);
        blockStorage.load();
        blockStorage.validateLoadedBlocks();
        if (getConfig().getBoolean("debug", false)) {
            int placedBlockCount = blockStorage.getAllBlocks().values().stream().mapToInt(Map::size).sum();
            getLogger().info("[DEBUG] blocks-data loaded entries=" + placedBlockCount);
        }

        resourcePackManager = new ResourcePackManager(this);
        resourcePackManager.ensureDirectories();

        recipeManager = new FormaRecipeManager(this);
        recipeManager.reload();

        api = FormaAPI.initialize(this);
        getServer().getServicesManager().register(FormaAPI.class, api, this, ServicePriority.Normal);

        getServer().getPluginManager().registerEvents(new FormaBlockListener(this), this);
        getServer().getPluginManager().registerEvents(new FormaItemListener(this), this);
        getServer().getPluginManager().registerEvents(new FormaRecipeListener(this), this);

        PluginCommand command = getCommand("forma");
        if (command == null) {
            getLogger().severe("Failed to register /forma command. Check plugin.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        FormaCommand formaCommand = new FormaCommand(this);
        command.setExecutor(formaCommand);
        command.setTabCompleter(formaCommand);

        getLogger().info("Forma enabled: items=" + itemManager.getItems().size()
                + ", blocks=" + blockManager.getBlocks().size());
    }

    @Override
    public void onDisable() {
        if (api != null) {
            getServer().getServicesManager().unregister(FormaAPI.class, api);
            FormaAPI.shutdown(api);
        }
        if (blockStorage != null) {
            blockStorage.save();
        }
    }

    public FormaItemManager getItemManager() {
        return Objects.requireNonNull(itemManager, "ItemManager has not been initialized.");
    }

    public FormaBlockManager getBlockManager() {
        return Objects.requireNonNull(blockManager, "BlockManager has not been initialized.");
    }

    public FormaBlockStorage getBlockStorage() {
        return Objects.requireNonNull(blockStorage, "BlockStorage has not been initialized.");
    }

    public VisualBlockStateManager getVisualBlockStateManager() {
        return Objects.requireNonNull(visualBlockStateManager, "VisualBlockStateManager has not been initialized.");
    }

    public ResourcePackManager getResourcePackManager() {
        return Objects.requireNonNull(resourcePackManager, "ResourcePackManager has not been initialized.");
    }

    public FormaRecipeManager getRecipeManager() {
        return Objects.requireNonNull(recipeManager, "RecipeManager has not been initialized.");
    }

    public void reloadAll() {
        reloadConfig();
        ensureItemsFile();
        ensureBlocksFile();
        getVisualBlockStateManager().loadCache();
        getItemManager().reload();
        getBlockManager().reload();
        getBlockStorage().validateLoadedBlocks();
        getRecipeManager().reload();
        getResourcePackManager().ensureDirectories();
    }

    private void ensureItemsFile() {
        File folder = getDataFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            getLogger().warning("Failed to create plugin data folder: " + folder.getAbsolutePath());
        }

        File itemsFile = new File(folder, "items.yml");
        if (!itemsFile.exists()) {
            saveResource("items.yml", false);
        }
    }

    private void ensureBlocksFile() {
        File folder = getDataFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            getLogger().warning("Failed to create plugin data folder: " + folder.getAbsolutePath());
        }

        File blocksFile = new File(folder, "blocks.yml");
        if (!blocksFile.exists()) {
            saveResource("blocks.yml", false);
        }
    }
}
