package com.bangzachery.vitae.vitaemanager.items;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public record ItemRules(Set<Material> blockedItems, Set<EntityType> blockedMobs,
                        Map<Material, Integer> stackLimits) {
    public ItemRules {
        blockedItems = Set.copyOf(blockedItems);
        blockedMobs = Set.copyOf(blockedMobs);
        stackLimits = Map.copyOf(stackLimits);
        stackLimits.forEach(ItemRules::validateLimit);
    }

    public ItemRules toggleItem(Material material) {
        var changed = new HashSet<>(blockedItems);
        if (!changed.remove(material)) changed.add(material);
        return new ItemRules(changed, blockedMobs, stackLimits);
    }

    public ItemRules toggleMob(EntityType type) {
        var changed = new HashSet<>(blockedMobs);
        if (!changed.remove(type)) changed.add(type);
        return new ItemRules(blockedItems, changed, stackLimits);
    }

    public ItemRules stack(Material material, Integer limit) {
        var changed = new HashMap<>(stackLimits);
        if (limit == null) changed.remove(material);
        else { validateLimit(material, limit); changed.put(material, limit); }
        return new ItemRules(blockedItems, blockedMobs, changed);
    }

    public static Material material(String name) {
        return Material.valueOf(identifier(name));
    }

    public static EntityType mob(String name) {
        return EntityType.valueOf(identifier(name));
    }

    private static String identifier(String name) {
        if (name == null || name.isBlank() || !name.equals(name.trim())) throw new IllegalArgumentException("Invalid identifier");
        String key = name.toLowerCase(Locale.ROOT);
        if (key.startsWith("minecraft:")) key = key.substring(10);
        if (!key.matches("[a-z0-9_]+")) throw new IllegalArgumentException("Only Minecraft material/entity identifiers are supported");
        return key.toUpperCase(Locale.ROOT);
    }

    public static void validateLimit(Material material, int limit) {
        if (limit < 1 || limit > 99) {
            throw new IllegalArgumentException("Stack limit must be 1..99");
        }
    }
}