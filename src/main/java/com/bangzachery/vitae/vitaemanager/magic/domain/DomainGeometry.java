package com.bangzachery.vitae.vitaemanager.magic.domain;

import java.util.*;

/** The six-neighbour boundary of a discrete solid ball. No axial path can escape through a hole. */
public final class DomainGeometry {
    private DomainGeometry() {}
    public record Cell(int x, int y, int z) {}
    /** Strict overlap: touching a block face alone does not put the player's body inside that block. */
    public static boolean overlapsShell(Set<Cell> shell,double minX,double minY,double minZ,double maxX,double maxY,double maxZ) {
        for(int x=(int)Math.floor(minX);x<=Math.floor(Math.nextDown(maxX));x++)
            for(int y=(int)Math.floor(minY);y<=Math.floor(Math.nextDown(maxY));y++)
                for(int z=(int)Math.floor(minZ);z<=Math.floor(Math.nextDown(maxZ));z++)
                    if(shell.contains(new Cell(x,y,z)))return true;
        return false;
    }
    public static boolean inside(int x, int y, int z, int radius) {
        return (long)x*x + (long)y*y + (long)z*z <= (long)radius*radius;
    }
    public static List<Cell> shell(int radius) {
        if (radius < 2 || radius > 64) throw new IllegalArgumentException("Radius domain harus 2–64.");
        List<Cell> cells = new ArrayList<>();
        for (int y=-radius;y<=radius;y++) for(int x=-radius;x<=radius;x++) for(int z=-radius;z<=radius;z++) {
            if (!inside(x,y,z,radius)) continue;
            if (!inside(x+1,y,z,radius)||!inside(x-1,y,z,radius)||!inside(x,y+1,z,radius)
                    ||!inside(x,y-1,z,radius)||!inside(x,y,z+1,radius)||!inside(x,y,z-1,radius)) cells.add(new Cell(x,y,z));
        }
        return List.copyOf(cells);
    }
}
