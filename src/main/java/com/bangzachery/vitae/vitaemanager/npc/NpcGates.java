package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.whisper.WhisperArea;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.IOException;
import java.util.*;
import java.util.function.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import static com.bangzachery.vitae.vitaemanager.npc.NpcData.*;

/** Global, durable gate claims; every operation runs on the server thread. */
public final class NpcGates implements Listener, AutoCloseable {
    @FunctionalInterface public interface StatusWriter { void write(String id, Gate next) throws IOException; }
    @FunctionalInterface public interface Claim { void save(Gate opening) throws IOException; }
    @FunctionalInterface public interface ResetSave { void save() throws IOException; }
    private record Cell(UUID world, int x, int y, int z) { }
    private record Pick(String id, boolean first) { }
    private record Corners(String id, Cell first, Cell second) { }
    private record Suppressed(Cell cell, long until) { }
    private static final class Run {
        final String id; final Gate gate; final List<BlockDisplay> visuals = new ArrayList<>();
        int age; boolean opened, initialized;
        Run(String id, Gate gate) { this.id = id; this.gate = gate; }
    }
    private final JavaPlugin plugin;
    private final Supplier<Map<String, Definition>> definitions;
    private final StatusWriter writer;
    private final Consumer<Exception> failure;
    private final Map<String, Run> runs = new HashMap<>();
    private final Map<String,com.bangzachery.vitae.vitaemanager.selection.AreaScanner.Handle> scans=new HashMap<>();
    private static final class Change {
        final String id;final Gate gate;final boolean clearing,effects;final Iterator<GateBlock> cells;final Runnable done;
        GateBlock pending;java.util.concurrent.CompletableFuture<Chunk> loading;long since;
        Change(String id,Gate gate,boolean clearing,boolean effects,Iterator<GateBlock> cells,Runnable done){this.id=id;this.gate=gate;this.clearing=clearing;this.effects=effects;this.cells=cells;this.done=done;}
    }
    private final Deque<Change> changes=new ArrayDeque<>();
    private final Set<String> changing=new HashSet<>();
    private final Map<UUID, Pick> picks = new HashMap<>();
    private final Map<UUID, Corners> corners = new HashMap<>();
    private final Map<UUID, Suppressed> suppressed = new HashMap<>();
    private final Set<Cell> protectedCells = new HashSet<>();
    private final Set<String> reported = new HashSet<>();
    private final Set<String> repairs = new HashSet<>();
    private long ticks;
    private boolean closed;
    NpcGates(JavaPlugin plugin, Supplier<Map<String, Definition>> definitions, StatusWriter writer, Consumer<Exception> failure) {
        this.plugin = plugin; this.definitions = definitions; this.writer = writer; this.failure = failure;
    }
    public boolean busy(String id) { return runs.containsKey(id)||scans.containsKey(id)||changing.contains(id); }
    public boolean selecting(UUID player) {
        Suppressed point = suppressed.get(player);
        return picks.containsKey(player) || point != null && point.until() >= ticks;
    }
    public void start() throws IOException {
        for (Definition definition : List.copyOf(definitions.get().values())) if (definition.gate() != null
                && definition.gate().status() == GateStatus.OPENING) writer.write(definition.id(), definition.gate().withStatus(GateStatus.CLOSED));
        refresh(); Bukkit.getPluginManager().registerEvents(this, plugin);
        for (Definition definition : List.copyOf(definitions.get().values())) if (definition.gate() != null) {
            Gate gate = definition.gate();
            if (gate.status() == GateStatus.OPEN) {
                change(definition.id(),gate,true,false,()->{});
                if (gate.timing().seconds() > 0 && gate.timing().closeAt() == 0) writer.write(definition.id(), gate.opened(System.currentTimeMillis()));
            } else if (gate.status() == GateStatus.CLOSED) repairs.add(definition.id());
        }
    }
    public void refresh() {
        protectedCells.clear();
        repairs.removeIf(id -> !definitions.get().containsKey(id) || definitions.get().get(id).gate() == null);
        for (Definition definition : definitions.get().values()) if (definition.gate() != null)
            for (GateBlock block : definition.gate().blocks()) protectedCells.add(cell(definition.gate().area().world(), block));
    }
    public void arm(Player admin, String id, boolean first) {
        requireAdmin(admin); picks.put(admin.getUniqueId(), new Pick(id, first));
        NpcService.message(admin, "Pukul block sudut " + (first ? "A" : "B") + " dinding. Block tidak dihancurkan. /vnpc " + id + " gate cancel untuk batal seleksi.");
    }
    public void cancelSelection(Player admin) { picks.remove(admin.getUniqueId()); }
    public WhisperArea selectedArea(Player admin,String id){
        Corners c=corners.get(admin.getUniqueId());
        if(c==null||!c.id.equals(id)||c.first==null||c.second==null)throw new IllegalArgumentException("Pilih kedua sudut atau gunakan /vnpc "+id+" wand gate.");
        if(!c.first.world.equals(c.second.world))throw new IllegalArgumentException("Kedua sudut harus satu world.");
        return WhisperArea.between(c.first.world,c.first.x,c.first.y,c.first.z,c.second.x,c.second.y,c.second.z);
    }
    public void scan(Player admin,String id,WhisperArea area,BooleanSupplier active,Consumer<Gate> success){
        if(busy(id))throw new IllegalStateException("Gerbang sedang dipakai/dipindai.");var cells=new ArrayList<GateBlock>();
        var handle=com.bangzachery.vitae.vitaemanager.selection.SelectionService.require().scanner().scan(area,
            ()->!closed&&admin.isOnline()&&admin.hasPermission("vitae.admin.npc")&&active.getAsBoolean(),
            (read,total)->admin.sendActionBar(Component.text("Membaca gerbang "+id+": "+read+"/"+total,NamedTextColor.AQUA)),
            block->{if(block.getType().isAir())return;if(!safe(block))throw new IllegalArgumentException("Gerbang perlu dinding solid biasa; blok tidak didukung: "+block.getType());cells.add(new GateBlock(block.getX(),block.getY(),block.getZ(),block.getBlockData().getAsString()));},
            ()->{scans.remove(id);admin.sendActionBar(Component.empty());success.accept(new Gate(area,cells,GateStatus.CLOSED));},
            reason->{scans.remove(id);admin.sendActionBar(Component.empty());NpcService.message(admin,reason);}).owned(admin.getUniqueId());
        scans.put(id,handle);NpcService.message(admin,"Membaca gerbang bertahap. Tunggu pesan 'Gerbang tersimpan' sebelum on.");
    }
    private void change(String id,Gate gate,boolean clearing,boolean effects,Runnable done){
        if(!changing.add(id))return;changes.addLast(new Change(id,gate,clearing,effects,gate.blocks().iterator(),done));
    }
    private void changeTick(){
        int used=0,idle=0;long end=System.nanoTime()+2_000_000;
        while(!changes.isEmpty()&&used<128&&System.nanoTime()<end){Change c=changes.removeFirst();World w=Bukkit.getWorld(c.gate.area().world());
            try{
                if(w==null||(!c.clearing&&occupied(c.gate))){changes.addLast(c);if(++idle>=changes.size())break;continue;}
                int slice=0;boolean waiting=false;
                while((c.pending!=null||c.cells.hasNext())&&slice<16&&used<128&&System.nanoTime()<end){
                    if(c.pending==null)c.pending=c.cells.next();GateBlock cell=c.pending;
                    if(!w.isChunkLoaded(cell.x()>>4,cell.z()>>4)){
                        if(c.loading==null){c.since=System.currentTimeMillis();c.loading=w.getChunkAtAsync(cell.x()>>4,cell.z()>>4,false);}
                        else if(c.loading.isDone()){if(c.loading.join()==null)throw new IllegalStateException("Chunk gerbang belum tersedia.");c.loading=null;}
                        if(System.currentTimeMillis()-c.since>60000)throw new IllegalStateException("Chunk gerbang belum bisa dimuat.");waiting=true;break;
                    }
                    c.loading=null;Block block=w.getBlockAt(cell.x(),cell.y(),cell.z());
                    if(c.clearing){if(block.getBlockData().getAsString().equals(cell.data())&&safe(block))block.setType(Material.AIR,false);}
                    else if(block.getType().isAir())block.setBlockData(Bukkit.createBlockData(cell.data()),false);
                    if(c.effects&&slice==0&&used%32==0)w.spawnParticle(Particle.BLOCK,block.getLocation().add(.5,.5,.5),2,.15,.15,.15,Bukkit.createBlockData(cell.data()));
                    c.pending=null;slice++;used++;
                }
                if(c.pending!=null||c.cells.hasNext()){changes.addLast(c);idle=waiting?idle+1:0;if(idle>=changes.size())break;}
                else{changing.remove(c.id);if(c.effects)w.playSound(center(c.gate),"minecraft:block.stone.place",.8f,.7f);c.done.run();idle=0;}
            }catch(RuntimeException e){changing.remove(c.id);Run run=runs.get(c.id);if(run!=null)abort(run);repairs.add(c.id);failure.accept(e);}
        }
    }
    static boolean safe(Block block) {
        Material type = block.getType();
        return type.isSolid() && !type.hasGravity() && !type.isInteractable() && !block.isLiquid() && !block.isPassable() && !(block.getState() instanceof TileState)
                && !Set.of(Material.BEDROCK, Material.BARRIER, Material.END_PORTAL_FRAME, Material.STRUCTURE_VOID, Material.REINFORCED_DEEPSLATE).contains(type);
    }
    public boolean open(String id, Claim claim) throws IOException {
        Definition definition = definitions.get().get(id); Gate gate = definition == null ? null : definition.gate();
        if (closed || gate == null) throw new IllegalStateException("Gerbang belum disiapkan.");
        if (busy(id) || gate.status() != GateStatus.CLOSED) return false;
        World world = Objects.requireNonNull(Bukkit.getWorld(gate.area().world()),"Dunia gerbang belum dimuat.");
        Gate opening=gate.withStatus(GateStatus.OPENING);claim.save(opening);
        Run run=new Run(id,opening);runs.put(id,run);
        try{
            Map<String,GateBlock> expected=new HashMap<>();for(GateBlock c:gate.blocks())expected.put(c.key(),c);
            var handle=com.bangzachery.vitae.vitaemanager.selection.SelectionService.require().scanner().scan(gate.area(),()->!closed&&runs.get(id)==run,
                (read,total)->{},block->{GateBlock c=expected.get(block.getX()+":"+block.getY()+":"+block.getZ());if(c!=null&&(!block.getBlockData().getAsString().equals(c.data())||!safe(block)))throw new IllegalStateException("Dinding berubah dari snapshot. Pulihkan atau setup ulang.");},
                ()->{scans.remove(id);try{
                    for(int i=0;i<Math.min(64,gate.blocks().size());i++){GateBlock c=gate.blocks().get(i);BlockDisplay v=world.spawn(new Location(world,c.x(),c.y(),c.z()),BlockDisplay.class);run.visuals.add(v);v.setBlock(Bukkit.createBlockData(c.data()));v.setPersistent(false);v.setInvulnerable(true);v.setGravity(false);v.setTeleportDuration(1);v.setInterpolationDuration(1);}
                    run.initialized=true;world.playSound(center(gate),"minecraft:block.stone.hit",.7f,.6f);
                }catch(RuntimeException e){abort(run);failure.accept(e);}},
                reason->{scans.remove(id);abort(run);failure.accept(new IllegalStateException(reason));});scans.put(id,handle);
        }catch(RuntimeException e){abort(run);failure.accept(e);throw e;}
        return true;
    }
    public void tick() {
        if (closed) return; ticks++;changeTick();
        suppressed.entrySet().removeIf(e -> e.getValue().until() < ticks);
        for (Run run : List.copyOf(runs.values())) try {
            if(!run.initialized)continue;
            run.age++;
            if (!run.opened) {
                double amplitude = .025 + .08 * run.age / 40.0;
                for (int i = 0; i < run.visuals.size(); i++) {
                    GateBlock block = run.gate.blocks().get(i);
                    Location location = new Location(Bukkit.getWorld(run.gate.area().world()), block.x() + Math.sin(run.age * 2.4 + i) * amplitude,
                            block.y() + Math.cos(run.age * 2 + i) * amplitude / 3, block.z() + Math.cos(run.age * 2.2 + i) * amplitude);
                    if (!run.visuals.get(i).isValid() || !run.visuals.get(i).teleport(location)) throw new IllegalStateException("Visual gerbang terputus.");
                }
                if (run.age % 10 == 0) center(run.gate).getWorld().playSound(center(run.gate), "minecraft:block.stone.hit", .5f, .5f);
                if (run.age >= 40) {
                    if(changing.contains(run.id))continue;
                    run.initialized=false;
                    change(run.id,run.gate,true,false,()->{try{writer.write(run.id,run.gate.opened(System.currentTimeMillis()));run.opened=true;run.initialized=true;run.age=40;}catch(IOException e){abort(run);failure.accept(e);}});
                    World world = center(run.gate).getWorld(); world.playSound(center(run.gate), "minecraft:entity.generic.explode", .9f, .8f);
                    world.spawnParticle(Particle.EXPLOSION_EMITTER, center(run.gate), 1);
                    for (GateBlock block : run.gate.blocks().subList(0,Math.min(64,run.gate.blocks().size()))) world.spawnParticle(Particle.BLOCK,
                            new Location(world, block.x() + .5, block.y() + .5, block.z() + .5), 3, .2, .2, .2, Bukkit.createBlockData(block.data()));
                }
            } else {
                double t = (run.age - 40) / 12.0;
                for (int i = 0; i < run.visuals.size(); i++) {
                    GateBlock block = run.gate.blocks().get(i); BlockDisplay visual = run.visuals.get(i);
                    if (visual.isValid()) visual.teleport(new Location(Bukkit.getWorld(run.gate.area().world()), block.x() + Math.sin(i * 2.4) * t,
                            block.y() + 1.5 * t - 2 * t * t, block.z() + Math.cos(i * 2.4) * t));
                }
                if (run.age >= 52) remove(run);
            }
        } catch (RuntimeException e) { abort(run); failure.accept(e); }
        if (ticks % 20 == 0) autoClose(System.currentTimeMillis());
    }
    void autoClose(long now) {
        for (Definition definition : List.copyOf(definitions.get().values())) {
            Gate gate = definition.gate();
            if (gate == null || busy(definition.id())) continue;
            try {
                if (gate.status() == GateStatus.OPEN && gate.timing().seconds() > 0) {
                    long remaining = gate.timing().closeAt() - now;
                    if (remaining > 0 && remaining <= 5000) warnNearby(gate, "Gerbang akan ditutup dalam " + ((remaining + 999) / 1000) + " detik", true);
                    if (gate.due(now)) {
                        if (occupied(gate)) { warnNearby(gate, "Gerbang menunggu pemain keluar dari posisi dinding", false); continue; }
                        writer.write(definition.id(), gate.withStatus(GateStatus.CLOSING));
                        change(definition.id(),gate,false,true,()->{try{writer.write(definition.id(),gate.withStatus(GateStatus.CLOSED));}catch(IOException e){failure.accept(e);}});
                    }
                } else if (gate.status() == GateStatus.CLOSING) {
                    if (occupied(gate)) continue;
                    change(definition.id(),gate,false,false,()->{try{writer.write(definition.id(),gate.withStatus(GateStatus.CLOSED));}catch(IOException e){failure.accept(e);}});
                } else if (gate.status() == GateStatus.CLOSED && repairs.contains(definition.id()) && !occupied(gate)) {
                    change(definition.id(),gate,false,false,()->repairs.remove(definition.id()));
                }
                reported.remove(definition.id());
            } catch (IOException | RuntimeException e) {
                if (reported.add(definition.id())) failure.accept(e); // One report until the condition is resolved.
            }
        }
    }
    static boolean occupied(Gate gate) {
        World world = Bukkit.getWorld(gate.area().world());
        if (world == null) return true;
        WhisperArea a=gate.area();var bounds=new org.bukkit.util.BoundingBox(a.minX(),a.minY(),a.minZ(),a.maxX()+1.0,a.maxY()+1.0,a.maxZ()+1.0);
        for(Player p:world.getPlayers())if(p.getBoundingBox().overlaps(bounds))return true;
        return false;
    }
    private static void warnNearby(Gate gate, String text, boolean sound) {
        Location center = center(gate); if (center.getWorld() == null) return;
        for (Player player : center.getWorld().getPlayers()) if (player.getLocation().distanceSquared(center) <= 144) {
            player.sendActionBar(Component.text(text, NamedTextColor.YELLOW));
            if (sound) player.playSound(player.getLocation(), "minecraft:block.note_block.hat", .35f, 1.2f);
        }
    }
    private void abort(Run run) {
        remove(run);
        if (!run.opened) try { writer.write(run.id, run.gate.withStatus(GateStatus.CLOSED)); }
        catch (IOException | RuntimeException e) { failure.accept(e); }
    }
    private void remove(Run run) { var scan=scans.remove(run.id);if(scan!=null)scan.cancel();runs.remove(run.id); for (BlockDisplay visual : run.visuals) if (visual.isValid()) visual.remove(); }
    public void reset(String id, ResetSave saveClosed) throws IOException {
        Definition definition = definitions.get().get(id); Gate gate = definition == null ? null : definition.gate();
        if (gate == null || busy(id)) throw new IllegalArgumentException("Gerbang belum ada atau masih beranimasi.");
        World world = Objects.requireNonNull(Bukkit.getWorld(gate.area().world()),"Dunia belum dimuat.");
        for (Player player : world.getPlayers()) {
            var box = player.getBoundingBox(); WhisperArea area = gate.area();
            if (box.overlaps(new org.bukkit.util.BoundingBox(area.minX(), area.minY(), area.minZ(), area.maxX() + 1, area.maxY() + 1, area.maxZ() + 1)))
                throw new IllegalStateException("Pindahkan pemain dari area gerbang sebelum reset.");
        }
        writer.write(id, gate.withStatus(GateStatus.CLOSING));
        change(id,gate,false,true,()->{try{saveClosed.save();refresh();}catch(IOException e){failure.accept(e);}});
    }
    private static Location center(Gate gate) {
        WhisperArea area = gate.area(); return new Location(Bukkit.getWorld(area.world()), (area.minX() + area.maxX()) / 2.0 + .5,
                (area.minY() + area.maxY()) / 2.0 + .5, (area.minZ() + area.maxZ()) / 2.0 + .5);
    }
    private static Cell cell(UUID world, GateBlock block) { return new Cell(world, block.x(), block.y(), block.z()); }
    private static Cell cell(Block block) { return new Cell(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ()); }
    private boolean protectedAt(Block block) { return protectedCells.contains(cell(block)); }
    private static void requireAdmin(Player player) { if (!player.hasPermission("vitae.admin.npc")) throw new IllegalArgumentException("Perlu izin vitae.admin.npc."); }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void select(PlayerInteractEvent event) {
        Pick pick = picks.get(event.getPlayer().getUniqueId());
        if (pick == null || event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.LEFT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        event.setCancelled(true); UUID player = event.getPlayer().getUniqueId(); picks.remove(player);
        if (!event.getPlayer().hasPermission("vitae.admin.npc")) return;
        Cell point = cell(event.getClickedBlock()); Corners previous = corners.get(player);
        if (previous == null || !previous.id().equals(pick.id())) previous = new Corners(pick.id(), null, null);
        corners.put(player, pick.first() ? new Corners(pick.id(), point, previous.second()) : new Corners(pick.id(), previous.first(), point));
        suppressed.put(player, new Suppressed(point, ticks + 3));
        NpcService.message(event.getPlayer(), "Gerbang " + pick.id() + ": titik " + (pick.first() ? "A" : "B") + " = " + point.x() + ", " + point.y() + ", " + point.z() + ".");
    }
    private boolean suppressed(Player player, Block block) {
        Suppressed value = suppressed.get(player.getUniqueId());
        return picks.containsKey(player.getUniqueId()) || value != null && value.until() >= ticks && value.cell().equals(cell(block));
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void breakBlock(BlockBreakEvent event) { if (protectedAt(event.getBlock()) || suppressed(event.getPlayer(), event.getBlock())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void damageBlock(BlockDamageEvent event) { if (protectedAt(event.getBlock()) || suppressed(event.getPlayer(), event.getBlock())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void place(BlockPlaceEvent event) { if (protectedAt(event.getBlock())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void piston(BlockPistonExtendEvent event) { if (event.getBlocks().stream().anyMatch(b -> protectedAt(b) || protectedAt(b.getRelative(event.getDirection())))) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void retract(BlockPistonRetractEvent event) { if (event.getBlocks().stream().anyMatch(b -> protectedAt(b) || protectedAt(b.getRelative(event.getDirection())))) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void explosion(EntityExplodeEvent event) { event.blockList().removeIf(this::protectedAt); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void explosion(BlockExplodeEvent event) { event.blockList().removeIf(this::protectedAt); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void change(EntityChangeBlockEvent event) { if (protectedAt(event.getBlock())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void flow(BlockFromToEvent event) { if (protectedAt(event.getToBlock())) event.setCancelled(true); }
    @EventHandler public void chunk(ChunkLoadEvent event) {
        for (Definition definition : definitions.get().values()) if (definition.gate() != null
                && definition.gate().area().world().equals(event.getWorld().getUID())
                && event.getChunk().getX() >= (definition.gate().area().minX() >> 4) && event.getChunk().getX() <= (definition.gate().area().maxX() >> 4)
                && event.getChunk().getZ() >= (definition.gate().area().minZ() >> 4) && event.getChunk().getZ() <= (definition.gate().area().maxZ() >> 4)) {
            if (definition.gate().status() == GateStatus.OPEN) change(definition.id(),definition.gate(),true,false,()->{});
            else if (definition.gate().status() == GateStatus.CLOSED) repairs.add(definition.id());
        }
    }
    @EventHandler public void quit(PlayerQuitEvent event) { picks.remove(event.getPlayer().getUniqueId()); corners.remove(event.getPlayer().getUniqueId()); suppressed.remove(event.getPlayer().getUniqueId()); }
    @Override public void close() {
        closed = true;for(var h:scans.values())h.cancel();scans.clear();changes.clear();changing.clear(); for (Run run : List.copyOf(runs.values())) abort(run);
        picks.clear(); corners.clear(); suppressed.clear(); protectedCells.clear(); repairs.clear(); reported.clear(); HandlerList.unregisterAll(this);
    }
}
