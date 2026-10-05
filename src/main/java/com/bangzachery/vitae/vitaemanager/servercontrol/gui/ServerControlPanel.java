package com.bangzachery.vitae.vitaemanager.servercontrol.gui;

import com.bangzachery.vitae.vitaemanager.core.gui.VitaeMenu;

import com.bangzachery.vitae.vitaemanager.core.message.MessageService;
import com.bangzachery.vitae.vitaemanager.servercontrol.ServerControlService;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class ServerControlPanel implements Listener, AutoCloseable {
    private enum Page { MAIN, TIME, WEATHER }

    private final JavaPlugin plugin;
    private final ServerControlService service;
    private final MessageService messages;
    private final Consumer<Player> players;

    public ServerControlPanel(JavaPlugin plugin, ServerControlService service, MessageService messages,
                              Consumer<Player> players) {
        this.plugin = plugin;
        this.service = service;
        this.messages = messages;
        this.players = players;
    }

    public void open(Player player) { open(player, player.getWorld(), Page.MAIN); }

    private void open(Player player, World world, Page page) {
        if (!player.hasPermission("vitae.admin")) { messages.send(player, "no-permission"); return; }
        String title = switch (page) {
            case MAIN -> "Vitae | Kontrol Server";
            case TIME -> "Vitae | Waktu";
            case WEATHER -> "Vitae | Cuaca";
        };
        Menu holder = new Menu(player.getUniqueId(), world.getUID(), page);
        Inventory inventory = Bukkit.createInventory(holder, page == Page.MAIN ? 54 : 9,
                Component.text(title, NamedTextColor.DARK_GRAY));
        holder.inventory = inventory;
        ItemStack glass = item(Material.BLACK_STAINED_GLASS_PANE, " ", "");
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, glass);
        if (page == Page.MAIN) {
            inventory.setItem(4, item(Material.GRASS_BLOCK, "Dunia: " + world.getName(), "Pengaturan berlaku untuk dunia ini."));
            inventory.setItem(11, item(Material.CLOCK, "Waktu", "Pilih pagi, siang, sore, atau malam."));
            inventory.setItem(15, item(Material.SUNFLOWER, "Cuaca", "Pilih cerah, hujan, atau badai."));
            inventory.setItem(22, item(Material.PLAYER_HEAD, "Pemain Online", "Profil dan inventory pemain."));
            inventory.setItem(29, item(Material.DIAMOND_SWORD, "PvP", status(world.getPVP())));
            inventory.setItem(31, item(Material.ZOMBIE_HEAD, "Vanilla Mob Spawning",
                    status(Boolean.TRUE.equals(world.getGameRuleValue(GameRule.DO_MOB_SPAWNING)))));
            inventory.setItem(33, item(Material.PAPER, "Chat", status(!service.current().chatMuted())));
            inventory.setItem(49, item(Material.BARRIER, "Maintenance", status(service.current().maintenance())));
        } else if (page == Page.TIME) {
            inventory.setItem(0, item(Material.LIGHT_BLUE_DYE, "Pagi", "06:00"));
            inventory.setItem(2, item(Material.YELLOW_DYE, "Siang", "12:00"));
            inventory.setItem(4, item(Material.ORANGE_DYE, "Sore", "18:00"));
            inventory.setItem(6, item(Material.BLUE_DYE, "Malam", "00:00"));
        } else {
            inventory.setItem(0, item(Material.SUNFLOWER, "Cerah", "Hentikan hujan dan badai."));
            inventory.setItem(2, item(Material.WATER_BUCKET, "Hujan", "Hujan tanpa badai petir."));
            inventory.setItem(4, item(Material.TRIDENT, "Badai", "Hujan dan badai petir."));
        }
        if (page != Page.MAIN) inventory.setItem(8, item(Material.ARROW, "Kembali", "Kontrol server"));
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void click(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Menu menu)) return;
        boolean alreadyCancelled = event.isCancelled();
        event.setCancelled(true);
        if (alreadyCancelled || !(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= menu.getInventory().getSize()) return;
        if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT) return;
        // Inventory transitions and actions run next tick, after the click transaction.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || !player.getUniqueId().equals(menu.owner)
                    || player.getOpenInventory().getTopInventory() != menu.inventory) return;
            if (!player.hasPermission("vitae.admin")) {
                messages.send(player, "no-permission");
                player.closeInventory();
                return;
            }
            World world = Bukkit.getWorld(menu.world);
            if (world == null) { messages.send(player, "world-not-found"); player.closeInventory(); return; }
            action(player, world, menu, slot);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void drag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu) event.setCancelled(true);
    }

    private void action(Player player, World world, Menu menu, int slot) {
        if (menu.page != Page.MAIN && slot == 8) { open(player, world, Page.MAIN); return; }
        if (menu.page == Page.TIME) {
            if (!allowed(player, "time")) return;
            long time = switch (slot) { case 0 -> 0L; case 2 -> 6000L; case 4 -> 12000L; case 6 -> 18000L; default -> -1L; };
            if (time < 0) return;
            world.setTime(time);
            messages.send(player, "control-changed");
            open(player, world, Page.TIME);
            return;
        }
        if (menu.page == Page.WEATHER) {
            if (!allowed(player, "weather") || (slot != 0 && slot != 2 && slot != 4)) return;
            world.setStorm(slot != 0);
            world.setThundering(slot == 4);
            if (slot == 0) world.setClearWeatherDuration(12000);
            else { world.setClearWeatherDuration(0); world.setWeatherDuration(12000); }
            world.setThunderDuration(12000);
            messages.send(player, "control-changed");
            open(player, world, Page.WEATHER);
            return;
        }
        Consumer<ServerControlService.Result> done = result -> {
            if (!player.isOnline()) return;
            messages.send(player, switch (result) {
                case APPLIED -> "control-changed";
                case BUSY -> "control-busy";
                case FAILED -> "control-failed";
            });
            if (result == ServerControlService.Result.APPLIED
                    && player.getOpenInventory().getTopInventory() == menu.inventory
                    && player.hasPermission("vitae.admin")) {
                World loaded = Bukkit.getWorld(menu.world);
                if (loaded != null) open(player, loaded, Page.MAIN);
            }
        };
        switch (slot) {
            case 11 -> { if (allowed(player, "time")) open(player, world, Page.TIME); }
            case 15 -> { if (allowed(player, "weather")) open(player, world, Page.WEATHER); }
            case 22 -> { if (allowed(player, "profile")) players.accept(player); }
            case 29 -> { if (allowed(player, "pvp")) service.world(world, !world.getPVP(), null, done); }
            case 31 -> { if (allowed(player, "mobspawning")) service.world(world, null,
                    !Boolean.TRUE.equals(world.getGameRuleValue(GameRule.DO_MOB_SPAWNING)), done); }
            case 33 -> { if (allowed(player, "chat")) service.chatMuted(!service.current().chatMuted(), done); }
            case 49 -> { if (allowed(player, "maintenance")) service.maintenance(!service.current().maintenance(), done); }
            default -> { }
        }
    }

    private boolean allowed(Player player, String action) {
        if (player.hasPermission("vitae.admin." + action)) return true;
        messages.send(player, "no-permission");
        return false;
    }

    private String status(boolean enabled) { return "Status: " + (enabled ? "ON" : "OFF"); }

    private ItemStack item(Material material, String title, String lore) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(title, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text(lore, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void close() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof Menu) player.closeInventory();
        }
    }

    private static final class Menu implements VitaeMenu {
        private final UUID owner;
        private final UUID world;
        private final Page page;
        private Inventory inventory;

        private Menu(UUID owner, UUID world, Page page) {
            this.owner = owner;
            this.world = world;
            this.page = page;
        }

        @Override
        public @NotNull Inventory getInventory() { return inventory; }
    }
}