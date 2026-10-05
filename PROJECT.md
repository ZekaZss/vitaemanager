# Vitaemanager remake

- Name: vitaemanager; existing server folder: plugins/vitaemanager/.
- Paper 1.21.4, Java 21, Gradle 8.14.3. Main class/package retain the original names.
- Coding proceeds one module at a time. Compilation/unit tests run during coding; Minecraft server testing is deferred until the whole remake is coded, as requested.
- Modules complete in code: foundation/config/messages, server control/admin time/weather panel, and Viti economy/physical notes.
- Commands: /vitae help, /vitae reload, /vitae panel, /adminpanel (alias /panel), /vitae maintenance on|off, /vitae chat on|off, /vitae pvp on|off [world], /vitae mobspawning on|off [world]. Console must supply a world for world commands.
- Per-action permissions: vitae.admin.reload, maintenance, chat, pvp, mobspawning, time, weather; root vitae.admin is required. Existing vitae.admin.maintenance also bypasses maintenance; OP bypasses it. Existing vitae.admin.chatbypass bypasses chat mute.
- Config validation is strict and atomic in memory; unknown/legacy settings remain unchanged on disk. Added message keys fall back to bundled defaults.
- server-control.yml is generated automatically. Existing config.yml maintenance_mode is imported once if that new state file does not exist. Afterward server-control.yml is authoritative; config.yml is left unchanged. /vitae reload only reloads config.yml.
- No carry consent, admin action audit or /vitae status is included.
- Not yet coded: profiles/invsee, roleplay/carry, totems, item rules/integrations. Do not install this development JAR as a complete replacement on the live server.
- viti.yml, blocker/blocked.yml and player/item PDC are untouched in Modules 1-2.
- Preserve namespace vitaemanager and keys viti_balance, viti_paper_amount, is_frozen, is_vanished, totem_count, totem_cooldown in the corresponding future modules.
- Old code belongs outside src in legacy-reference; the new singular command package is command, not commands.
- Build: ./gradlew clean build or .\gradlew.bat clean build.

- Viti commands: /viti lihat, beri nominal, convert nominal, board, add|set|remove player nominal, reload and help. Aliases uang/money are preserved. Public permission vitae.viti defaults true; vitae.admin.viti defaults op and is inherited by vitae.admin.
- /viti board reports historic peak, not current balance. set permits zero; add/remove/transfer/convert require positive amounts. Remove refuses insufficient funds. Exact online player names are required for admin targets.
- Existing viti.yml and DOUBLE player/paper PDC values are imported. New exact fields avoid decimal rounding and retain numeric mirrors. No bundled sample player is copied into a fresh file.
- Uang fisik baru: one UUID per note, redeemable once. Legacy paper redemption seals and redeems the total value of the held stack. Right-click with main hand. Anvil output for Viti paper is blocked.
- viti.yml.before-remake is created before the first migration write. Pending minted notes and redeemed IDs are persisted; freeslot/offline recovery is part of this monetary implementation, not an extra admin audit/consent/status feature.
- Manual viti.yml edits after migration should use balance_exact/highest_balance_exact, then /viti reload. The exact fields are authoritative.
- Minecraft runtime, native playerdata persistence, interaction, shutdown/crash, cross-plugin aliases and performance remain to be tested after all coding modules are finished.
