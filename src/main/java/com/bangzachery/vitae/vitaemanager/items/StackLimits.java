package com.bangzachery.vitae.vitaemanager.items;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class StackLimits {
    private final Supplier<ItemRules> rules;
    private final NamespacedKey originalKey, appliedKey;

    public StackLimits(JavaPlugin plugin, Supplier<ItemRules> rules) {
        this.rules = rules;
        originalKey = new NamespacedKey(plugin, "stack_rule_original");
        appliedKey = new NamespacedKey(plugin, "stack_rule_applied");
    }

    public boolean managed(ItemStack item) {
        return usable(item) && (rules.get().stackLimits().containsKey(item.getType())
                || item.getItemMeta().getPersistentDataContainer().has(appliedKey));
    }

    public ItemStack prepared(ItemStack source) {
        ItemStack item = source.clone();
        if (!usable(item) || !managed(item)) return item;
        var meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        Integer requested = rules.get().stackLimits().get(item.getType());
        if ((pdc.has(appliedKey) && !pdc.has(appliedKey, PersistentDataType.INTEGER))
                || (pdc.has(originalKey) && !pdc.has(originalKey, PersistentDataType.INTEGER))) return item;
        Integer applied = pdc.get(appliedKey, PersistentDataType.INTEGER);
        Integer baseline = pdc.get(originalKey, PersistentDataType.INTEGER);
        int current = meta.hasMaxStackSize() ? meta.getMaxStackSize() : 0;
        StackPolicy.Decision decision = StackPolicy.decide(current, applied, baseline, requested,
                item.getType().getMaxDurability() > 0 || (meta instanceof Damageable damage && damage.hasMaxDamage()));
        if (requested == null) {
            if (decision.restore()) meta.setMaxStackSize(decision.maximum() == 0 ? null : decision.maximum());
            pdc.remove(appliedKey);
            pdc.remove(originalKey);
        } else {
            meta.setMaxStackSize(decision.maximum());
            pdc.set(originalKey, PersistentDataType.INTEGER, decision.baseline());
            pdc.set(appliedKey, PersistentDataType.INTEGER, decision.maximum());
        }
        item.setItemMeta(meta);
        return item;
    }

    public void normalize(Player player) {
        var inventory = player.getInventory();
        // Real player storage/equipment only: never mutate cloned plugin menus or crafting outputs.
        for (int slot = 0; slot <= 40; slot++) {
            ItemStack original = inventory.getItem(slot);
            if (!usable(original) || !managed(original)) continue;
            ItemStack prepared = prepared(original);
            int limit = prepared.getMaxStackSize();
            if (slot >= 36 && slot <= 39 && original.getAmount() > 1) continue;
            long needed = PickupPlan.extraSlots(original.getAmount(), limit);
            List<Integer> empty = new ArrayList<>();
            for (int index = 0; index < 36 && empty.size() < needed; index++) {
                if (index != slot && !usable(inventory.getItem(index))) empty.add(index);
            }
            if (empty.size() < needed) continue; // Defer safely until there is room; never truncate/drop.
            int remaining = original.getAmount();
            ItemStack first = prepared.clone();
            first.setAmount(Math.min(remaining, limit));
            remaining -= first.getAmount();
            if (!first.equals(original)) inventory.setItem(slot, first);
            for (int index : empty) {
                ItemStack part = prepared.clone();
                part.setAmount(Math.min(remaining, limit));
                remaining -= part.getAmount();
                inventory.setItem(index, part);
            }
        }
    }

    private boolean usable(ItemStack item) {
        return item != null && !item.getType().isAir() && item.getAmount() > 0;
    }
}