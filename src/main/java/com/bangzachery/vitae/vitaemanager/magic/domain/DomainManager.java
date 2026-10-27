package com.bangzachery.vitae.vitaemanager.magic.domain;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.structure.Structure;
import org.bukkit.util.BlockVector;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiPredicate;

/** Bukkit world access is confined to the server thread, with a bounded 128 blocks per tick. */
public final class DomainManager implements Listener, AutoCloseable {
    private static final List<org.bukkit.block.BlockFace> FACES=List.of(org.bukkit.block.BlockFace.UP,org.bukkit.block.BlockFace.DOWN,org.bukkit.block.BlockFace.NORTH,org.bukkit.block.BlockFace.SOUTH,org.bukkit.block.BlockFace.EAST,org.bukkit.block.BlockFace.WEST);
    private final JavaPlugin plugin;
    private final Path directory;
    private final List<Handle> handles=new ArrayList<>();
    private final Set<UUID> recoveryOwners=new HashSet<>();
    private BukkitTask task;
    private boolean failedRecovery;
    private BiPredicate<Player,Location> evacuationTeleport=Player::teleport;
    public DomainManager(JavaPlugin plugin, Path directory){this.plugin=plugin;this.directory=directory;}
    public void setEvacuationTeleport(BiPredicate<Player,Location> strategy){this.evacuationTeleport=Objects.requireNonNull(strategy);}
    public void start() throws IOException {
        requireThread();Files.createDirectories(directory);
        plugin.getServer().getPluginManager().registerEvents(this,plugin);
        try(var files=Files.list(directory)) {
            for(Path file:files.filter(p->p.toString().endsWith(".snapshot")).toList()) {
                try {
                    DomainJournal.Image image=DomainJournal.load(file);
                    if(Files.exists(marker(image.id(),"complete")))continue;
                    Handle handle=new Handle(image); handles.add(handle); recoveryOwners.add(image.owner());handle.restoring=true;handle.restoreTarget=handle.cells.size();
                } catch(Exception ex){failedRecovery=true;plugin.getLogger().severe("Pemulihan domain diblokir, journal dipertahankan: "+file+": "+ex.getMessage());}
            }
        }
        task=plugin.getServer().getScheduler().runTaskTimer(plugin,()->{for(Handle h:List.copyOf(handles))h.tick(128);handles.removeIf(h->h.complete);},1L,1L);
    }
    public Set<UUID> recoveryOwners(){return Set.copyOf(recoveryOwners);}
    public boolean recovering(){return failedRecovery||handles.stream().anyMatch(h->h.restoring&&!h.complete);}
    public Handle prepare(Player player,int radius) {
        requireThread();
        if(recovering())throw new IllegalStateException("Pemulihan domain sebelumnya belum selesai; periksa journal dan log server.");
        Location center=player.getLocation().toBlockLocation();World w=center.getWorld();
        if(center.getBlockY()-radius<w.getMinHeight()||center.getBlockY()+radius>=w.getMaxHeight())throw new IllegalArgumentException("Bola domain melewati batas tinggi world.");
        Handle handle=new Handle(player.getUniqueId(),center,radius);
        for(Handle other:handles)if(!other.complete&&other.world.equals(handle.world)&&other.center().distanceSquared(center)<=Math.pow(other.radius+radius+2,2))throw new IllegalArgumentException("Domain bertumpuk dengan domain yang belum dipulihkan.");
        handle.checkPlayers(false); handles.add(handle);
        for(Player viewer:w.getPlayers())if(inventoryProtected(viewer.getOpenInventory().getTopInventory()))viewer.closeInventory();
        return handle;
    }
    private Path marker(UUID id,String suffix){return directory.resolve(id+"."+suffix);}
    private static void requireThread(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Domain world access requires server thread");}
    public final class Handle {
        private final UUID id,owner,world;
        private final int x,y,z,radius;
        private final List<DomainGeometry.Cell> cells;
        private final Set<DomainGeometry.Cell> shell;
        private final List<byte[]> snapshots=new ArrayList<>();
        private int capture,build,restore,buildTarget,restoreTarget;
        private boolean ready,restoring,complete,moved;
        private String failure;
        private Handle(UUID owner,Location center,int radius){this.id=UUID.randomUUID();this.owner=owner;this.world=center.getWorld().getUID();x=center.getBlockX();y=center.getBlockY();z=center.getBlockZ();this.radius=radius;cells=DomainGeometry.shell(radius);shell=Set.copyOf(cells);}
        private Handle(DomainJournal.Image image){id=image.id();owner=image.owner();world=image.world();x=image.x();y=image.y();z=image.z();radius=image.radius();cells=DomainGeometry.shell(radius);shell=Set.copyOf(cells);snapshots.addAll(image.blocks());capture=cells.size();ready=true;}
        public UUID owner(){return owner;}
        public boolean ready(){return ready&&failure==null;}
        public String failedReason(){return failure;}
        public int radius(){return radius;}
        public Location center(){return new Location(Bukkit.getWorld(world),x,y,z);}
        public boolean contains(Location location){return location.getWorld()!=null&&location.getWorld().getUID().equals(world)&&location.distanceSquared(center())<=(double)radius*radius;}
        public boolean fullyRestored(){return complete;}
        public boolean fullyBuilt(){return build==cells.size();}
        public void buildFraction(double fraction){requireThread();if(restoring||!ready())return;buildTarget=Math.max(buildTarget,(int)Math.ceil(cells.size()*Math.clamp(fraction,0,1)));}
        public void restoreFraction(double fraction){requireThread();restoring=true;restoreTarget=Math.max(restoreTarget,(int)Math.ceil(cells.size()*Math.clamp(fraction,0,1)));}
        public void emergencyRestore(){restoreFraction(1);}
        private Block block(DomainGeometry.Cell c){World w=Bukkit.getWorld(world);if(w==null)throw new IllegalStateException("World domain belum dimuat: "+world);return w.getBlockAt(x+c.x(),y+c.y(),z+c.z());}
        private boolean protectedBlock(Block b){return !complete&&b.getWorld().getUID().equals(world)&&shell.contains(new DomainGeometry.Cell(b.getX()-x,b.getY()-y,b.getZ()-z));}
        private boolean overlaps(org.bukkit.util.BoundingBox box) {
            return DomainGeometry.overlapsShell(shell,box.getMinX()-x,box.getMinY()-y,box.getMinZ()-z,box.getMaxX()-x,box.getMaxY()-y,box.getMaxZ()-z);
        }
        private boolean clearBody(Player player,Location location) {
            var box=player.getBoundingBox().shift(location.getX()-player.getX(),location.getY()-player.getY(),location.getZ()-player.getZ());
            if(overlaps(box))return false;
            World w=location.getWorld();
            for(int bx=(int)Math.floor(box.getMinX());bx<=Math.floor(Math.nextDown(box.getMaxX()));bx++)
                for(int by=(int)Math.floor(box.getMinY());by<=Math.floor(Math.nextDown(box.getMaxY()));by++)
                    for(int bz=(int)Math.floor(box.getMinZ());bz<=Math.floor(Math.nextDown(box.getMaxZ()));bz++)
                        if(by<w.getMinHeight() || by>=w.getMaxHeight() || !w.getBlockAt(bx,by,bz).isPassable())return false;
            return true;
        }
        private void validate(DomainGeometry.Cell cell) {
            Block block=block(cell);
            if(!Bukkit.getWorld(world).getWorldBorder().isInside(block.getLocation()))throw new IllegalArgumentException("Bola domain melewati world border.");
            String unsupported=null;
            var data=block.getBlockData();
            if(data instanceof org.bukkit.block.data.Waterlogged waterlogged && waterlogged.isWaterlogged())
                unsupported="waterlogged dengan penjadwalan fluida belum didukung";
            else if(data instanceof Bed || data instanceof org.bukkit.block.data.type.Piston)
                unsupported="struktur multiblok/mekanik bergerak";
            else if(data instanceof org.bukkit.block.data.type.Chest chest && chest.getType()!=org.bukkit.block.data.type.Chest.Type.SINGLE)
                unsupported="double chest terhubung";
            else if(data instanceof Bisected half && !(data instanceof org.bukkit.block.data.type.Stairs) && !(data instanceof org.bukkit.block.data.type.TrapDoor)) {
                if(!DomainTerrainPolicy.naturalDoublePlant(block.getType()))unsupported="struktur dua blok";
                else {
                    int dy=half.getHalf()==Bisected.Half.BOTTOM ? 1 : -1;
                    var pair=new DomainGeometry.Cell(cell.x(),cell.y()+dy,cell.z());
                    if(!shell.contains(pair)||block(pair).getType()!=block.getType())unsupported="tanaman dua blok memotong batas lapisan";
                }
            }
            BlockState state=block.getState();
            if(unsupported==null && state instanceof TileState && !DomainTerrainPolicy.container(block.getType()))
                unsupported="block entity aktif/khusus belum didukung";
            if(unsupported==null && !DomainTerrainPolicy.supported(block.getType()))unsupported="physics blok belum didukung";
            if(unsupported!=null)throw unsupported(block,unsupported);
            for(var face:FACES) {
                Block near=block.getRelative(face);var nearData=near.getBlockData();
                if(DomainTerrainPolicy.unsupportedConnectedNeighbour(near.getType(),near.getType().hasGravity(),nearData instanceof Bed || nearData instanceof org.bukkit.block.data.type.Door))
                    throw unsupported(near,"tetangga dengan struktur terhubung/gravitasi di batas lapisan belum aman");
            }
            if(DomainTerrainPolicy.container(block.getType())) {
                for(var face:FACES) {
                    Block near=block.getRelative(face);
                    if(!DomainTerrainPolicy.stableContainerNeighbour(near.getType()) || near.getBlockData() instanceof Bisected || near.getBlockData() instanceof Bed)
                        throw unsupported(near,"tetangga container memiliki physics/struktur yang belum aman");
                }
            }
        }
        private IllegalArgumentException unsupported(Block block,String reason) {
            return new IllegalArgumentException("Domain ditolak: "+reason+" ("+block.getType()+" di "+block.getX()+","+block.getY()+","+block.getZ()+"). Kemiringan tanah bukan penyebab penolakan.");
        }
        private byte[] snapshot(Block block)throws IOException {
            Structure structure=Bukkit.getStructureManager().createStructure();structure.fill(block.getLocation(),new BlockVector(1,1,1),false);
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();Bukkit.getStructureManager().saveStructure(bytes,structure);return bytes.toByteArray();
        }
        private void checkPlayers(boolean move) {
            World w=Bukkit.getWorld(world);if(w==null)throw new IllegalStateException("World belum dimuat.");
            for(Player p:w.getPlayers())if(overlaps(p.getBoundingBox())) {
                Location safe=null;
                for(int dy:List.of(0,1,-1,2,-2,3,-3,4,-4)) {if(safe!=null)break;for(int dx=-4;dx<=4&&safe==null;dx++)for(int dz=-4;dz<=4;dz++) {
                    Location candidate=p.getLocation().clone().add(dx,dy,dz);
                    if(candidate.distanceSquared(center())>Math.pow(radius-2,2))continue;
                    if(clearBody(p,candidate)&&candidate.clone().subtract(0,.1,0).getBlock().getType().isSolid()){safe=candidate;break;}
                }}
                if(safe==null)throw new IllegalStateException("Tidak ada ruang aman di dalam domain untuk pemain pada lapisan.");
                if(move&&!evacuationTeleport.test(p,safe))throw new IllegalStateException("Pemindahan pemain dari lapisan ditolak; domain dibatalkan.");
            }
        }
        private void tick(int budget) {
            if(complete)return;
            try {
                if(Bukkit.getWorld(world)==null)return;
                if(!ready) {
                    if(restoring){complete=true;return;} // No snapshot was committed, hence no mutation was possible.
                    for(int n=0;n<budget&&capture<cells.size();n++,capture++){validate(cells.get(capture));snapshots.add(snapshot(block(cells.get(capture))));}
                    if(capture==cells.size()) {
                        DomainJournal.save(marker(id,"snapshot"),new DomainJournal.Image(id,owner,world,x,y,z,radius,List.copyOf(snapshots)));ready=true;
                    }
                    return;
                }
                if(restoring) {
                    for(int n=0;n<budget&&restore<restoreTarget;n++,restore++) {
                        Block b=block(cells.get(restore));
                        Structure s=Bukkit.getStructureManager().loadStructure(new ByteArrayInputStream(snapshots.get(restore)));
                        DomainBlockRestorer.restore(b,s);
                        if(!StructureNbt.equivalent(snapshot(b),snapshots.get(restore)))throw new IOException("Verifikasi NBT pemulihan gagal pada "+b.getLocation());
                    }
                    DomainJournal.atomic(marker(id,"progress"),Integer.toString(restore).getBytes(StandardCharsets.US_ASCII));
                    if(restore==cells.size()) {
                        // Do not release protection until the world has been saved and completion committed.
                        Bukkit.getWorld(world).save(true); // Paper 1.21.4 waits for the chunk writer to finish.
                        DomainJournal.atomic(marker(id,"complete"),"world-save-flushed; snapshots retained".getBytes(StandardCharsets.UTF_8));complete=true;failure=null;
                    }
                } else if(failure==null&&build<buildTarget) {
                    checkPlayers(true);moved=true;
                    for(int n=0;n<budget&&build<buildTarget;n++,build++)block(cells.get(build)).setType(Material.OBSIDIAN,false);
                }
            } catch(Exception ex) {
                if(failure==null)plugin.getLogger().severe("Domain "+id+": "+ex.getMessage()+"; journal tetap disimpan dan pemulihan dicoba ulang.");
                failure=ex.getMessage();restoring=true;restoreTarget=cells.size();
            }
        }
    }
    private boolean protects(Block b){return handles.stream().anyMatch(h->h.protectedBlock(b));}
    private boolean nearProtectedShell(Block b,int distance) {
        for(Handle h:handles)if(!h.complete&&h.world.equals(b.getWorld().getUID())) {
            int bx=b.getX()-h.x,by=b.getY()-h.y,bz=b.getZ()-h.z;
            for(int dx=-distance;dx<=distance;dx++)for(int dy=-(distance-Math.abs(dx));dy<=distance-Math.abs(dx);dy++) {
                int remaining=distance-Math.abs(dx)-Math.abs(dy);
                for(int dz=-remaining;dz<=remaining;dz++)if(h.shell.contains(new DomainGeometry.Cell(bx+dx,by+dy,bz+dz)))return true;
            }
        }
        return false;
    }
    private boolean inventoryProtected(Inventory inventory){
        if (inventory == null || handles.isEmpty()) return false;
        Set<Inventory> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<Inventory> pending = new ArrayDeque<>();
        pending.add(inventory);
        while (!pending.isEmpty()) {
            Inventory current = pending.removeFirst();
            if (!visited.add(current)) continue;
            if (current instanceof DoubleChestInventory chest) {
                // Inspect the physical inventories directly. A side's Chest.getInventory()
                // rejoins the combined chest; its holder is not a safe traversal edge.
                Inventory left = chest.getLeftSide(), right = chest.getRightSide();
                if (left != null) pending.addLast(left);
                if (right != null) pending.addLast(right);
            } else {
                InventoryHolder holder = current.getHolder();
                if (holder instanceof BlockState state && protects(state.getBlock())) return true;
                // Wrappers can expose the same combined inventory through fresh DoubleChest holders.
                // Visit inventory identities, not holder identities, so back-edges terminate.
                if (holder instanceof DoubleChest chest) pending.addLast(chest.getInventory());
            }
        }
        return false;
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void inventory(InventoryClickEvent e){if(inventoryProtected(e.getView().getTopInventory()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void inventory(InventoryDragEvent e){if(inventoryProtected(e.getView().getTopInventory()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void inventory(InventoryMoveItemEvent e){if(inventoryProtected(e.getSource())||inventoryProtected(e.getDestination()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void inventory(InventoryPickupItemEvent e){if(inventoryProtected(e.getInventory()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void breakBlock(BlockBreakEvent e){if(protects(e.getBlock()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void place(BlockPlaceEvent e){if(protects(e.getBlock())||List.of(org.bukkit.block.BlockFace.NORTH,org.bukkit.block.BlockFace.SOUTH,org.bukkit.block.BlockFace.EAST,org.bukkit.block.BlockFace.WEST).stream().anyMatch(f->protects(e.getBlock().getRelative(f))))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void interact(PlayerInteractEvent e){if(e.getClickedBlock()!=null&&protects(e.getClickedBlock()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void bucket(PlayerBucketEmptyEvent e){if(protects(e.getBlock())||protects(e.getBlockClicked().getRelative(e.getBlockFace())))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void bucket(PlayerBucketFillEvent e){if(protects(e.getBlock()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void physics(BlockPhysicsEvent e){if(nearProtectedShell(e.getBlock(),1)||protects(e.getSourceBlock()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void flow(BlockFromToEvent e){if(protects(e.getBlock())||protects(e.getToBlock()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void fade(BlockFadeEvent e){if(protects(e.getBlock()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void form(BlockFormEvent e){if(protects(e.getBlock()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void spread(BlockSpreadEvent e){if(protects(e.getBlock())||protects(e.getSource()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void burn(BlockBurnEvent e){if(protects(e.getBlock()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void decay(LeavesDecayEvent e){if(nearProtectedShell(e.getBlock(),6))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void grow(BlockGrowEvent e){if(nearProtectedShell(e.getBlock(),1))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void entity(EntityChangeBlockEvent e){if(protects(e.getBlock()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void explosion(EntityExplodeEvent e){e.blockList().removeIf(this::protects);}
    @EventHandler(priority=EventPriority.HIGHEST) public void explosion(BlockExplodeEvent e){e.blockList().removeIf(this::protects);}
    @EventHandler(priority=EventPriority.HIGHEST) public void extend(BlockPistonExtendEvent e){if(protects(e.getBlock())||e.getBlocks().stream().anyMatch(b->protects(b)||protects(b.getRelative(e.getDirection()))))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void retract(BlockPistonRetractEvent e){if(protects(e.getBlock())||e.getBlocks().stream().anyMatch(b->protects(b)||protects(b.getRelative(e.getDirection()))))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void grow(StructureGrowEvent e){if(e.getBlocks().stream().anyMatch(b->protects(b.getBlock())))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void portal(PortalCreateEvent e){if(e.getBlocks().stream().anyMatch(b->protects(b.getBlock())))e.setCancelled(true);}
    @EventHandler public void unload(WorldUnloadEvent e){if(handles.stream().anyMatch(h->!h.complete&&h.world.equals(e.getWorld().getUID())))e.setCancelled(true);}
    @Override public void close(){requireThread();if(task!=null)task.cancel();for(Handle h:handles){h.emergencyRestore();int attempts=0;while(!h.complete&&attempts++<((h.cells.size()/128)+3)){h.tick(128);if(h.failure!=null)break;}}HandlerList.unregisterAll(this);}
}
