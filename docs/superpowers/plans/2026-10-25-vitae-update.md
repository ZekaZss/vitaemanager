# VitaeManager feature update implementation plan

Goal: Implement the user's A–U brief in the existing IntelliJ project; build its Gradle JAR.
Architecture: Reuse VitiAmount/VitiService/VitiLedger and existing NPC rendering/commands. Add a magic feature package with UUID state and durable domain recovery. Do not relocate monetary state or run currency conversion.
Tech stack: Java 21, Paper 1.21.4, Gradle 9.7.0; optional PAPI, LuckPerms, GSit.
Spec: User brief A–U in this session. rules.md and AGENTS.md absent (reported); PROJECT.md and ARCHITECTURE.md read, older module completion notes are stale.

Constraints: Preserve staged PANDUAN-SETUP.md, commands, namespaces, existing account data, exact fractions. Work directly in requested project. No replacement project, ZIP, mana, RP sanctions, mandatory TAB/ItemsAdder. No in-game claims without runtime evidence.

Review focus: interrupted migration and conflicting copies; crash between inventory/ledger saves; tile entities and multiblock restoration; owner versus victim lifecycle; optional integration absence and startup recovery.

- [x] Task 1 (root): classify existing paths; test copy-first migration retry/conflict/backup; wire categorized feature files. Keep config.yml, viti.yml and its dependent markers/backups in place.
- [x] Task 2 (root): remove carry names only; retain mechanisms and release feedback. Add tested VitiAmount short formatting; optional persistent PAPI expansion using current in-memory ledger; exact transaction displays retained.
- [x] Task 3 (shop worker): inspect existing NPC/economy; add NPC shop catalogs/editor/template/amount confirmation and optimistic edits; receipt-backed official ledger transaction and durable inventory evidence; preserve original NPC behavior and isolated clone identity. Test pricing, receipts and interrupted recovery.
- [x] Task 4 (domain worker): implement sphere geometry and write-ahead block snapshots with durable progress and startup recovery; validate closure, world bounds, complex blocks; protect tracked shell. Tests prove geometry, durable retry and incomplete records fail closed. Expose API to magic worker before integrating.
- [x] Task 5 (magic worker): UUID ownership <=9, commands/admin/offline known identity, 9-slot GUIs, shared active/cooldown durable state, configurable spells/sounds, LuckPerms inherited membership/GSit API validation, Telekinesis/Tunduk/domain phases and lifecycle. Use Task 4 recovery API. Tests for ownership/cooldown and interrupted sessions.
- [x] Task 6 (root): integrate lifecycle/dependency metadata, inspect cross-module guards; review workers' code and fix concrete gaps. Run full Gradle test/build; count executed tests; inspect JAR resources/classes. Document data layout/install/configuration and explicit untested game behavior.

Verification: workers run targeted tests with --no-configuration-cache; root runs .\gradlew.bat clean build, parses build/test-results/test/TEST-*.xml, and inspects build/libs JAR. No commits/reset/staging of user changes.

Review decisions: preserve monetary file group; completed layout migration cannot restore missing destination from stale source; recovered shop inventory is reverified and account held; domain uses Paper save(true) flush before completion; magic preflight reservation does not count as use. Runtime/client verification remains separate.

Final evidence: clean build successful; 227 tests, 0 failures/errors/skips; JAR integrity/resources/Java21 verified. See docs/BUILD-VERIFICATION-2026-10-25.md. No Minecraft runtime test performed.
