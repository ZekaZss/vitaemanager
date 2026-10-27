package com.bangzachery.vitae.vitaemanager.magic.domain;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.structure.Structure;
import java.io.IOException;
import java.util.Random;

/** Ordinary blocks need no structure placement, whose native neighbour-shape updates can break plants. */
final class DomainBlockRestorer {
    private DomainBlockRestorer() {}
    static void restore(Block target, Structure snapshot) throws IOException {
        if(snapshot.getPalettes().size()!=1 || snapshot.getPalettes().getFirst().getBlockCount()!=1)
            throw new IOException("Snapshot domain harus tepat satu blok dan satu palette.");
        BlockState original=snapshot.getPalettes().getFirst().getBlocks().getFirst();
        if(DomainTerrainPolicy.container(original.getType())) {
            snapshot.place(target.getLocation(),false,StructureRotation.NONE,Mirror.NONE,0,1f,new Random(0));
        } else {
            // Every ordinary block property is in BlockData. Full NBT is still compared after restoration.
            // false uses Paper's no-physics/no-observer/no-place path, including for grass and snow.
            target.setBlockData(original.getBlockData(),false);
        }
    }
}
