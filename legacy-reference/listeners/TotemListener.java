package com.bangzachery.vitae.vitaemanager.listeners;

import com.bangzachery.vitae.vitaemanager.managers.TotemManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.EntityEffect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class TotemListener implements Listener {
    private final MiniMessage mm = MiniMessage.miniMessage();

    @EventHandler
    public void onTotemAbsorb(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() != Material.TOTEM_OF_UNDYING) {
            item = player.getInventory().getItemInOffHand();
            if (item.getType() != Material.TOTEM_OF_UNDYING) return;
        }

        long cd = TotemManager.getCooldown(player);
        if (System.currentTimeMillis() < cd) {
            int hours = (int) Math.ceil((cd - System.currentTimeMillis()) / 3600000.0);
            // Pesan cooldown diperbarui menjadi lebih keren dan tebal
            player.sendMessage(mm.deserialize("<dark_red><bold>❌ GAGAL!</bold></dark_red> <gray>Totem belum bisa diserap. Tunggu <yellow>" + hours + " jam</yellow> lagi.</gray>"));
            return;
        }

        int current = TotemManager.getAbsorbed(player);
        if (current >= TotemManager.getLimit()) {
            // Pesan limit maksimal diperbarui
            player.sendMessage(mm.deserialize("<red><bold>⚠️ BATAS MAKSIMAL!</bold></red> <gray>Tubuhmu tidak kuat menampung lebih dari <white>" + TotemManager.getLimit() + "</white> totem.</gray>"));
            return;
        }

        event.setCancelled(true);
        item.setAmount(item.getAmount() - 1);
        TotemManager.setAbsorbed(player, current + 1);

        // Pesan sukses diserap diperbarui dengan efek yang memuaskan
        player.sendMessage(mm.deserialize("<green><bold>✦ TOTEM BERHASIL DISERAP! ✦</bold></green> <gray>Total di tubuhmu: <white>" + (current + 1) + "/" + TotemManager.getLimit() + "</white></gray>"));
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.5f, 1.5f);
    }

    @SuppressWarnings({"deprecation", "removal"})
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFatalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.getHealth() - event.getFinalDamage() > 0) return;

        int current = TotemManager.getAbsorbed(player);
        if (current > 0) {
            event.setCancelled(true);

            double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            player.setHealth(maxHealth);

            TotemManager.setAbsorbed(player, current - 1);

            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 900, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1));

            player.playEffect(EntityEffect.TOTEM_RESURRECT);

            if (System.currentTimeMillis() > TotemManager.getCooldown(player)) {
                TotemManager.startCooldown(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVanillaResurrect(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }
}