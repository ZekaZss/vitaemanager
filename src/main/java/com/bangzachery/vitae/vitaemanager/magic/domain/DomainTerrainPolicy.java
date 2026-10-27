package com.bangzachery.vitae.vitaemanager.magic.domain;

import org.bukkit.Material;
import java.util.Set;

/** Safety depends on block mechanics, never on terrain height or a requirement for an empty sphere. */
final class DomainTerrainPolicy {
    private static final Set<Material> SURFACE = Set.of(
            Material.SHORT_GRASS, Material.FERN, Material.DEAD_BUSH, Material.SNOW,
            Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM,
            Material.AZURE_BLUET, Material.RED_TULIP, Material.ORANGE_TULIP,
            Material.WHITE_TULIP, Material.PINK_TULIP, Material.OXEYE_DAISY,
            Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY, Material.WITHER_ROSE,
            Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.PINK_PETALS,
            Material.AZALEA, Material.FLOWERING_AZALEA, Material.MOSS_CARPET,
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
            Material.TORCHFLOWER, Material.TORCHFLOWER_CROP, Material.SWEET_BERRY_BUSH);
    private static final Set<Material> DOUBLE_PLANTS = Set.of(Material.TALL_GRASS, Material.LARGE_FERN,
            Material.SUNFLOWER, Material.LILAC, Material.ROSE_BUSH, Material.PEONY, Material.PITCHER_PLANT, Material.PITCHER_CROP);
    private DomainTerrainPolicy() {}
    static boolean naturalDoublePlant(Material type) {return DOUBLE_PLANTS.contains(type);}
    static boolean container(Material type) {
        return type==Material.CHEST || type==Material.TRAPPED_CHEST || type==Material.BARREL
                || type==Material.SHULKER_BOX || type.name().endsWith("_SHULKER_BOX");
    }
    static boolean supported(Material type) {
        return supported(type,type.hasGravity(),type.isAir(),type.isSolid(),type.isOccluding());
    }
    /** Pure decision over platform-supplied material facts; tests do not bootstrap a fake Bukkit registry. */
    static boolean supported(Material type,boolean gravity,boolean air,boolean solid,boolean occluding) {
        if(gravity || type==Material.WATER || type==Material.LAVA
                || type==Material.MOVING_PISTON || type==Material.PISTON_HEAD
                || type==Material.PISTON || type==Material.STICKY_PISTON
                || type==Material.CACTUS || type==Material.SUGAR_CANE || type==Material.BAMBOO
                || type==Material.BAMBOO_SAPLING || type==Material.CHORUS_PLANT || type==Material.CHORUS_FLOWER
                || type==Material.SCAFFOLDING || type==Material.POINTED_DRIPSTONE
                || type==Material.NETHER_PORTAL || type==Material.END_PORTAL || type==Material.END_GATEWAY) return false;
        return air || occluding || solid || container(type)
                || SURFACE.contains(type) || DOUBLE_PLANTS.contains(type)
                || type.name().endsWith("_SAPLING") || type.name().endsWith("_LEAVES");
    }
    static boolean unsupportedConnectedNeighbour(Material type,boolean gravity,boolean bedOrDoor) {
        return gravity || bedOrDoor || type==Material.CACTUS || type==Material.SUGAR_CANE
                || type==Material.BAMBOO || type==Material.BAMBOO_SAPLING
                || type==Material.CHORUS_PLANT || type==Material.CHORUS_FLOWER
                || type==Material.SCAFFOLDING || type==Material.POINTED_DRIPSTONE;
    }
    /** Container restoration uses Structure.place for NBT and needs stable neighbouring shapes. */
    static boolean stableContainerNeighbour(Material type) {
        return stableContainerNeighbour(type,type.hasGravity(),type.isAir(),type.isOccluding());
    }
    static boolean stableContainerNeighbour(Material type,boolean gravity,boolean air,boolean occluding) {
        return !gravity && (air || occluding || container(type));
    }
}
