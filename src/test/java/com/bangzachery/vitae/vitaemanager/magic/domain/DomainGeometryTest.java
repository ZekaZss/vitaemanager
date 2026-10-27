package com.bangzachery.vitae.vitaemanager.magic.domain;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class DomainGeometryTest {
    @Test void radiusThirtyShellHasBothPolesAndNoSixNeighbourEscape() {
        int r=30;Set<DomainGeometry.Cell> shell=Set.copyOf(DomainGeometry.shell(r));
        assertTrue(shell.contains(new DomainGeometry.Cell(0,-r,0)));assertTrue(shell.contains(new DomainGeometry.Cell(0,r,0)));
        assertFalse(shell.contains(new DomainGeometry.Cell(0,0,0)));
        Set<DomainGeometry.Cell> visited=new HashSet<>();Deque<DomainGeometry.Cell> queue=new ArrayDeque<>();queue.add(new DomainGeometry.Cell(0,0,0));
        int[][] directions={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        while(!queue.isEmpty()) {var c=queue.remove();if(!visited.add(c))continue;assertTrue(DomainGeometry.inside(c.x(),c.y(),c.z(),r));for(var d:directions){var next=new DomainGeometry.Cell(c.x()+d[0],c.y()+d[1],c.z()+d[2]);if(!shell.contains(next)&&!visited.contains(next))queue.add(next);}}
        assertTrue(visited.size()>90_000);
    }
    @Test void shellIsUniqueBottomToTopAndOnlyBoundaryIsReplaced(){var cells=DomainGeometry.shell(30);assertEquals(cells.size(),new HashSet<>(cells).size());int previous=-31;for(var c:cells){assertTrue(c.y()>=previous);previous=c.y();assertTrue(DomainGeometry.inside(c.x(),c.y(),c.z(),30));}assertThrows(IllegalArgumentException.class,()->DomainGeometry.shell(1));}
}
