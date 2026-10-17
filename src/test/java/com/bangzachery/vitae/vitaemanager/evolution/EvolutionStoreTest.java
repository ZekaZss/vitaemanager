package com.bangzachery.vitae.vitaemanager.evolution;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionAltar.*;
import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionStore.*;

class EvolutionStoreTest {
    @TempDir Path directory;
    static final UUID WORLD=UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID OWNER=UUID.fromString("22222222-2222-2222-2222-222222222222");
    static final String ITEM="AQ==";
    static EvolutionAltar altar() {
        return new EvolutionAltar("api",new Point(WORLD,0,64,0),ITEM,"Ag==",
                List.of(new Ingredient(ITEM,4,new Point(WORLD,2,64,0))),Set.of(OWNER),
                Preset.INFERNO,180,1,24,24,9,0,true,Map.of(),
                new Music("vitae:ritual/fire",false,.8f,180),new Effects(true,30,3,true),true);
    }
    static Set<String> sounds() {
        Set<String> result=new HashSet<>();
        for(Preset preset:Preset.values()) {
            var a=new EvolutionAltar("test",altar().table(),"","",List.of(),Set.of(),preset,180,1,24,24,9,0,false,Map.of());
            for(Stage stage:Stage.values()) result.add(a.cue(stage).sound());
        }
        result.add("minecraft:music_disc.pigstep"); return result;
    }
    EvolutionStore store(){return new EvolutionStore(directory.resolve("evolution.yml"),sounds());}
    static State initial(){return new State(Map.of("api",altar()),Map.of());}
    @Test void settingsDepositTransferAndRecoverySurviveAtomicRestart() throws Exception {
        Deposit d=new Deposit(UUID.randomUUID(),OWNER,"api",altar().table(),Map.of(0,ITEM),
                new Transfer(UUID.randomUUID(),ITEM,Map.of(0,ITEM,8,ITEM)));
        State state=initial().deposit(d);store().save(state);
        assertEquals(state,store().initialize()); assertEquals(2,store().document(state).getInt("state-version"));
        assertEquals(180,store().load().altars().get("api").ticks());
        assertEquals("vitae:ritual/fire",store().load().altars().get("api").music().sound());
    }
    @Test void legacyConfigurationAndPendingTicketsMigrateWithoutActivatingLethalOrMusic() throws Exception {
        Ticket t=new Ticket(UUID.randomUUID(),OWNER,"api",altar().table(),Phase.ACTIVE,List.of(ITEM),ITEM);
        YamlConfiguration yaml=store().document(initial().prepare(t));
        yaml.set("state-version",1);yaml.set("deposits",null);
        for(String field:List.of("music","effects","lethal"))yaml.set("altars.api."+field,null);
        State migrated=store().parse(yaml);
        assertFalse(migrated.altars().get("api").lethal());assertEquals("",migrated.altars().get("api").music().sound());
        assertEquals(t,migrated.tickets().get(OWNER));assertEquals(altar().ingredients(),migrated.altars().get("api").ingredients());
        store().save(migrated);assertEquals(migrated,store().load());
    }
    @Test void malformedAudioAndUnsafeEffectsNeverReplaceSavedSnapshot() throws Exception {
        store().save(initial());byte[] before=Files.readAllBytes(directory.resolve("evolution.yml"));
        Map<String,Object> invalid=Map.of("music.volume",2.0,"music.private","yes","music.sound","minecraft:no_such_sound",
                "effects.interval-ticks",1,"effects.radius",9,"lethal",1,"duration",Double.NaN);
        for(var entry:invalid.entrySet()) {
            var yaml=store().document(initial());yaml.set("altars.api."+entry.getKey(),entry.getValue());
            assertThrows(IllegalArgumentException.class,()->store().parse(yaml),entry.getKey());
        }
        assertArrayEquals(before,Files.readAllBytes(directory.resolve("evolution.yml")));
    }
    @Test void corruptSectionsUnknownVersionAndNoncanonicalSlotsAreRejected() {
        for(String path:List.of("deposits","altars.api.effects","altars.api.music")) {
            var yaml=store().document(initial());yaml.set(path,"wrong");assertThrows(IllegalArgumentException.class,()->store().parse(yaml));
        }
        var yaml=store().document(initial());yaml.set("state-version",3);assertThrows(IllegalArgumentException.class,()->store().parse(yaml));
        Deposit d=new Deposit(UUID.randomUUID(),OWNER,"api",altar().table(),Map.of(0,ITEM),null);
        var slots=store().document(initial().deposit(d));slots.set("deposits."+OWNER+".items.00",ITEM);
        assertThrows(IllegalArgumentException.class,()->store().parse(slots));
    }
    @Test void missingReloadNeverCreatesAnEmptyReplacement() throws Exception {
        assertEquals(new State(Map.of(),Map.of()),store().initialize());Files.delete(directory.resolve("evolution.yml"));
        assertThrows(java.io.IOException.class,()->store().load());assertFalse(Files.exists(directory.resolve("evolution.yml")));
    }
    @Test void unicodeAndAllLineEndingsRemainReadable() throws Exception {
        store().save(initial());String text=Files.readString(directory.resolve("evolution.yml"));
        for(String newline:List.of("\n","\r\n","\r")) {
            Files.writeString(directory.resolve("evolution.yml"),text.replace("\n",newline));assertEquals(initial(),store().load());
        }
    }
}
