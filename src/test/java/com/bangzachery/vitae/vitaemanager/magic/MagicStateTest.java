package com.bangzachery.vitae.vitaemanager.magic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class MagicStateTest {
 @TempDir Path dir;
 @Test void ownershipIsUniqueAndLimitedAndDurable() throws Exception {
  var s=new MagicState(dir.resolve("state.properties")); var id=UUID.randomUUID();
  for(int i=0;i<9;i++) assertTrue(s.grant(id,"spell"+i));
  assertFalse(s.grant(id,"spell0")); assertFalse(s.grant(id,"tenth"));
  assertEquals(9,new MagicState(dir.resolve("state.properties")).owned(id).size());
 }
 @Test void activeLocksSurviveResetAndInterruptedUsageReceivesCooldown() throws Exception {
  var path=dir.resolve("state.properties"); var s=new MagicState(path); var id=UUID.randomUUID();
  s.begin(id,"tunduk"); s.reset(id); assertTrue(s.active(id));
  var recovered=new MagicState(path); recovered.recover(1000);
  assertFalse(recovered.active(id)); assertEquals(301000,recovered.cooldown(id));
  assertEquals(301000,new MagicState(path).cooldown(id));
 }
 @Test void finishIsIdempotentAndCooldownUsesCompletionTime() throws Exception {
  var s=new MagicState(dir.resolve("state.properties")); var id=UUID.randomUUID(); s.begin(id,"tunduk");
  s.finish(id,5000); s.finish(id,9000); assertEquals(305000,s.cooldown(id));
 }
 @Test void failedActivationClearsOnlyActiveMarkerWithoutCooldown() throws Exception {
  var path=dir.resolve("state.properties");var s=new MagicState(path);var id=UUID.randomUUID();
  s.grant(id,"tunduk");s.begin(id,"tunduk");s.abort(id);
  var loaded=new MagicState(path);assertFalse(loaded.active(id));assertEquals(0,loaded.cooldown(id));assertTrue(loaded.owned(id).contains("tunduk"));
 }
 @Test void interruptedTemporaryWriteDoesNotPublishOrReplaceAuthoritativeState() throws Exception {
  var path=dir.resolve("state.properties");var s=new MagicState(path);var id=UUID.randomUUID();s.grant(id,"tunduk");
  java.nio.file.Files.writeString(path.resolveSibling("state.properties.tmp"),"broken interrupted file");
  assertEquals(java.util.Set.of("tunduk"),new MagicState(path).owned(id));
 }
 @Test void recoveryCanBeRepeatedWithoutExtendingAlreadyStartedCooldown() throws Exception {
  var path=dir.resolve("state.properties");var s=new MagicState(path);var id=UUID.randomUUID();s.begin(id,"tunduk");s.recover(1000);
  var resumed=new MagicState(path);resumed.recover(9000);assertEquals(301000,resumed.cooldown(id));
 }
}
