package com.bangzachery.vitae.vitaemanager.evolution;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class EvolutionGridTest {
    @Test void eightMaterialsSurroundWeaponAndNeverIncludeTheLockedResult() {
        Set<Integer> slots=new HashSet<>();
        for(int i=0;i<8;i++){int raw=EvolutionGrid.rawSlot(i);assertTrue(slots.add(raw));assertNotEquals(5,raw);assertEquals(i,EvolutionGrid.index(raw));}
        assertEquals(8,EvolutionGrid.index(5));assertEquals(-1,EvolutionGrid.index(0));assertEquals(-1,EvolutionGrid.index(10));
    }
    @Test void partialStacksNeverConsumeMoreThanRequiredOrOffered() {
        assertEquals(3,EvolutionGrid.accepted(4,1,64,false));assertEquals(1,EvolutionGrid.accepted(4,1,64,true));
        assertEquals(0,EvolutionGrid.accepted(4,4,64,false));assertEquals(2,EvolutionGrid.accepted(64,0,2,false));
        assertThrows(IllegalArgumentException.class,()->EvolutionGrid.accepted(4,5,1,false));
    }
    @Test void weaponDoesNotUnlockWithIncompleteMaterials() {
        var altar=EvolutionStoreTest.altar();assertFalse(EvolutionGrid.materialsComplete(altar,Map.of()));
        assertFalse(EvolutionGrid.materialsComplete(altar,Map.of(0,3)));assertTrue(EvolutionGrid.materialsComplete(altar,Map.of(0,4)));
        assertFalse(EvolutionGrid.materialsComplete(altar,Map.of(0,5)));
    }
}
