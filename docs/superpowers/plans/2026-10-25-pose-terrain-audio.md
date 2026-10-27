# VitaeManager pose, terrain, roleplay and audio update

Spec: current user request sections1-5. Work directly in existing project; RULES.md, PROJECT.md, ARCHITECTURE.md read. Existing work preserved. Paper1.21.4, Java21; no new dependency, no currency/shop/player-data mutation.

- [x] Tunduk: verify supported forced pose/hitbox server API; replace GSit activation requirement with owned native pose lease and lifecycle cleanup. Keep rotation free and movement/items/attack/spell guards. Preserve old crawl recovery evidence without guessing GSit ownership. Tests for state/lifecycle without requiring GSit.
- [x] Roleplay text: inspect native passenger/teleport/transform/client placement; use one position authority without adding player height twice; configurable small head clearance, preserve existing record/API constructors. Keep true carry passengers and visibility/lifecycle. Tests for pose-height and anchor math, document client verification.
- [x] Domain: retain closed discrete sphere+write-ahead complete NBT; allow uneven solid terrain, support safe surface block structures without erasing interior or unsafe gravity/container behaviour. Specific unsupported/world-bound errors. Test flat/slope/hill/valley classification, geometry unchanged, restore journal; retain inventory traversal regression fix.
- [x] Sound (root): immutable phase sound configuration + explicit recipients/range, multiple valid vanilla cues and bat interval, legacy single cue compatibility; one lifecycle scheduler owned by use, no duplicate completion; tests for phase timing/stop and config.
- [x] Integration/review: cross-review focused changes, full test+clean build, inspect JAR and record totals/hash. Update current docs superseding previous GSit/text/terrain restrictions; never claim client/server success without direct game test.

Ownership: magic worker owns MagicService/Tunduk adapters and tests, npc_shop worker owns roleplay/config related text math and tests, domain worker owns magic/domain and tests. Root owns sound/config/resources/docs integration. Do not edit each other's files without coordination. No commits/reset.

Final evidence: clean build without cache;281 tests/45 suites,0 failures/errors/skips. JAR inspected,23 economy/NPC source hashes unchanged. Client/server visual and recovery verification remains an explicitly documented deployment checklist, not a completed test. See ../../POSE-TERRAIN-AUDIO-UPDATE.md.
