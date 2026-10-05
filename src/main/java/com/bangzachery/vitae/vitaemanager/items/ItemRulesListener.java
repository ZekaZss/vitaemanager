package com.bangzachery.vitae.vitaemanager.items;

import com.bangzachery.vitae.vitaemanager.core.config.ConfigurationService;
import com.bangzachery.vitae.vitaemanager.core.gui.VitaeMenu;
import com.bangzachery.vitae.vitaemanager.core.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

public final class ItemRulesListener implements Listener, AutoCloseable {
    private final JavaPlugin plugin;
    private final ConfigurationService configuration;
    private final MessageService messages;
    private final ItemRulesService rules;
    private final Map<UUID, BukkitTask> pending = new HashMap<>();
    private final Map<UUID, Long> feedback = new HashMap<>();
    private boolean closed;

    public ItemRulesListener(JavaPlugin plugin, ConfigurationService configuration,
                             MessageService messages, ItemRulesService rules) {
        this.plugin = plugin; this.configuration = configuration; this.messages = messages; this.rules = rules;
    }
    public void start() { Bukkit.getOnlinePlayers().forEach(this::schedule); }
    private void schedule(Player player) {
        if (closed || pending.containsKey(player.getUniqueId())) return;
        pending.put(player.getUniqueId(), Bukkit.getScheduler().runTask(plugin, () -> {
            pending.remove(player.getUniqueId());
            if (!closed && player.isOnline()) rules.stacks().normalize(player);
        }));
    }
    private void denied(Player player) {
        long now = System.nanoTime();
        Long last = feedback.get(player.getUniqueId());
        if (last == null || now - last > 1_000_000_000L) {
            feedback.put(player.getUniqueId(), now); messages.send(player, "item-blocked");
        }
    }
    private boolean blocked(ItemStack item) { return rules.blocked(item); }
    private boolean usable(ItemStack item) { return item != null && !item.getType().isAir() && item.getAmount() > 0; }

    @EventHandler(priority = EventPriority.LOWEST)
    public void interact(PlayerInteractEvent event) {
        if (!blocked(event.getItem())) return;
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        denied(event.getPlayer());
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void entityInteract(PlayerInteractEntityEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItem(event.getHand());
        if (blocked(item)) { event.setCancelled(true); denied(event.getPlayer()); }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void entityInteractAt(PlayerInteractAtEntityEvent event) { entityInteract(event); }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void attack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && blocked(player.getInventory().getItemInMainHand())) {
            event.setCancelled(true); denied(player);
        }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void place(BlockPlaceEvent event) {
        if (blocked(event.getItemInHand())) { event.setCancelled(true); denied(event.getPlayer()); }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void breakBlock(BlockBreakEvent event) {
        if (blocked(event.getPlayer().getInventory().getItemInMainHand())) { event.setCancelled(true); denied(event.getPlayer()); }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void shoot(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player && (blocked(event.getBow()) || blocked(event.getConsumable()))) {
            event.setCancelled(true); denied(player);
        }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void consume(PlayerItemConsumeEvent event) {
        if (blocked(event.getItem())) { event.setCancelled(true); denied(event.getPlayer()); }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void drop(PlayerDropItemEvent event) {
        if (blocked(event.getItemDrop().getItemStack())) { event.setCancelled(true); denied(event.getPlayer()); }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void swap(PlayerSwapHandItemsEvent event) {
        if (blocked(event.getMainHandItem()) || blocked(event.getOffHandItem())) { event.setCancelled(true); denied(event.getPlayer()); }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void spawn(CreatureSpawnEvent event) {
        if (rules.current().blockedMobs().contains(event.getEntityType())) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void click(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getView().getTopInventory().getHolder() instanceof VitaeMenu) return;
        ItemStack hotbar = event.getHotbarButton() >= 0 ? player.getInventory().getItem(event.getHotbarButton()) : null;
        boolean reject = blocked(event.getCurrentItem()) || blocked(event.getCursor()) || blocked(hotbar)
                || (event.getClick() == ClickType.SWAP_OFFHAND && blocked(player.getInventory().getItemInOffHand()));
        if (event instanceof CraftItemEvent craft) {
            for (ItemStack ingredient : craft.getInventory().getMatrix()) reject |= blocked(ingredient);
        }
        // Double-click collects from every slot; do not allow it to collect blocked material.
        if (event.getClick() == ClickType.DOUBLE_CLICK && blocked(event.getCursor())) reject = true;
        if (reject) { event.setCancelled(true); denied(player); }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void drag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && !(event.getView().getTopInventory().getHolder() instanceof VitaeMenu)
                && blocked(event.getOldCursor())) { event.setCancelled(true); denied(player); }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void pickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.isSneaking()) { event.setCancelled(true); return; }
        ItemStack ground = event.getItem().getItemStack();
        if (blocked(ground)) { event.setCancelled(true); return; }
        if (!rules.stacks().managed(ground)) { schedule(player); return; }
        // Native pickup may merge using old components. Allocate real storage explicitly instead.
        event.setCancelled(true);
        rules.stacks().normalize(player);
        ItemStack prepared = rules.stacks().prepared(ground);
        List<PickupPlan.Slot> slots = new ArrayList<>();
        for (int index = 0; index < 36; index++) {
            ItemStack existing = player.getInventory().getItem(index);
            slots.add(new PickupPlan.Slot(usable(existing) ? existing.getAmount() : 0,
                    usable(existing) && existing.isSimilar(prepared)));
        }
        PickupPlan plan = PickupPlan.allocate(ground.getAmount(), prepared.getMaxStackSize(), slots);
        int collected = ground.getAmount() - plan.remaining();
        if (collected == 0) return;
        for (int index = 0; index < 36; index++) {
            int add = plan.added().get(index);
            if (add == 0) continue;
            ItemStack existing = player.getInventory().getItem(index);
            ItemStack result = usable(existing) ? existing.clone() : prepared.clone();
            result.setAmount((usable(existing) ? existing.getAmount() : 0) + add);
            player.getInventory().setItem(index, result);
        }
        player.playPickupItemAnimation(event.getItem(), collected);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.2f, 1.0f);
        if (plan.remaining() == 0) event.getItem().remove();
        else { ItemStack remainder = ground.clone(); remainder.setAmount(plan.remaining()); event.getItem().setItemStack(remainder); }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void pickupArrow(PlayerPickupArrowEvent event) {
        if (!event.getPlayer().isSneaking() || blocked(event.getItem().getItemStack())) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void clicked(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) schedule(player);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void dragged(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) schedule(player);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void opened(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) schedule(player);
    }
    @EventHandler public void joined(PlayerJoinEvent event) { schedule(event.getPlayer()); }
    @EventHandler public void closed(InventoryCloseEvent event) { if (event.getPlayer() instanceof Player player) schedule(player); }
    @EventHandler public void held(PlayerItemHeldEvent event) { schedule(event.getPlayer()); }
    @EventHandler public void quit(PlayerQuitEvent event) {
        BukkitTask task = pending.remove(event.getPlayer().getUniqueId());
        if (task != null) task.cancel();
        feedback.remove(event.getPlayer().getUniqueId());
    }
    @Override public void close() { closed = true; pending.values().forEach(BukkitTask::cancel); pending.clear(); feedback.clear(); }
}
