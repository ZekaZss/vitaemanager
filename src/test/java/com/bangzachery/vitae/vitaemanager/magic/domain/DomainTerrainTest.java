package com.bangzachery.vitae.vitaemanager.magic.domain;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.structure.Palette;
import org.bukkit.structure.Structure;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.ToIntBiFunction;
import static org.junit.jupiter.api.Assertions.*;

class DomainTerrainTest {
    @Test void flatSlopeHillAndValleyAcceptNaturalShellWithoutFlatteningInterior() {
        List<ToIntBiFunction<Integer,Integer>> heights=List.of((x,z)->0,(x,z)->x/3,(x,z)->12-(x*x+z*z)/60,(x,z)->-12+(x*x+z*z)/60);
        List<DomainGeometry.Cell> shell=DomainGeometry.shell(30);
        for(var terrain:heights) {
            Map<DomainGeometry.Cell,Material> before=new HashMap<>(),live=new HashMap<>();
            for(int x=-30;x<=30;x++)for(int y=-30;y<=30;y++)for(int z=-30;z<=30;z++) {
                var cell=new DomainGeometry.Cell(x,y,z);int height=terrain.applyAsInt(x,z);
                Material material=y<height-1?Material.STONE:y<height?Material.DIRT:y==height?Material.GRASS_BLOCK:y==height+1?Material.SHORT_GRASS:Material.AIR;
                before.put(cell,material);live.put(cell,material);
            }
            Map<DomainGeometry.Cell,Material> saved=new HashMap<>();
            for(var cell:shell){assertTrue(supported(before.get(cell)),"Terrain block rejected: "+before.get(cell));saved.put(cell,before.get(cell));live.put(cell,Material.OBSIDIAN);}
            for(var entry:before.entrySet())if(!saved.containsKey(entry.getKey()))assertEquals(entry.getValue(),live.get(entry.getKey()),"Interior changed");
            saved.forEach(live::put);assertEquals(before,live);
        }
    }
    @Test void commonSurfacePlantsSnowAndLeavesAreNotMisclassifiedAsInvalidTerrain() {
        for(Material material:List.of(Material.SHORT_GRASS,Material.FERN,Material.DANDELION,Material.SNOW,Material.OAK_LEAVES,Material.OAK_SAPLING,Material.POPPY,Material.MOSS_CARPET))assertTrue(supported(material),material.toString());
        assertTrue(DomainTerrainPolicy.naturalDoublePlant(Material.TALL_GRASS));assertFalse(DomainTerrainPolicy.naturalDoublePlant(Material.OAK_DOOR));
    }
    @Test void fluidsGravityAndLinkedSpecialMechanicsRemainExplicitlyUnsupported() {
        for(Material material:List.of(Material.SAND,Material.GRAVEL,Material.WATER,Material.LAVA,Material.MOVING_PISTON,Material.END_PORTAL,Material.CACTUS))assertFalse(supported(material),material.toString());
        assertFalse(DomainTerrainPolicy.stableContainerNeighbour(Material.SHORT_GRASS,false,false,false));assertTrue(DomainTerrainPolicy.stableContainerNeighbour(Material.STONE,false,false,true));
    }
    @Test void unsupportedConnectedNeighboursFailClosedWithoutRejectingSafeNaturalSurface() {
        assertTrue(DomainTerrainPolicy.unsupportedConnectedNeighbour(Material.SAND,true,false));
        assertTrue(DomainTerrainPolicy.unsupportedConnectedNeighbour(Material.OAK_DOOR,false,true));
        assertTrue(DomainTerrainPolicy.unsupportedConnectedNeighbour(Material.RED_BED,false,true));
        assertTrue(DomainTerrainPolicy.unsupportedConnectedNeighbour(Material.BAMBOO,false,false));
        for(Material natural:List.of(Material.SHORT_GRASS,Material.SNOW,Material.DANDELION,Material.FERN,Material.OAK_LEAVES))
            assertFalse(DomainTerrainPolicy.unsupportedConnectedNeighbour(natural,false,false));
    }
    @Test void boundingBoxEdgeOverlapIsDetectedEvenWhenFeetAndEyesUseAnotherBlock() {
        Set<DomainGeometry.Cell> shell=Set.of(new DomainGeometry.Cell(1,0,0));
        assertTrue(DomainGeometry.overlapsShell(shell,.8,0,.2,1.4,1.8,.8));
        assertFalse(DomainGeometry.overlapsShell(shell,.3,0,.2,1,1.8,.8));
        assertFalse(DomainGeometry.overlapsShell(shell,1,1,.2,1.6,2.8,.8));
    }
    @Test void ordinaryPlantsRestoreExactBlockDataWithoutNativeStructurePhysics() throws Exception {
        BlockData data=stub(BlockData.class,(name,args)->{throw new AssertionError(name);});
        BlockState state=stub(BlockState.class,(name,args)->switch(name){case "getType"->Material.SHORT_GRASS;case "getBlockData"->data;default->throw new AssertionError(name);});
        Structure image=structure(state,(name,args)->{throw new AssertionError("Ordinary restoration must not call "+name);});
        boolean[] restored={false};
        Block target=stub(Block.class,(name,args)->{assertEquals("setBlockData",name);assertSame(data,args[0]);assertEquals(false,args[1]);restored[0]=true;return null;});
        DomainBlockRestorer.restore(target,image);assertTrue(restored[0]);
    }
    @Test void containerRestorationUsesOriginalFullNbtStructureWithEntitiesDisabled() throws Exception {
        BlockState state=stub(BlockState.class,(name,args)->{if(name.equals("getType"))return Material.BARREL;throw new AssertionError(name);});
        boolean[] placed={false};
        Structure image=structure(state,(name,args)->{assertEquals("place",name);assertEquals(false,args[1]);assertEquals(1f,args[5]);placed[0]=true;return null;});
        Block target=stub(Block.class,(name,args)->{assertEquals("getLocation",name);return null;});
        DomainBlockRestorer.restore(target,image);assertTrue(placed[0]);
    }
    private static boolean supported(Material material) {
        // Factual vanilla inputs supplied explicitly: Paper Material physical flags require a running registry.
        boolean gravity=material==Material.SAND || material==Material.GRAVEL;
        boolean solid=Set.of(Material.STONE,Material.DIRT,Material.GRASS_BLOCK,Material.OAK_LEAVES).contains(material);
        return DomainTerrainPolicy.supported(material,gravity,material==Material.AIR,solid,solid && material!=Material.OAK_LEAVES);
    }
    private interface Call {Object run(String name,Object[] args);}
    private static Structure structure(BlockState state,Call other) {
        Palette palette=stub(Palette.class,(name,args)->switch(name){case "getBlockCount"->1;case "getBlocks"->List.of(state);default->throw new AssertionError(name);});
        return stub(Structure.class,(name,args)->name.equals("getPalettes")?List.of(palette):other.run(name,args));
    }
    private static <T>T stub(Class<T> type,Call call){return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(proxy,method,args)->call.run(method.getName(),args)));}
}
