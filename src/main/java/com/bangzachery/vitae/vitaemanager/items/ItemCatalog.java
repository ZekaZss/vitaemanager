package com.bangzachery.vitae.vitaemanager.items;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import java.util.*;

/** Snapshot Paper registries once on the server thread; disk workers read only immutable sets. */
public record ItemCatalog(Set<Material> materials, Set<EntityType> mobs, Set<Material> damageable) {
    public ItemCatalog {
        materials = Set.copyOf(materials); mobs = Set.copyOf(mobs); damageable = Set.copyOf(damageable);
        if (!materials.containsAll(damageable)) throw new IllegalArgumentException("Unknown damageable material");
    }
    public static ItemCatalog fromPaper() {
        var materials = new HashSet<Material>();
        var damageable = new HashSet<Material>();
        for (Material material : Material.values()) {
            if (material.isLegacy() || !material.isItem() || material.isAir()) continue;
            materials.add(material);
            if (material.getMaxDurability() > 0) damageable.add(material);
        }
        var mobs = new HashSet<EntityType>();
        for (EntityType type : EntityType.values()) if (type.isAlive() && type.isSpawnable()) mobs.add(type);
        return new ItemCatalog(materials, mobs, damageable);
    }
    public Material material(String name) {
        Material material = ItemRules.material(name);
        if (!materials.contains(material)) throw new IllegalArgumentException("Not an item material");
        return material;
    }
    public EntityType mob(String name) {
        EntityType type = ItemRules.mob(name);
        if (!mobs.contains(type)) throw new IllegalArgumentException("Not a spawnable living entity");
        return type;
    }
    public void limit(Material material, int limit) {
        if (!materials.contains(material) || limit < 1 || limit > 99 || (damageable.contains(material) && limit > 1)) {
            throw new IllegalArgumentException("Stack limit must be 1..99; damageable equipment must remain 1");
        }
    }
    public void validate(ItemRules rules) {
        if (!materials.containsAll(rules.blockedItems()) || !mobs.containsAll(rules.blockedMobs())) {
            throw new IllegalArgumentException("Unknown blocked item/mob");
        }
        rules.stackLimits().forEach(this::limit);
    }
}