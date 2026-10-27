package com.bangzachery.vitae.vitaemanager.magic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class VictimJournalTest {
 @TempDir Path root;
 @Test void writeAheadRecordSurvivesRestartAndRemovingOneDoesNotRemoveAnother()throws Exception{
  Path path=root.resolve("players.properties");var j=new VictimJournal(path);UUID a=UUID.randomUUID(),b=UUID.randomUUID();j.put(a,"resistance","1;none");j.put(b,"darkness","0;none");
  var loaded=new VictimJournal(path);assertEquals("1;none",loaded.records(a).get("resistance"));loaded.remove(a,"resistance");
  assertTrue(new VictimJournal(path).records(a).isEmpty());assertEquals("0;none",new VictimJournal(path).records(b).get("darkness"));
 }
 @Test void checkpointUpdatesRemainingEffectAndKeepsOtherOfflineVictimEvidence()throws Exception{
  Path path=root.resolve("players.properties");var j=new VictimJournal(path);UUID a=UUID.randomUUID(),b=UUID.randomUUID();j.put(a,"slowness","2;0;100;false;true;true");j.put(b,"nausea","0;none");
  j.checkpoint(Map.of(a+".slowness","2;0;80;false;true;true"));var loaded=new VictimJournal(path);assertTrue(loaded.records(a).get("slowness").contains(";80;"));assertFalse(loaded.records(b).isEmpty());
 }
 @Test void interruptedTempWriteLeavesOriginalEvidenceAuthoritative()throws Exception{
  Path path=root.resolve("players.properties");var j=new VictimJournal(path);UUID a=UUID.randomUUID();j.put(a,"darkness","0;none");Files.writeString(root.resolve("players.properties.tmp"),"partial");assertEquals("0;none",new VictimJournal(path).records(a).get("darkness"));
 }
 @Test void corruptRecoveryFileFailsClosedWithoutRewritingEvidence()throws Exception{
  Path path=root.resolve("players.properties");String broken="victim=\\uINVALID";Files.writeString(path,broken);
  assertThrows(IllegalArgumentException.class,()->new VictimJournal(path));assertEquals(broken,Files.readString(path));
 }
}
