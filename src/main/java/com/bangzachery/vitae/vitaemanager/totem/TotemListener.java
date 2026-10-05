package com.bangzachery.vitae.vitaemanager.totem;

import com.bangzachery.vitae.vitaemanager.core.message.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class TotemListener implements Listener {
    private final JavaPlugin plugin;
    private final TotemService service;
    private final MessageService messages;
    private final Map<UUID, Integer> lastInteraction = new HashMap<>();

    public TotemListener(JavaPlugin plugin, TotemService service, MessageService messages) {
        this.plugin = plugin;
        this.service = service;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void absorb(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        EquipmentSlot hand = event.getHand();
        if (hand != EquipmentSlot.HAND && hand != EquipmentSlot.OFF_HAND) return;
        ItemStack expected = event.getItem();
        if (!totem(expected)) return;
        // Air's vanilla block denial is not an item denial. Respect explicit item/blocked-block denial.
        if (event.useItemInHand() == Event.Result.DENY
                || (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.useInteractedBlock() == Event.Result.DENY)) return;
        Player player = event.getPlayer();
        if (!player.isOnline() || player.isDead() || player.getGameMode() == GameMode.SPECTATOR) return;
        event.setCancelled(true);
        int tick = Bukkit.getCurrentTick();
        if (Integer.valueOf(tick).equals(lastInteraction.put(player.getUniqueId(), tick))) return;
        if (!player.hasPermission("vitae.totem")) { messages.send(player, "no-permission"); return; }
        ItemStack original = player.getInventory().getItem(hand).clone();
        if (!totem(original) || !original.equals(expected)) return;
        TotemState before;
        try {
            before = service.state(player);
            TotemState after = before.absorb(service.limit(), service.now());
            ItemStack remaining = original.clone();
            remaining.setAmount(original.getAmount() - 1);
            try {
                service.write(player, after);
                player.getInventory().setItem(hand, remaining.getAmount() == 0 ? null : remaining);
                player.saveData();
            }
            catch (RuntimeException exception) {
                // Restore both in-memory halves; native playerdata error behavior still needs server testing.
                player.getInventory().setItem(hand, original);
                service.write(player, before);
                throw exception;
            }
            messages.send(player, "totem-absorbed", Component.text(" " + after.count() + "/" + service.limit()));
            player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.5F, 1.5F);
        } catch (TotemState.Failure failure) {
            Component detail = failure.key().equals("totem-cooldown")
                    ? Component.text(" " + duration(service.state(player).remainingSeconds(service.now())))
                    : Component.text(" " + service.limit());
            messages.send(player, failure.key(), detail);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Penyerapan totem gagal: " + player.getUniqueId(), exception);
            messages.send(player, "totem-failed");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void damage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.isDead()
                || !TotemState.fatal(player.getHealth(), event.getFinalDamage())) return;
        var maximum = player.getAttribute(Attribute.MAX_HEALTH);
        if (maximum == null || !Double.isFinite(maximum.getValue()) || maximum.getValue() <= 0) return;
        try {
            TotemState before = service.state(player);
            if (before.count() == 0) return;
            TotemState after = before.use(service.now(), service.cooldownMillis());
            service.write(player, after);
            try { player.saveData(); }
            catch (RuntimeException exception) { service.write(player, before); throw exception; }
            // Keep the original stored-totem behavior: cancel lethal damage and restore vanilla maximum HP.
            event.setCancelled(true);
            player.setHealth(maximum.getValue());
            player.setFireTicks(0);
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 900, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1));
            player.playEffect(EntityEffect.PROTECTED_FROM_DEATH);
            messages.send(player, "totem-used", Component.text(" " + after.count()));
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Pemakaian totem gagal: " + player.getUniqueId(), exception);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void vanilla(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player && service.blockVanilla()) event.setCancelled(true);
    }

    @EventHandler public void quit(PlayerQuitEvent event) { lastInteraction.remove(event.getPlayer().getUniqueId()); }

    private boolean totem(ItemStack item) {
        return item != null && item.getType() == Material.TOTEM_OF_UNDYING && item.getAmount() > 0;
    }

    private String duration(long seconds) {
        return seconds / 3600 + " jam " + seconds % 3600 / 60 + " menit " + seconds % 60 + " detik";
    }
}