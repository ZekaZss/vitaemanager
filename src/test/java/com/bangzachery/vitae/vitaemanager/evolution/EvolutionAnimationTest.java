package com.bangzachery.vitae.vitaemanager.evolution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionAltar.*;

class EvolutionAnimationTest {
    List<EvolutionAnimation.Vec> origins(){return List.of(new EvolutionAnimation.Vec(3,0,0),new EvolutionAnimation.Vec(-3,0,0));}
    @ParameterizedTest @EnumSource(Preset.class)
    void eachPresetStartsAtPedestalsAndFinishesInsideTheWeapon(Preset preset) {
        assertEquals(origins(),EvolutionAnimation.frame(preset,0,0,origins()).ingredients());
        var end=EvolutionAnimation.frame(preset,1,180,origins());
        for(var p:end.ingredients()) {assertEquals(end.weapon().x(),p.x(),1e-10);assertEquals(end.weapon().y(),p.y(),1e-10);assertEquals(end.weapon().z(),p.z(),1e-10);}
        assertEquals(Stage.FINISH,end.stage());
    }
    @ParameterizedTest @EnumSource(Preset.class)
    void nineSecondTimelineHasFinitePositions(Preset preset) {
        for(int ticks=0;ticks<=180;ticks+=2) {
            var frame=EvolutionAnimation.frame(preset,ticks/180.0,ticks,origins());assertTrue(Double.isFinite(frame.rotation()));
            for(var p:frame.ingredients())assertTrue(Double.isFinite(p.x())&&Double.isFinite(p.y())&&Double.isFinite(p.z()));
        }
    }
    @Test void newPresetsHaveDifferentPaths() {
        Set<List<EvolutionAnimation.Vec>> paths=new HashSet<>();
        for(Preset p:List.of(Preset.ASTRAL,Preset.FROST,Preset.PHOENIX))assertTrue(paths.add(EvolutionAnimation.frame(p,.5,90,origins()).ingredients()));
    }
    @Test void invalidTimingSoundAndLightningRangesAreRejected() {
        assertThrows(IllegalArgumentException.class,()->new Music("vitae:fire",false,1,99));
        assertThrows(IllegalArgumentException.class,()->new Music("file.wav",false,1,180));
        assertThrows(IllegalArgumentException.class,()->new Effects(true,1,3,true));
        assertThrows(IllegalArgumentException.class,()->new Effects(true,30,9,true));
        assertThrows(IllegalArgumentException.class,()->EvolutionAnimation.frame(Preset.VOID,Double.NaN,0,origins()));
    }
}
