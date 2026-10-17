package com.bangzachery.vitae.vitaemanager.evolution;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import java.util.*;

/** Event damage normal menjaga kompatibilitas dengan TotemListener Vitae. */
public final class EvolutionBlast {
    private final Set<UUID> lethalHits = new HashSet<>();
    public void hit(Player player, boolean lethal, double damage) {
        if (player.isDead() || (player.getGameMode() != GameMode.SURVIVAL
                && player.getGameMode() != GameMode.ADVENTURE)) return;
        if (!lethal) { if (damage > 0) player.damage(damage); return; }
        if (!lethalHits.add(player.getUniqueId())) return;
        int previous = player.getNoDamageTicks();
        try {
            player.setNoDamageTicks(0);
            player.damage(Math.max(1000, player.getHealth() + player.getAbsorptionAmount() + 1));
        } finally {
            lethalHits.remove(player.getUniqueId());
            if (!player.isDead()) player.setNoDamageTicks(Math.max(previous, player.getNoDamageTicks()));
        }
    }
    @SuppressWarnings("removal")
    public void adjust(EntityDamageEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof Player player)
                || !lethalHits.contains(player.getUniqueId())) return;
        event.setDamage(Math.max(1, player.getHealth() + player.getAbsorptionAmount() + 1));
        for (EntityDamageEvent.DamageModifier modifier : EntityDamageEvent.DamageModifier.values())
            if (modifier != EntityDamageEvent.DamageModifier.BASE && event.isApplicable(modifier))
                event.setDamage(modifier, 0);
    }
}
