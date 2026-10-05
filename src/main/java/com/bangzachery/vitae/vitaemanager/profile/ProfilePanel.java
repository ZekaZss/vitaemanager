package com.bangzachery.vitae.vitaemanager.profile;

import com.bangzachery.vitae.vitaemanager.core.gui.VitaeMenu;

import com.bangzachery.vitae.vitaemanager.items.ItemRulesService;

import com.bangzachery.vitae.vitaemanager.core.message.MessageService;
import com.bangzachery.vitae.vitaemanager.economy.VitiAmount;
import com.bangzachery.vitae.vitaemanager.economy.VitiService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public final class ProfilePanel implements Listener, AutoCloseable {
    private enum Page { PLAYERS, PROFILE, INVENTORY }
    private final JavaPlugin plugin;
    private final MessageService messages;
    private final VitiService viti;
    private final ItemRulesService itemRules;
    private final Consumer<Player> back;
    private final NamespacedKey vanished;
    private BukkitTask refreshTask;
    private volatile boolean closed;

    public ProfilePanel(JavaPlugin plugin, MessageService messages, VitiService viti,
                        Consumer<Player> back, ItemRulesService itemRules) {
        this.itemRules = itemRules;
        this.plugin = plugin; this.messages = messages; this.viti = viti; this.back = back;
        vanished = new NamespacedKey(plugin, "is_vanished");
    }

    public void start() {
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, 5L, 5L);
    }

    public void openPlayers(Player viewer) { openPlayers(viewer, 0); }

    private void openPlayers(Player viewer, int requestedPage) {
        if (!allowed(viewer, "profile")) return;
        List<? extends Player> players = Bukkit.getOnlinePlayers().stream().filter(p -> visible(viewer, p))
                .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Player::getUniqueId)).toList();
        int page = Math.max(0, Math.min(requestedPage, Math.max(0, (players.size() - 1) / 45)));
        Menu menu = menu(viewer, null, Page.PLAYERS, page, 54, "Vitae | Pemain " + (page + 1));
        for (int slot = 0, index = page * 45; slot < 45 && index < players.size(); slot++, index++) {
            Player target = players.get(index);
            ItemStack head = item(Material.PLAYER_HEAD, target.getName(),
                    "HP: " + Math.round(target.getHealth()) + " | " + target.getGameMode().name());
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(target);
            head.setItemMeta(meta);
            menu.inventory.setItem(slot, head);
            menu.targets.put(slot, target.getUniqueId());
        }
        menu.inventory.setItem(45, item(Material.ARROW, "Kembali", "Panel administrator"));
        if (page > 0) menu.inventory.setItem(48, item(Material.ARROW, "Sebelumnya", "Halaman pemain"));
        if ((page + 1) * 45 < players.size()) menu.inventory.setItem(53, item(Material.ARROW, "Berikutnya", "Halaman pemain"));
        viewer.openInventory(menu.inventory);
    }

    private void openProfile(Player viewer, Player target, int page) {
        if (!allowed(viewer, "profile") || !visible(viewer, target)) return;
        Menu menu = menu(viewer, target, Page.PROFILE, page, 27, "Vitae | " + target.getName());
        renderProfile(menu, target);
        viewer.openInventory(menu.inventory);
    }

    private void renderProfile(Menu menu, Player target) {
        menu.inventory.setItem(0, item(Material.GOLDEN_APPLE, "Heal", "Pulihkan HP pemain yang masih hidup."));
        menu.inventory.setItem(3, item(Material.TOTEM_OF_UNDYING, "Revive", reviveAvailable()
                ? "Kirim permintaan revive." : "Integrasi revive tidak tersedia."));
        menu.inventory.setItem(4, item(Material.GRASS_BLOCK, "Gamemode: " + target.getGameMode().name(), "Klik untuk mengganti mode."));
        menu.inventory.setItem(6, item(Material.ENDER_PEARL, "Teleport ke pemain", target.getName()));
        menu.inventory.setItem(8, item(Material.ENDER_EYE, "Tarik pemain", target.getName()));
        String balance;
        try { balance = VitiAmount.format(viti.balance(target)); }
        catch (IllegalArgumentException exception) { balance = "Saldo belum tersedia"; }
        menu.inventory.setItem(10, item(Material.PAPER, "Saldo Viti", balance));
        menu.inventory.setItem(12, item(Material.COOKED_BEEF, "Feed", "Pulihkan hunger dan saturation."));
        menu.inventory.setItem(14, item(Material.CHEST, "Invsee", "Klik kiri/kanan untuk mengatur item."));
        menu.inventory.setItem(22, item(Material.ARROW, "Kembali", "Daftar pemain"));
    }

    private void openInventory(Player viewer, Player target, int page) {
        if (!allowed(viewer, "invsee") || !visible(viewer, target)) return;
        itemRules.stacks().normalize(target);
        Menu menu = menu(viewer, target, Page.INVENTORY, page, 54, "Vitae | Tas " + target.getName());
        menu.inventory.setItem(50, item(Material.PAPER, "Klik kiri / kanan", "Shift, drag dan hotbar swap diblokir."));
        menu.inventory.setItem(51, item(Material.IRON_HELMET, "Armor", "45 kepala, 46 dada, 47 kaki, 48 sepatu"));
        menu.inventory.setItem(52, item(Material.SHIELD, "Offhand", "Slot 49"));
        menu.inventory.setItem(53, item(Material.ARROW, "Kembali", "Profil pemain"));
        renderInventory(menu, target);
        viewer.openInventory(menu.inventory);
    }

    private Menu menu(Player viewer, Player target, Page page, int listPage, int size, String title) {
        Menu menu = new Menu(viewer.getUniqueId(), target == null ? null : target.getUniqueId(), page, listPage);
        menu.inventory = Bukkit.createInventory(menu, size, Component.text(title, NamedTextColor.DARK_GRAY));
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", "");
        for (int slot = 0; slot < size; slot++) menu.inventory.setItem(slot, filler);
        return menu;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void click(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Menu menu)) return;
        boolean cancelled = event.isCancelled();
        event.setCancelled(true); // Cancel even empty slots, creative clones and bottom-inventory clicks.
        if (cancelled || event instanceof InventoryCreativeEvent || menu.pending || closed
                || !(event.getWhoClicked() instanceof Player viewer)) return;
        if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT) return;
        int raw = event.getRawSlot();
        if (raw < 0 || raw >= event.getView().countSlots()) return;
        int ownSlot = raw >= menu.inventory.getSize() ? event.getSlot() : -1;
        if (ownSlot >= 0 && (event.getClickedInventory() != viewer.getInventory() || ownSlot >= 36)) return;
        ItemStack expected = copy(event.getCurrentItem()), cursor = copy(event.getCursor());
        boolean right = event.getClick() == ClickType.RIGHT;
        menu.pending = true;
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (!active(viewer, menu)) return;
                if (menu.page == Page.INVENTORY && (targetSlot(raw) >= 0 || ownSlot >= 0)) {
                    exchange(viewer, menu, raw, ownSlot, expected, cursor, right);
                } else if (raw < menu.inventory.getSize()) navigate(viewer, menu, raw);
            } finally { menu.pending = false; }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void drag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu) event.setCancelled(true);
    }

    private void navigate(Player viewer, Menu menu, int slot) {
        if (menu.page == Page.PLAYERS) {
            if (slot == 45) back.accept(viewer);
            else if (slot == 48 && menu.listPage > 0) openPlayers(viewer, menu.listPage - 1);
            else if (slot == 53) openPlayers(viewer, menu.listPage + 1);
            else if (menu.targets.containsKey(slot)) {
                Player target = Bukkit.getPlayer(menu.targets.get(slot));
                if (target != null && visible(viewer, target)) openProfile(viewer, target, menu.listPage);
                else messages.send(viewer, "profile-unavailable");
            }
            return;
        }
        Player target = Bukkit.getPlayer(menu.target);
        if (menu.page == Page.INVENTORY) {
            if (slot == 53) openProfile(viewer, target, menu.listPage);
            return;
        }
        if (slot == 22) { openPlayers(viewer, menu.listPage); return; }
        String permission = switch (slot) {
            case 0 -> "heal"; case 3 -> "revive"; case 4 -> "gamemode";
            case 6 -> "teleport"; case 8 -> "pull"; case 12 -> "feed"; case 14 -> "invsee";
            default -> null;
        };
        if (permission == null || !allowed(viewer, permission)) return;
        switch (slot) {
            case 0 -> {
                var maximum = target.getAttribute(Attribute.MAX_HEALTH);
                if (target.isDead() || maximum == null) { messages.send(viewer, "profile-action-failed"); return; }
                target.setHealth(maximum.getValue());
                target.setFireTicks(0);
            }
            case 3 -> {
                var command = Bukkit.getPluginCommand("revive");
                if (!reviveAvailable()) { messages.send(viewer, "profile-integration-unavailable"); return; }
                if (!command.testPermissionSilent(viewer)) { messages.send(viewer, "no-permission"); return; }
                boolean dispatched = viewer.performCommand("revive revive " + target.getName());
                messages.send(viewer, dispatched ? "profile-revive-requested" : "profile-action-failed");
                return;
            }
            case 4 -> target.setGameMode(switch (target.getGameMode()) {
                case SURVIVAL -> GameMode.CREATIVE; case CREATIVE -> GameMode.ADVENTURE;
                case ADVENTURE -> GameMode.SPECTATOR; case SPECTATOR -> GameMode.SURVIVAL;
            });
            case 6, 8 -> {
                Player moving = slot == 6 ? viewer : target;
                Player destination = slot == 6 ? target : viewer;
                moving.teleportAsync(destination.getLocation()).whenComplete((success, failure) -> {
                    if (closed) return;
                    try {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (!closed && viewer.isOnline()) messages.send(viewer,
                                    failure == null && Boolean.TRUE.equals(success)
                                            ? "profile-action-done" : "profile-action-failed");
                        });
                    } catch (IllegalPluginAccessException ignored) {
                        // Disable can occur between future completion and scheduling.
                    }
                });
                return;
            }
            case 12 -> { target.setFoodLevel(20); target.setSaturation(20F); target.setExhaustion(0F); }
            case 14 -> { openInventory(viewer, target, menu.listPage); return; }
            default -> { return; }
        }
        messages.send(viewer, "profile-action-done");
        renderProfile(menu, target);
    }

    private void exchange(Player viewer, Menu menu, int raw, int ownSlot,
                          ItemStack expected, ItemStack cursor, boolean right) {
        Player target = Bukkit.getPlayer(menu.target);
        int index = ownSlot >= 0 ? ownSlot : targetSlot(raw);
        var inventory = ownSlot >= 0 ? viewer.getInventory() : target.getInventory();
        ItemStack actual = copy(inventory.getItem(index));
        if (!Objects.equals(actual, expected) || !Objects.equals(copy(viewer.getItemOnCursor()), cursor)) {
            messages.send(viewer, "profile-inventory-changed");
            renderInventory(menu, target);
            return;
        }
        if (itemRules.blocked(actual) || itemRules.blocked(cursor)) {
            messages.send(viewer, "item-blocked"); return;
        }
        // A rule reload may have changed metadata since this view was rendered.
        ItemStack preparedActual = actual == null ? null : itemRules.stacks().prepared(actual);
        ItemStack preparedCursor = cursor == null ? null : itemRules.stacks().prepared(cursor);
        if (!Objects.equals(actual, preparedActual)) {
            itemRules.stacks().normalize(ownSlot >= 0 ? viewer : target);
            renderInventory(menu, target);
            messages.send(viewer, "profile-inventory-changed"); return;
        }
        if (preparedCursor != null && preparedCursor.getAmount() > preparedCursor.getMaxStackSize()) {
            messages.send(viewer, "item-stack-full"); return;
        }
        cursor = preparedCursor;
        if (index >= 36 && index <= 39 && cursor != null && !fitsArmor(cursor, index)) return;
        StackExchange.Result result = StackExchange.move(count(actual), count(cursor),
                actual != null && cursor != null && actual.isSimilar(cursor),
                actual == null ? 99 : actual.getMaxStackSize(), cursor == null ? 99 : cursor.getMaxStackSize(),
                index >= 36 && index <= 39 ? 1 : 99, right);
        ItemStack newSlot = amount(result.swapped() || actual == null ? cursor : actual, result.slot());
        ItemStack newCursor = amount(result.swapped() || cursor == null ? actual : cursor, result.cursor());
        // Server-thread commit: one live slot and cursor, never a whole snapshot or close-time flush.
        inventory.setItem(index, newSlot);
        viewer.setItemOnCursor(newCursor);
        renderInventory(menu, target);
        viewer.updateInventory();
        if (target != viewer) target.updateInventory();
    }

    private boolean fitsArmor(ItemStack item, int index) {
        EquipmentSlot desired = switch (index) {
            case 36 -> EquipmentSlot.FEET; case 37 -> EquipmentSlot.LEGS;
            case 38 -> EquipmentSlot.CHEST; case 39 -> EquipmentSlot.HEAD;
            default -> throw new IllegalArgumentException("Not an armor slot");
        };
        var meta = item.getItemMeta();
        if (meta.hasEquippable()) {
            var component = meta.getEquippable();
            var entities = component.getAllowedEntities();
            return component.getSlot() == desired && (entities == null || entities.contains(EntityType.PLAYER));
        }
        return item.getType().getEquipmentSlot() == desired;
    }

    private int targetSlot(int raw) {
        if (raw >= 0 && raw < 36) return raw;
        return switch (raw) { case 45 -> 39; case 46 -> 38; case 47 -> 37; case 48 -> 36; case 49 -> 40; default -> -1; };
    }

    private void renderInventory(Menu menu, Player target) {
        for (int raw = 0; raw < 50; raw++) {
            int index = targetSlot(raw);
            if (index >= 0) menu.inventory.setItem(raw, copy(target.getInventory().getItem(index)));
        }
    }

    private void refresh() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!(viewer.getOpenInventory().getTopInventory().getHolder() instanceof Menu menu)) continue;
            if (!active(viewer, menu) || menu.pending) continue;
            if (menu.page == Page.PLAYERS) {
                menu.targets.entrySet().removeIf(entry -> {
                    if (visible(viewer, Bukkit.getPlayer(entry.getValue()))) return false;
                    menu.inventory.setItem(entry.getKey(), item(Material.BLACK_STAINED_GLASS_PANE, " ", ""));
                    return true;
                });
                continue;
            }
            Player target = Bukkit.getPlayer(menu.target);
            if (menu.page == Page.INVENTORY) renderInventory(menu, target);
            else renderProfile(menu, target);
        }
    }

    private boolean active(Player viewer, Menu menu) {
        if (closed || !viewer.isOnline() || viewer.getOpenInventory().getTopInventory() != menu.inventory) return false;
        if (!viewer.getUniqueId().equals(menu.owner) || viewer.isDead() || !has(viewer, "profile")
                || (menu.page == Page.INVENTORY && !has(viewer, "invsee"))) {
            messages.send(viewer, "no-permission"); viewer.closeInventory(); return false;
        }
        Player target = menu.target == null ? null : Bukkit.getPlayer(menu.target);
        if (menu.target != null && (!visible(viewer, target)
                || (menu.page == Page.INVENTORY && target.isDead()))) {
            messages.send(viewer, "profile-unavailable"); viewer.closeInventory(); return false;
        }
        return true;
    }

    private boolean visible(Player viewer, Player target) {
        if (target == null || !target.isOnline() || !viewer.canSee(target)) return false;
        if (target.getMetadata("vanished").stream().anyMatch(value -> value.asBoolean())) return false;
        if (target.getPersistentDataContainer().has(vanished)
                && !target.getPersistentDataContainer().has(vanished, PersistentDataType.BOOLEAN)) return false;
        return !Boolean.TRUE.equals(target.getPersistentDataContainer().get(vanished, PersistentDataType.BOOLEAN));
    }

    private boolean reviveAvailable() {
        var command = Bukkit.getPluginCommand("revive");
        return command != null && command.getPlugin().isEnabled();
    }

    private boolean has(Player player, String action) {
        return player.hasPermission("vitae.admin") && player.hasPermission("vitae.admin.profile")
                && player.hasPermission("vitae.admin." + action);
    }

    private boolean allowed(Player player, String action) {
        if (!closed && has(player, action)) return true;
        messages.send(player, "no-permission"); return false;
    }

    private ItemStack item(Material material, String title, String lore) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(title, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text(lore, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack copy(ItemStack item) { return item == null || item.getType().isAir() ? null : item.clone(); }
    private int count(ItemStack item) { return item == null ? 0 : item.getAmount(); }
    private ItemStack amount(ItemStack item, int count) {
        if (count == 0) return null;
        ItemStack result = Objects.requireNonNull(item).clone(); result.setAmount(count); return result;
    }

    @Override
    public void close() {
        closed = true;
        if (refreshTask != null) refreshTask.cancel();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof Menu) player.closeInventory();
        }
    }

    private static final class Menu implements VitaeMenu {
        private final UUID owner, target;
        private final Page page;
        private final int listPage;
        private final Map<Integer, UUID> targets = new HashMap<>();
        private Inventory inventory;
        private boolean pending;
        private Menu(UUID owner, UUID target, Page page, int listPage) {
            this.owner = owner; this.target = target; this.page = page; this.listPage = listPage;
        }
        @Override public @NotNull Inventory getInventory() { return inventory; }
    }
}