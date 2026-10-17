package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.whisper.WhisperArea;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import java.util.function.*;

/** One occupant per overlapping quest area; world restoration runs on the server thread. */
public final class NpcQuest implements Listener, AutoCloseable {
    public static final class Run {
        public final UUID player;
        public final NpcData.Definition npc;
        public final long deadline = System.currentTimeMillis() + 1_800_000;
        public final BossBar bar = BossBar.bossBar(Component.text("Quest • 30:00"), 1, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
        public final Set<String> remaining = new HashSet<>();
        final Map<String, NpcData.Crop> crops = new HashMap<>();
        boolean preparing;
        NpcFarm.Handle work;
        public int count;
        public long nextWave;
        public Mob mob;
        public Location spawn;
        Run(UUID player, NpcData.Definition npc) { this.player = player; this.npc = npc; }
    }
    private final JavaPlugin plugin;
    private final Supplier<Collection<NpcData.Definition>> definitions;
    private final Consumer<Run> completed;
    private final BiPredicate<Player, Location> teleport;
    private final NamespacedKey key;
    private final Map<UUID, Run> runs = new HashMap<>();
    private final NpcFarm farms = new NpcFarm();
    private static final class Repair {
        final NpcData.Definition npc;
        NpcFarm.Handle work;
        long retryAt;
        boolean warned;
        Repair(NpcData.Definition npc) { this.npc = npc; }
    }
    private final Map<UUID, Repair> repairs = new LinkedHashMap<>();
    private long ticks;
    public NpcQuest(JavaPlugin plugin, Supplier<Collection<NpcData.Definition>> definitions,
                    Consumer<Run> completed, BiPredicate<Player, Location> teleport) {
        this.plugin = plugin; this.definitions = definitions; this.completed = completed; this.teleport = teleport;
        key = new NamespacedKey(plugin, "npc_quest_mob");
    }
    public Run run(UUID player) { return runs.get(player); }
    public boolean uses(String npc) {
        return runs.values().stream().anyMatch(r -> r.npc.id().equals(npc))
                || repairs.values().stream().anyMatch(r -> r.npc.id().equals(npc));
    }
    public boolean farmBusy(WhisperArea area) {
        return farms.busy(area) || repairs.values().stream().anyMatch(r -> NpcData.overlaps(r.npc.quest().area(), area))
                || runs.values().stream().anyMatch(r -> NpcData.overlaps(r.npc.quest().area(), area));
    }
    NpcFarm.Handle scan(WhisperArea area, BooleanSupplier active, BiConsumer<Long, Long> progress,
                        Consumer<List<NpcData.Crop>> success, Consumer<String> failure) {
        return farms.scan(area, active, progress, success, failure);
    }
    /** Keep an idle field reserved until its complete snapshot has been restored. */
    public void restore(NpcData.Definition npc) {
        if (npc.quest().kind() != NpcData.QuestKind.FARM || repairs.containsKey(npc.token())) return;
        if (runs.values().stream().anyMatch(r -> NpcData.overlaps(r.npc.quest().area(), npc.quest().area()))) return;
        Repair repair = new Repair(npc); repairs.put(npc.token(), repair); repair(repair);
    }
    private void repair(Repair repair) {
        repair.work = farms.restore(repair.npc.quest(), () -> repairs.get(repair.npc.token()) == repair,
                crop -> {}, () -> repairs.remove(repair.npc.token()), reason -> {
                    repair.work = null; repair.retryAt = ticks + 200;
                    if (!repair.warned) { repair.warned = true; plugin.getLogger().warning("Ladang " + repair.npc.id() + " belum pulih: " + reason); }
                });
    }
    public boolean tool(Player p, NpcData.Quest q) {
        Material type = q.kind() == NpcData.QuestKind.FARM ? Material.IRON_HOE : Material.IRON_SWORD;
        for (ItemStack item : p.getInventory().getStorageContents()) if (item != null && item.getType() == type) return true;
        return p.getInventory().getItemInOffHand().getType() == type;
    }
    public String refusal(Player p, NpcData.Definition npc) {
        if (!tool(p, npc.quest())) return npc.quest().kind() == NpcData.QuestKind.FARM
                ? "Kamu perlu iron hoe di inventory untuk meladang." : "Bawalah iron sword untuk mengerjakan quest ini.";
        if (runs.values().stream().anyMatch(r -> NpcData.overlaps(r.npc.quest().area(), npc.quest().area())))
            return "Tempat quest sedang dipakai. Tunggu pemain sebelumnya selesai, ya.";
        if (farmBusy(npc.quest().area())) return "Ladang sedang disiapkan atau dipulihkan. Coba kembali sebentar lagi.";
        return null;
    }
    public boolean start(Player p, NpcData.Definition npc) {
        if (refusal(p, npc) != null || runs.containsKey(p.getUniqueId())) return false;
        Run run = new Run(p.getUniqueId(), npc); runs.put(run.player, run);
        if (!teleport.test(p, NpcRenderer.location(npc.quest().start()))) { runs.remove(run.player); return false; }
        p.showBossBar(run.bar);
        try { wave(run); tell(p, npc, "Quest dimulai: " + objective(npc.quest()) + ". Target " + npc.quest().goal() + "; batas waktu 30 menit. /quest quit untuk batal."); return true; }
        catch (RuntimeException e) { cancel(p.getUniqueId(), "Lokasi spawn tidak aman. Hubungi admin.", true); return false; }
    }
    private static String objective(NpcData.Quest q) {
        if (q.kind() == NpcData.QuestKind.MOB) return "kalahkan " + q.mob().toLowerCase(Locale.ROOT);
        Set<String> names = new LinkedHashSet<>();
        Set<String> plants = new LinkedHashSet<>(); for (var crop : q.crops()) plants.add(crop.plant());
        for (String plant : plants) names.add(switch (Bukkit.createBlockData(plant).getMaterial()) {
            case CARROTS -> "wortel"; case POTATOES -> "kentang"; case WHEAT -> "gandum"; case BEETROOTS -> "bit";
            default -> "tanaman";
        });
        return "panen seluruh " + String.join(", ", names) + " setiap gelombang";
    }
    private void wave(Run run) {
        run.nextWave = 0;
        if (run.npc.quest().kind() == NpcData.QuestKind.FARM) {
            run.preparing = true; run.remaining.clear(); run.crops.clear();
            Player player = Bukkit.getPlayer(run.player);
            if (player != null) tell(player, run.npc, "Menyiapkan " + run.npc.quest().crops().size() + " tanaman untuk putaran " + (run.count + 1) + ". Tunggu sampai ladang siap.");
            run.work = farms.restore(run.npc.quest(), () -> runs.get(run.player) == run,
                    crop -> { run.remaining.add(crop.key()); run.crops.put(crop.key(), crop); }, () -> {
                        if (runs.get(run.player) != run) return;
                        run.work = null; run.preparing = false;
                        Player p = Bukkit.getPlayer(run.player);
                        if (p != null) tell(p, run.npc, "Ladang siap. Panen seluruh " + run.remaining.size() + " tanaman!");
                    }, reason -> {
                        plugin.getLogger().warning("Putaran ladang " + run.npc.id() + " gagal: " + reason);
                        cancel(run.player, "Ladang belum bisa disiapkan. Hubungi admin; quest dibatalkan.", true);
                    });
        } else spawn(run);
    }
    private void spawn(Run run) {
        WhisperArea area = run.npc.quest().area(); World world = Bukkit.getWorld(area.world());
        if (world == null) throw new IllegalStateException("Dunia belum dimuat.");
        Location chosen = null; Random random = new Random();
        for (int attempt = 0; attempt < 48 && chosen == null; attempt++) {
            int x = area.minX() + random.nextInt(area.maxX() - area.minX() + 1);
            int z = area.minZ() + random.nextInt(area.maxZ() - area.minZ() + 1);
            int low = Math.max(world.getMinHeight() + 1, area.minY() - 1);
            int high = Math.min(world.getMaxHeight() - 2, area.maxY() + 2);
            for (int y = high; y >= low; y--) {
                Material floor = world.getBlockAt(x, y - 1, z).getType();
                if (world.getBlockAt(x, y, z).isPassable() && !world.getBlockAt(x, y, z).isLiquid()
                        && world.getBlockAt(x, y + 1, z).isPassable() && !world.getBlockAt(x, y + 1, z).isLiquid()
                        && floor.isSolid() && floor != Material.MAGMA_BLOCK && floor != Material.CAMPFIRE
                        && floor != Material.SOUL_CAMPFIRE && floor != Material.CACTUS) {
                    chosen = new Location(world, x + .5, y, z + .5); break;
                }
            }
        }
        if (chosen == null) throw new IllegalStateException("Tidak ada titik mob aman di area quest.");
        EntityType type = mobType(run.npc.quest().mob());
        Entity entity = world.spawnEntity(chosen, type); if (!(entity instanceof Mob mob)) { entity.remove(); throw new IllegalArgumentException("Mob tidak didukung."); }
        run.spawn = chosen; run.mob = mob;
        mob.setPersistent(false); mob.setRemoveWhenFarAway(false); mob.setCanPickupItems(false);
        mob.getPersistentDataContainer().set(key, PersistentDataType.STRING, run.player.toString());
        if (mob instanceof Enemy) { mob.setAI(false); mob.setTarget(null); }
    }
    public static EntityType mobType(String name) {
        EntityType type = EntityType.valueOf(name.toUpperCase(Locale.ROOT));
        if (type.getEntityClass() == null || !Mob.class.isAssignableFrom(type.getEntityClass()) || type == EntityType.ENDER_DRAGON)
            throw new IllegalArgumentException("Jenis mob vanilla tidak didukung.");
        return type;
    }
    private void advance(Run run) {
        run.count++;
        Player p = Bukkit.getPlayer(run.player);
        if (p != null) tell(p, run.npc, "Progres quest: " + run.count + "/" + run.npc.quest().goal());
        if (run.count >= run.npc.quest().goal()) completed.accept(run); else run.nextWave = ticks + 40;
    }
    public void retry(Run run) { run.count = Math.max(0, run.npc.quest().goal() - 1); run.nextWave = ticks + 40; }
    public void tick() {
        ticks++;
        for (Run run : List.copyOf(runs.values())) {
            Player p = Bukkit.getPlayer(run.player);
            if (p == null || p.isDead() || !p.isOnline()) { cancel(run.player, "Quest dibatalkan.", false); continue; }
            long left = run.deadline - System.currentTimeMillis();
            if (left <= 0) { cancel(run.player, "Waktu quest habis.", true); continue; }
            if (!NpcData.inside(run.npc.quest().area(), NpcRenderer.point(p.getLocation()))) {
                cancel(run.player, "Kamu keluar dari area. Quest dibatalkan.", true); continue;
            }
            if (!tool(p, run.npc.quest())) { cancel(run.player, "Alat wajib tidak ada lagi di inventory.", true); continue; }
            if (ticks % 20 == 0) {
                long seconds = (left + 999) / 1000;
                run.bar.name(Component.text("Quest " + NpcText.plain(run.npc.name()) + " • " + String.format("%02d:%02d", seconds / 60, seconds % 60)));
                run.bar.progress(Math.max(0, Math.min(1, left / 1_800_000f)));
            }
            if (run.nextWave > 0 && ticks >= run.nextWave) {
                try { wave(run); } catch (RuntimeException e) { cancel(run.player, "Spawn quest gagal; hubungi admin.", true); }
            }
            if (run.mob != null && run.mob.isValid() && !NpcData.inside(run.npc.quest().area(), NpcRenderer.point(run.mob.getLocation()))) run.mob.teleport(run.spawn);
        }
        for (Repair repair : List.copyOf(repairs.values())) if (repair.work == null && ticks >= repair.retryAt) repair(repair);
        farms.tick();
    }
    public void finish(Run run) { cleanup(run); }
    public void cancel(UUID player, String reason, boolean back) {
        Run run = runs.get(player); if (run == null) return; cleanup(run);
        Player p = Bukkit.getPlayer(player); if (p != null) {
            if (!reason.isBlank()) tell(p, run.npc, reason);
            if (back && p.isOnline() && !p.isDead()) teleport.test(p, NpcRenderer.location(run.npc.stand()));
        }
    }
    private void cleanup(Run run) {
        runs.remove(run.player); if (run.work != null) run.work.cancel(); if (run.mob != null) run.mob.remove();
        Player p = Bukkit.getPlayer(run.player); if (p != null) p.hideBossBar(run.bar);
        restore(run.npc);
    }
    private boolean protectedAt(Block block) {
        NpcData.Point p = NpcRenderer.point(block.getLocation());
        return definitions.get().stream().anyMatch(d -> d.enabled() && d.quest().kind() == NpcData.QuestKind.FARM && NpcData.inside(d.quest().area(), p))
                || runs.values().stream().anyMatch(r -> r.npc.quest().kind() == NpcData.QuestKind.FARM && NpcData.inside(r.npc.quest().area(), p))
                || repairs.values().stream().anyMatch(r -> NpcData.inside(r.npc.quest().area(), p)) || farms.contains(p);
    }
    private Run owner(Entity entity) {
        String id = entity.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (id == null) return null;
        try { return runs.get(UUID.fromString(id)); } catch (IllegalArgumentException e) { return null; }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void harvest(BlockBreakEvent e) {
        if (!protectedAt(e.getBlock())) return; e.setCancelled(true); e.setDropItems(false); e.setExpToDrop(0);
        Run r = runs.get(e.getPlayer().getUniqueId()); if (r == null || r.npc.quest().kind() != NpcData.QuestKind.FARM || r.nextWave > 0 || r.preparing) return;
        Block b = e.getBlock(); String position = b.getX() + ":" + b.getY() + ":" + b.getZ();
        NpcData.Crop crop = r.crops.get(position);
        if (!r.remaining.contains(position) || crop == null || b.getType() != Bukkit.createBlockData(crop.plant()).getMaterial()
                || !(b.getBlockData() instanceof Ageable age) || age.getAge() != age.getMaximumAge() || !tool(e.getPlayer(), r.npc.quest())) return;
        b.setType(Material.AIR, false); r.remaining.remove(position); if (r.remaining.isEmpty()) advance(r);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void damage(EntityDamageEvent e) {
        Run r = owner(e.getEntity()); if (r == null) return;
        if (!(e instanceof EntityDamageByEntityEvent hit) || !(hit.getDamager() instanceof Player p)
                || !p.getUniqueId().equals(r.player) || p.getInventory().getItemInMainHand().getType() != Material.IRON_SWORD) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void outgoing(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        if (owner(damager) != null || (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity source && owner(source) != null)) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void dead(EntityDeathEvent e) {
        Run r = owner(e.getEntity()); if (r == null) return;
        e.getDrops().clear(); e.setDroppedExp(0); r.mob = null;
        if (e.getEntity().getKiller() != null && e.getEntity().getKiller().getUniqueId().equals(r.player)) advance(r); else r.nextWave = ticks + 40;
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void interactMob(org.bukkit.event.player.PlayerInteractEntityEvent e) {
        if (e.getRightClicked().getPersistentDataContainer().has(key, PersistentDataType.STRING)) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void shear(org.bukkit.event.player.PlayerShearEntityEvent e) {
        if (e.getEntity().getPersistentDataContainer().has(key, PersistentDataType.STRING)) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void target(EntityTargetEvent e) { if (owner(e.getEntity()) != null) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void split(SlimeSplitEvent e) { if (e.getEntity().getPersistentDataContainer().has(key, PersistentDataType.STRING)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void breed(EntityBreedEvent e) { if (owner(e.getMother()) != null || owner(e.getFather()) != null) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void place(BlockPlaceEvent e) { if (protectedAt(e.getBlock())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void physical(org.bukkit.event.player.PlayerInteractEvent e) {
        if (e.getAction() == Action.PHYSICAL && e.getClickedBlock() != null && protectedAt(e.getClickedBlock())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void trample(EntityInteractEvent e) { if (protectedAt(e.getBlock())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void change(EntityChangeBlockEvent e) { if (protectedAt(e.getBlock()) || owner(e.getEntity()) != null) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void grow(BlockGrowEvent e) { if (protectedAt(e.getBlock())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void fade(BlockFadeEvent e) { if (protectedAt(e.getBlock())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void flow(BlockFromToEvent e) { if (protectedAt(e.getToBlock())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void piston(BlockPistonExtendEvent e) { if (e.getBlocks().stream().anyMatch(b -> protectedAt(b) || protectedAt(b.getRelative(e.getDirection())))) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void retract(BlockPistonRetractEvent e) { if (e.getBlocks().stream().anyMatch(b -> protectedAt(b) || protectedAt(b.getRelative(e.getDirection())))) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void explosion(EntityExplodeEvent e) { if (owner(e.getEntity()) != null) e.setCancelled(true); else e.blockList().removeIf(this::protectedAt); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void explosion(BlockExplodeEvent e) { e.blockList().removeIf(this::protectedAt); }
    private static void tell(Player p, NpcData.Definition d, String message) { p.sendMessage(NpcText.name(d.name()).append(Component.text(": " + message))); }
    @Override public void close() {
        for (UUID player : List.copyOf(runs.keySet())) cancel(player, "", false);
        farms.close(); repairs.clear(); HandlerList.unregisterAll(this);
    }
}
