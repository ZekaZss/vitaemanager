package com.bangzachery.vitae.vitaemanager.magic;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.persistence.*;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.lang.reflect.*;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class InternalCrawlPoseTest {
 @TempDir Path dir;
 @Test void forcesRealFixedSwimmingStateAndRestoresOnlyOwnedState()throws Exception{
  var fixture=new Subject();var journal=new VictimJournal(dir.resolve("players.properties"));var pose=new InternalCrawlPose(journal);
  pose.acquire(fixture.player,UUID.randomUUID());assertEquals(Pose.SWIMMING,fixture.pose);assertTrue(fixture.fixed);assertTrue(fixture.swimming);assertEquals(.6,fixture.height);
  pose.release(fixture.player);assertEquals(Pose.STANDING,fixture.pose);assertFalse(fixture.fixed);assertFalse(fixture.swimming);assertTrue(journal.records(fixture.id).isEmpty());
  pose.release(fixture.player);assertEquals(2,fixture.poseChanges);
 }
 @Test void preexistingFixedPoseIsRejectedBeforeAnyChange()throws Exception{
  var f=new Subject();f.fixed=true;f.pose=Pose.SLEEPING;var pose=new InternalCrawlPose(new VictimJournal(dir.resolve("p.properties")));
  assertThrows(IllegalStateException.class,()->pose.acquire(f.player,UUID.randomUUID()));assertEquals(0,f.poseChanges);assertEquals(Pose.SLEEPING,f.pose);
 }
 @Test void externalPoseTakesOwnershipAndCleanupDoesNotOverrideIt()throws Exception{
  var f=new Subject();var pose=new InternalCrawlPose(new VictimJournal(dir.resolve("p.properties")));pose.acquire(f.player,UUID.randomUUID());
  pose.externalChange(f.player);f.pose=Pose.SNEAKING;f.fixed=true;pose.release(f.player);assertEquals(Pose.SNEAKING,f.pose);assertTrue(f.fixed);
 }
 @Test void journalCanRecoverMatchingTokenAfterServiceRestartWithoutGSit()throws Exception{
  var f=new Subject();Path path=dir.resolve("p.properties");var first=new InternalCrawlPose(new VictimJournal(path));first.acquire(f.player,UUID.randomUUID());
  var restarted=new InternalCrawlPose(new VictimJournal(path));restarted.recover(f.player);assertEquals(Pose.STANDING,f.pose);assertFalse(f.fixed);assertFalse(f.swimming);assertTrue(new VictimJournal(path).records(f.id).isEmpty());
 }
 @Test void completeServerRestartDoesNotOverwriteNewNativePose()throws Exception{
  var f=new Subject();Path path=dir.resolve("p.properties");new InternalCrawlPose(new VictimJournal(path)).acquire(f.player,UUID.randomUUID());
  f.fixed=false;f.pose=Pose.SNEAKING;f.swimming=false;new InternalCrawlPose(new VictimJournal(path)).recover(f.player);assertEquals(Pose.SNEAKING,f.pose);assertFalse(f.fixed);
 }
 @Test void unchangedStandingHitboxFailsAndRollsBackInsteadOfPretendingToCrawl()throws Exception{
  var f=new Subject();f.supported=false;var pose=new InternalCrawlPose(new VictimJournal(dir.resolve("p.properties")));assertThrows(IllegalStateException.class,()->pose.acquire(f.player,UUID.randomUUID()));assertFalse(f.fixed);assertFalse(f.swimming);assertEquals(Pose.STANDING,f.pose);
 }
 @Test void cleanupPreservesPreexistingNaturalSwimmingState()throws Exception{
  var f=new Subject();f.swimming=true;f.pose=Pose.SWIMMING;f.height=.6;var pose=new InternalCrawlPose(new VictimJournal(dir.resolve("p.properties")));pose.acquire(f.player,UUID.randomUUID());pose.release(f.player);assertTrue(f.swimming);assertFalse(f.fixed);assertEquals(Pose.SWIMMING,f.pose);
 }
 @Test void deathCleanupReleasesFixedStateWithoutForcingStandingOverDeathPose()throws Exception{
  var f=new Subject();var pose=new InternalCrawlPose(new VictimJournal(dir.resolve("p.properties")));pose.acquire(f.player,UUID.randomUUID());f.dead=true;pose.release(f.player);assertEquals(Pose.DYING,f.pose);assertFalse(f.fixed);
 }
 @Test void dyingPoseEventBeforeDeathEventPreservesOwnedCleanupEvidence()throws Exception{
  var f=new Subject();var journal=new VictimJournal(dir.resolve("p.properties"));var pose=new InternalCrawlPose(journal);pose.acquire(f.player,UUID.randomUUID());
  pose.externalChange(f.player,Pose.DYING);assertFalse(journal.records(f.id).isEmpty());
  f.pose=Pose.DYING;f.fixed=true;pose.check(f.player);assertFalse(journal.records(f.id).isEmpty());f.dead=true;pose.release(f.player);assertEquals(Pose.DYING,f.pose);assertFalse(f.fixed);assertFalse(f.swimming);assertTrue(journal.records(f.id).isEmpty());
 }
 @Test void cleaningOneVictimDoesNotReleaseOtherVictims()throws Exception{
  var a=new Subject();var b=new Subject();var pose=new InternalCrawlPose(new VictimJournal(dir.resolve("p.properties")));UUID use=UUID.randomUUID();pose.acquire(a.player,use);pose.acquire(b.player,use);pose.release(a.player);assertFalse(a.fixed);assertTrue(b.fixed);assertTrue(pose.holdsSwimming(b.player));
 }
 @Test void mismatchedPersistentTokenFailsClosedAndRetainsRecoveryEvidence()throws Exception{
  var f=new Subject();Path path=dir.resolve("p.properties");new InternalCrawlPose(new VictimJournal(path)).acquire(f.player,UUID.randomUUID());f.data.replaceAll((key,value)->"foreign-token");
  var recovery=new InternalCrawlPose(new VictimJournal(path));assertThrows(IllegalStateException.class,()->recovery.recover(f.player));assertTrue(f.fixed);assertFalse(new VictimJournal(path).records(f.id).isEmpty());
 }
 @Test void journalWriteFailureRejectsBeforeChangingPoseSwimmingOrPdc()throws Exception{
  Path parent=dir.resolve("not-a-directory");java.nio.file.Files.writeString(parent,"keep");var f=new Subject();var pose=new InternalCrawlPose(new VictimJournal(parent.resolve("pose.properties")));assertThrows(IllegalStateException.class,()->pose.acquire(f.player,UUID.randomUUID()));assertEquals(0,f.poseChanges);assertFalse(f.fixed);assertFalse(f.swimming);assertTrue(f.data.isEmpty());
 }
 static final class Subject implements InvocationHandler {
  final UUID id=UUID.randomUUID();Pose pose=Pose.STANDING;boolean fixed,swimming,dead,supported=true;double height=1.8;int poseChanges;final Map<Object,Object> data=new HashMap<>();
  final PersistentDataContainer pdc=(PersistentDataContainer)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{PersistentDataContainer.class},(p,m,a)->{return switch(m.getName()){case "get"->data.get(a[0]);case "set"->{data.put(a[0],a[2]);yield null;}case "remove"->{data.remove(a[0]);yield null;}case "has"->data.containsKey(a[0]);default->null;};});
  final Player player=(Player)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Player.class},this);
  public Object invoke(Object proxy,Method method,Object[] args){return switch(method.getName()){
   case "getUniqueId"->id;case "getPose"->pose;case "hasFixedPose"->fixed;case "isSwimming"->swimming;
   case "setSwimming"->{swimming=(Boolean)args[0];yield null;}
   case "setPose"->{pose=(Pose)args[0];fixed=(Boolean)args[1];height=supported&&pose==Pose.SWIMMING?.6:1.8;poseChanges++;yield null;}
   case "getBoundingBox"->new BoundingBox(0,0,0,.6,height,.6);case "getHeight"->height;
   case "getPersistentDataContainer"->pdc;case "isDead"->dead;case "isInsideVehicle"->false;case "getPassengers"->List.of();case "getAttribute"->null;
   case "toString"->"pose-subject";case "hashCode"->id.hashCode();case "equals"->proxy==args[0];default->null;
  };}
 }
}
