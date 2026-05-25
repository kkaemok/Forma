package org.kkaemok.forma.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.kkaemok.forma.Forma;
import org.kkaemok.forma.api.event.FormaItemGiveEvent;
import org.kkaemok.forma.block.FormaBlock;
import org.kkaemok.forma.block.FormaBlockItemBuilder;
import org.kkaemok.forma.item.FormaItem;
import org.kkaemok.forma.item.FormaItemBuilder;
import org.kkaemok.forma.pack.ResourcePackGenerator;
import org.kkaemok.forma.util.NoteBlockInstrumentUtil;
import org.kkaemok.forma.util.TextUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

public final class FormaCommand implements CommandExecutor, TabCompleter {
    private static final String ADMIN_PERMISSION = "forma.op";

    private final Forma plugin;
    private final FormaItemBuilder itemBuilder;
    private final FormaBlockItemBuilder blockItemBuilder;

    public FormaCommand(Forma plugin) {
        this.plugin = plugin;
        this.itemBuilder = new FormaItemBuilder(plugin);
        this.blockItemBuilder = new FormaBlockItemBuilder(plugin);
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(TextUtil.prefixed("&c권한이 없습니다. (&f" + ADMIN_PERMISSION + "&c)"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> handleGive(sender, args);
            case "get" -> handleGet(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            case "pack" -> handlePack(sender, args);
            case "block" -> handleBlock(sender, args);
            case "recipe" -> handleRecipe(sender, args);
            default -> sendHelp(sender, label);
        }
        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(TextUtil.prefixed("&c사용법: /forma give <player> <id> [amount]"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(TextUtil.prefixed("&c플레이어를 찾을 수 없습니다: &f" + args[1]));
            return;
        }

        FormaItem item = plugin.getItemManager().getItem(args[2]);
        if (item == null) {
            sender.sendMessage(TextUtil.prefixed("&c등록되지 않은 아이템 ID입니다: &f" + args[2]));
            return;
        }

        Integer amount = parseAmount(args.length >= 4 ? args[3] : "1");
        if (amount == null) {
            sender.sendMessage(TextUtil.prefixed("&c수량은 1~64 범위의 정수여야 합니다."));
            return;
        }

        FormaItemBuilder.BuildResult buildResult = itemBuilder.buildWithDebug(item);
        logGiveDebug(buildResult.debugInfo());
        FormaItemGiveEvent giveEvent = new FormaItemGiveEvent(target, item.id(), buildResult.itemStack().clone());
        plugin.getServer().getPluginManager().callEvent(giveEvent);
        debugApiEvent("FormaItemGiveEvent", item.id(), giveEvent.isCancelled());
        if (giveEvent.isCancelled()) {
            sender.sendMessage(TextUtil.prefixed("&e아이템 지급이 이벤트에 의해 취소되었습니다."));
            return;
        }
        giveItem(target, giveEvent.getItemStack(), amount);

        sender.sendMessage(TextUtil.prefixed("&a지급 완료: &f" + target.getName() + "&a 에게 &f" + item.id() + " x" + amount));
        if (!sender.getName().equalsIgnoreCase(target.getName())) {
            target.sendMessage(TextUtil.prefixed("&a아이템을 지급받았습니다: &f" + item.id() + " x" + amount));
        }
    }

    private void handleGet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.prefixed("&c이 명령어는 플레이어만 사용할 수 있습니다."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(TextUtil.prefixed("&c사용법: /forma get <id> [amount]"));
            return;
        }

        FormaItem item = plugin.getItemManager().getItem(args[1]);
        if (item == null) {
            sender.sendMessage(TextUtil.prefixed("&c등록되지 않은 아이템 ID입니다: &f" + args[1]));
            return;
        }

        Integer amount = parseAmount(args.length >= 3 ? args[2] : "1");
        if (amount == null) {
            sender.sendMessage(TextUtil.prefixed("&c수량은 1~64 범위의 정수여야 합니다."));
            return;
        }

        FormaItemBuilder.BuildResult buildResult = itemBuilder.buildWithDebug(item);
        logGiveDebug(buildResult.debugInfo());
        FormaItemGiveEvent giveEvent = new FormaItemGiveEvent(player, item.id(), buildResult.itemStack().clone());
        plugin.getServer().getPluginManager().callEvent(giveEvent);
        debugApiEvent("FormaItemGiveEvent", item.id(), giveEvent.isCancelled());
        if (giveEvent.isCancelled()) {
            sender.sendMessage(TextUtil.prefixed("&e아이템 지급이 이벤트에 의해 취소되었습니다."));
            return;
        }
        giveItem(player, giveEvent.getItemStack(), amount);
        sender.sendMessage(TextUtil.prefixed("&a지급 완료: &f" + item.id() + " x" + amount));
    }

    private void handleList(CommandSender sender) {
        if (plugin.getItemManager().getIds().isEmpty()) {
            sender.sendMessage(TextUtil.prefixed("&e등록된 아이템이 없습니다."));
            return;
        }

        sender.sendMessage(TextUtil.prefixed("&a등록된 아이템 목록 (&f" + plugin.getItemManager().getIds().size() + "&a개)"));
        for (String id : plugin.getItemManager().getIds()) {
            Component prefix = TextUtil.parse(TextUtil.PREFIX_RAW + "&a- ");
            Component clickable = Component.text(id)
                    .color(TextColor.color(0x7CFF6B))
                    .clickEvent(ClickEvent.suggestCommand("/forma get " + id + " "))
                    .hoverEvent(HoverEvent.showText(TextUtil.parse("&7클릭하면 &a/forma get " + id + " &7명령어가 자동 입력됩니다.")));
            sender.sendMessage(prefix.append(clickable));
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadAll();
        sendItemValidationWarnings(sender);
        sender.sendMessage(TextUtil.prefixed("&aconfig.yml, items.yml, blocks.yml, 조합법, cache를 다시 로드했습니다."));
    }

    private void handlePack(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(TextUtil.prefixed("&c사용법: /forma pack <generate|reload>"));
            return;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        if ("generate".equals(action)) {
            handlePackGenerate(sender);
            return;
        }
        if ("reload".equals(action)) {
            handlePackReload(sender);
            return;
        }

        sender.sendMessage(TextUtil.prefixed("&c사용법: /forma pack <generate|reload>"));
    }

    private void handlePackGenerate(CommandSender sender) {
        if (!plugin.getResourcePackManager().isEnabled()) {
            sender.sendMessage(TextUtil.prefixed("&cresource-pack.enabled=false 이므로 생성할 수 없습니다."));
            return;
        }

        plugin.getItemManager().reload();
        sendItemValidationWarnings(sender);
        plugin.getBlockManager().reload();

        try {
            ResourcePackGenerator.GenerationResult result = plugin.getResourcePackManager().generatePack();
            sender.sendMessage(TextUtil.prefixed("&a리소스팩 생성 완료: &f" + result.outputFile().getAbsolutePath()));

            if (!result.missingTextureItems().isEmpty()) {
                sender.sendMessage(TextUtil.prefixed("&e텍스처 누락: &f" + String.join(", ", result.missingTextureItems())));
            }
            if (!result.defaultModelItems().isEmpty()) {
                sender.sendMessage(TextUtil.prefixed("&e기본 모델 생성: &f" + String.join(", ", result.defaultModelItems())));
            }
        } catch (Exception ex) {
            sender.sendMessage(TextUtil.prefixed("&c리소스팩 생성 실패. 콘솔 로그를 확인하세요."));
            plugin.getLogger().log(Level.SEVERE, "리소스팩 생성 중 오류가 발생했습니다.", ex);
        }
    }

    private void handlePackReload(CommandSender sender) {
        plugin.reloadConfig();
        try {
            plugin.getResourcePackManager().ensureDirectories();
            sender.sendMessage(TextUtil.prefixed("&apack 설정을 다시 읽고 resourcepack/generated 폴더를 확인했습니다."));
        } catch (Exception ex) {
            sender.sendMessage(TextUtil.prefixed("&cpack reload 실패. 콘솔 로그를 확인하세요."));
            plugin.getLogger().log(Level.SEVERE, "pack reload 중 오류가 발생했습니다.", ex);
        }
    }

    private void handleBlock(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(TextUtil.prefixed("&c사용법: /forma block <give|get|list|reload>"));
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "give" -> handleBlockGive(sender, args);
            case "get" -> handleBlockGet(sender, args);
            case "list" -> handleBlockList(sender);
            case "reload" -> handleBlockReload(sender);
            default -> sender.sendMessage(TextUtil.prefixed("&c사용법: /forma block <give|get|list|reload>"));
        }
    }

    private void handleRecipe(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(TextUtil.prefixed("&c사용법: /forma recipe <reload|list>"));
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.getItemManager().reload();
                sendItemValidationWarnings(sender);
                plugin.getBlockManager().reload();
                plugin.getRecipeManager().reload();
                sender.sendMessage(TextUtil.prefixed("&aitems.yml 기반 조합법을 다시 등록했습니다."));
            }
            case "list" -> {
                if (plugin.getRecipeManager().getRegisteredRecipes().isEmpty()) {
                    sender.sendMessage(TextUtil.prefixed("&e등록된 Forma 조합법이 없습니다."));
                    return;
                }
                sender.sendMessage(TextUtil.prefixed("&a등록된 조합법 목록 (&f"
                        + plugin.getRecipeManager().getRegisteredRecipes().size() + "&a개)"));
                plugin.getRecipeManager().getRegisteredRecipes().forEach((id, type) ->
                        sender.sendMessage(TextUtil.prefixed("&a- &f" + id + " &7(" + type + ")")));
            }
            default -> sender.sendMessage(TextUtil.prefixed("&c사용법: /forma recipe <reload|list>"));
        }
    }

    private void handleBlockGive(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(TextUtil.prefixed("&c사용법: /forma block give <player> <id> [amount]"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(TextUtil.prefixed("&c플레이어를 찾을 수 없습니다: &f" + args[2]));
            return;
        }

        FormaBlock block = plugin.getBlockManager().getBlock(args[3]);
        if (block == null) {
            sender.sendMessage(TextUtil.prefixed("&c등록되지 않은 블럭 ID입니다: &f" + args[3]));
            return;
        }

        Integer amount = parseAmount(args.length >= 5 ? args[4] : "1");
        if (amount == null) {
            sender.sendMessage(TextUtil.prefixed("&c수량은 1~64 범위의 정수여야 합니다."));
            return;
        }

        giveItem(target, blockItemBuilder.build(block), amount);
        sender.sendMessage(TextUtil.prefixed("&a블럭 지급 완료: &f" + target.getName() + "&a 에게 &f" + block.id() + " x" + amount));
        if (!sender.getName().equalsIgnoreCase(target.getName())) {
            target.sendMessage(TextUtil.prefixed("&a블럭을 지급받았습니다: &f" + block.id() + " x" + amount));
        }
    }

    private void handleBlockGet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.prefixed("&c이 명령어는 플레이어만 사용할 수 있습니다."));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(TextUtil.prefixed("&c사용법: /forma block get <id> [amount]"));
            return;
        }

        FormaBlock block = plugin.getBlockManager().getBlock(args[2]);
        if (block == null) {
            sender.sendMessage(TextUtil.prefixed("&c등록되지 않은 블럭 ID입니다: &f" + args[2]));
            return;
        }

        Integer amount = parseAmount(args.length >= 4 ? args[3] : "1");
        if (amount == null) {
            sender.sendMessage(TextUtil.prefixed("&c수량은 1~64 범위의 정수여야 합니다."));
            return;
        }

        giveItem(player, blockItemBuilder.build(block), amount);
        sender.sendMessage(TextUtil.prefixed("&a블럭 지급 완료: &f" + block.id() + " x" + amount));
    }

    private void handleBlockList(CommandSender sender) {
        if (plugin.getBlockManager().getIds().isEmpty()) {
            sender.sendMessage(TextUtil.prefixed("&e등록된 커스텀 블럭이 없습니다."));
            return;
        }

        boolean debug = plugin.getConfig().getBoolean("debug", false);
        sender.sendMessage(TextUtil.prefixed("&a등록된 커스텀 블럭 목록 (&f" + plugin.getBlockManager().getIds().size() + "&a개)"));
        for (String id : plugin.getBlockManager().getIds()) {
            FormaBlock block = plugin.getBlockManager().getBlock(id);
            if (block == null) {
                continue;
            }

            String label = buildBlockListLabel(id, block, debug);
            Component prefix = TextUtil.parse(TextUtil.PREFIX_RAW + "&a- ");
            Component clickable = Component.text(label)
                    .color(TextColor.color(0x7CFF6B))
                    .clickEvent(ClickEvent.suggestCommand("/forma block get " + id + " "))
                    .hoverEvent(HoverEvent.showText(TextUtil.parse(buildBlockHover(id, block))));
            sender.sendMessage(prefix.append(clickable));
        }
    }

    private String buildBlockListLabel(String id, FormaBlock block, boolean debug) {
        if (debug) {
            return id + " [" + block.providerType().name() + "] -> " + block.visualState().asString();
        }
        if (block.variation() != null) {
            return id + " (v=" + block.variation() + ")";
        }
        if (block.variationRaw() != null && !block.variationRaw().isBlank()) {
            return id + " (v=" + block.variationRaw() + ")";
        }
        return id;
    }

    private String buildBlockHover(String id, FormaBlock block) {
        StringBuilder builder = new StringBuilder();
        builder.append("&7클릭하면 &a/forma block get ").append(id).append(" &7명령어가 자동 입력됩니다.");
        builder.append("\n&7Provider: &f").append(block.providerType().name());
        builder.append("\n&7Visual: &f").append(block.visualState().asString());
        if (block.variationRaw() != null && !block.variationRaw().isBlank()) {
            builder.append("\n&7Variation: &f").append(block.variationRaw());
        }
        if (block.usesNoteBlockProvider() && block.instrument() != null) {
            builder.append("\n&7Mapped: &f")
                    .append(block.instrument().name())
                    .append(" &7/ note &f").append(block.note())
                    .append(" &7/ powered &f").append(block.powered());
            builder.append("\n&8blockstate: ")
                    .append(NoteBlockInstrumentUtil.toBlockStateName(block.instrument()))
                    .append(",").append(block.note()).append(",").append(block.powered());
        }
        return builder.toString();
    }

    private void handleBlockReload(CommandSender sender) {
        plugin.getBlockManager().reload();
        plugin.getBlockStorage().validateLoadedBlocks();
        sender.sendMessage(TextUtil.prefixed("&ablocks.yml을 다시 로드하고 저장 데이터 충돌을 검증했습니다."));
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(TextUtil.prefixed("&a사용 가능한 명령어"));
        sender.sendMessage(TextUtil.prefixed("&f/" + label + " <give|get|list|reload|pack|block|recipe>"));
        sender.sendMessage(TextUtil.prefixed("&f/" + label + " give <player> <id> [amount]"));
        sender.sendMessage(TextUtil.prefixed("&f/" + label + " get <id> [amount]"));
        sender.sendMessage(TextUtil.prefixed("&f/" + label + " list"));
        sender.sendMessage(TextUtil.prefixed("&f/" + label + " reload"));
        sender.sendMessage(TextUtil.prefixed("&f/" + label + " pack <generate|reload>"));
        sender.sendMessage(TextUtil.prefixed("&f/" + label + " block <give|get|list|reload>"));
        sender.sendMessage(TextUtil.prefixed("&f/" + label + " block give <player> <id> [amount]"));
        sender.sendMessage(TextUtil.prefixed("&f/" + label + " block get <id> [amount]"));
        sender.sendMessage(TextUtil.prefixed("&f/" + label + " recipe <reload|list>"));
    }

    private void logGiveDebug(FormaItemBuilder.ModelDebugInfo debugInfo) {
        if (!plugin.getConfig().getBoolean("debug", false)) {
            return;
        }

        plugin.getLogger().info("[DEBUG] item id: " + debugInfo.itemId());
        plugin.getLogger().info("[DEBUG] model-mode: " + debugInfo.modelMode());
        plugin.getLogger().info("[DEBUG] parsed model key: " + debugInfo.parsedModelKey());
        plugin.getLogger().info("[DEBUG] item_model applied: " + debugInfo.itemModelApplied());
        plugin.getLogger().info("[DEBUG] fallback used: " + debugInfo.fallbackUsed());
        plugin.getLogger().info("[DEBUG] attributes applied: " + debugInfo.attributesApplied());
        if (debugInfo.failureReason() != null && !debugInfo.failureReason().isBlank()) {
            plugin.getLogger().info("[DEBUG] item_model failure: " + debugInfo.failureReason());
        }
    }

    private void debugApiEvent(String eventName, String id, boolean cancelled) {
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[DEBUG] API event: " + eventName
                    + ", id=" + id + ", cancelled=" + cancelled);
        }
    }

    private void sendItemValidationWarnings(CommandSender sender) {
        List<String> warnings = plugin.getItemManager().getValidationWarnings();
        if (warnings.isEmpty()) {
            return;
        }

        sender.sendMessage(TextUtil.prefixed("&eitems.yml 설정 경고: &f" + warnings.size() + "&e개"));
        for (String warning : warnings) {
            sender.sendMessage(TextUtil.prefixed("&e- &f" + warning));
        }
    }

    private Integer parseAmount(String raw) {
        try {
            int parsed = Integer.parseInt(raw);
            return parsed >= 1 && parsed <= 64 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void giveItem(Player player, ItemStack template, int amount) {
        int maxStackSize = Math.max(1, template.getMaxStackSize());
        int remaining = amount;
        while (remaining > 0) {
            int giveNow = Math.min(maxStackSize, remaining);
            ItemStack stack = template.clone();
            stack.setAmount(giveNow);

            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
            remaining -= giveNow;
        }
    }

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            return List.of();
        }

        if (args.length == 1) {
            return filter(List.of("give", "get", "list", "reload", "pack", "block", "recipe"), args[0]);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("pack")) {
            return args.length == 2 ? filter(List.of("generate", "reload"), args[1]) : List.of();
        }
        if (sub.equals("block")) {
            return completeBlock(args);
        }
        if (sub.equals("recipe")) {
            return args.length == 2 ? filter(List.of("reload", "list"), args[1]) : List.of();
        }
        if (sub.equals("give")) {
            return completeGive(args);
        }
        if (sub.equals("get")) {
            if (args.length == 2) {
                return filter(plugin.getItemManager().getIds(), args[1]);
            }
            return args.length == 3 ? filter(List.of("1", "16", "64"), args[2]) : List.of();
        }

        return List.of();
    }

    private List<String> completeBlock(String[] args) {
        if (args.length == 2) {
            return filter(List.of("give", "get", "list", "reload"), args[1]);
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("give")) {
            return filter(onlinePlayerNames(), args[2]);
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("give")) {
            return filter(plugin.getBlockManager().getIds(), args[3]);
        }
        if (args.length == 5 && args[1].equalsIgnoreCase("give")) {
            return filter(List.of("1", "16", "64"), args[4]);
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("get")) {
            return filter(plugin.getBlockManager().getIds(), args[2]);
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("get")) {
            return filter(List.of("1", "16", "64"), args[3]);
        }
        return List.of();
    }

    private List<String> completeGive(String[] args) {
        if (args.length == 2) {
            return filter(onlinePlayerNames(), args[1]);
        }
        if (args.length == 3) {
            return filter(plugin.getItemManager().getIds(), args[2]);
        }
        return args.length == 4 ? filter(List.of("1", "16", "64"), args[3]) : List.of();
    }

    private List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    private List<String> filter(Collection<String> source, String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        return source.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
