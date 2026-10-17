package com.bangzachery.vitae.vitaemanager.story;

import com.bangzachery.vitae.vitaemanager.core.gui.VitaeMenu;
import com.bangzachery.vitae.vitaemanager.npc.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.TileState;
import org.bukkit.boss.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import static com.bangzachery.vitae.vitaemanager.story.DungeonData.*;
import com.bangzachery.vitae.vitaemanager.story.DungeonData.Mob;
import com.bangzachery.vitae.vitaemanager.story.DungeonData.Action;

public final class DungeonService implements Listener,AutoCloseable {
    private final JavaPlugin plugin;
    private final StoryStore<State> store;
    private State state;
    private final FadeService fade;
    private final DungeonMobs mobs;
    private final DungeonCamera camera;
    private final Set<UUID> internal=new HashSet<>(),hardDeaths=new HashSet<>();
    private final Map<UUID,Long> portals=new HashMap<>(),warns=new HashMap<>(),actionClicks=new HashMap<>();
    private final Map<String,BossBar> bars=new HashMap<>();
    private final Map<String,Set<UUID>> finishes=new HashMap<>();
    private final Map<String,Long> decideAt=new HashMap<>();
    private final Map<String,ArrayDeque<Mob>> spawnQueues=new HashMap<>();
    private record WorldJob(String dungeon,Runnable work){}
    private final ArrayDeque<WorldJob> worldWork=new ArrayDeque<>();
    private final Map<String,Set<Chunk>> tickets=new HashMap<>();
    private final Map<String,Integer> outstanding=new HashMap<>();
    private final NamespacedKey receiptKey;
    private final Map<String,com.bangzachery.vitae.vitaemanager.selection.AreaScanner.Handle> scans=new HashMap<>();
    private BukkitTask task;
    private long tick;
    private boolean closed;
    public DungeonService(JavaPlugin plugin,FadeService fade)throws IOException{
        this.plugin=plugin;this.fade=fade;mobs=new DungeonMobs(plugin);receiptKey=new NamespacedKey(plugin,"dungeon_rewards");
        store=new StoryStore<>(plugin.getDataFolder().toPath().resolve("dungeons.json"),State.class);state=store.load(new State());state.validate();
        for(Definition d:state.definitions.values())for(List<String> items:d.rewards.values())for(String item:items)NpcService.decode(item);
        camera=new DungeonCamera(plugin,this::teleport);
    }
    public State current(){return state;}
    public Definition get(String id){Definition d=state.definitions.get(id);if(d==null)throw new IllegalArgumentException("Dungeon tidak ditemukan. /vdungeon list");return d;}
    public void start()throws IOException{
        Bukkit.getPluginManager().registerEvents(this,plugin);
        mobs.guardNaturalSpawns(w->definition(w)!=null);
        for(Definition d:state.definitions.values())if(d.enabled){World w=Bukkit.getWorld(d.world);if(w!=null)w.setGameRule(GameRule.DO_MOB_SPAWNING,false);}
        // A restart stops the previous scene instead of resuming a partly executed spawn/camera.
        for(Run r:state.runs.values())if(r.active()||r.restoring){r.phase=Phase.ABORTED;r.restoring=true;
            for(var e:r.participants.entrySet())if(e.getValue().status!=Status.DEAD&&e.getValue().status!=Status.EXITED){e.getValue().status=Status.EXITED;state.returns.put(e.getKey(),get(r.dungeon).home);}
            queueRestore(r);}
        save();task=Bukkit.getScheduler().runTaskTimer(plugin,this::tick,1,1);
        for(Player p:Bukkit.getOnlinePlayers())recover(p);
    }
    private void save()throws IOException{store.save(state);}
    private State draft(){State next=new State();next.version=state.version;
        state.definitions.forEach((id,d)->next.definitions.put(id,StoryStore.JSON.fromJson(StoryStore.JSON.toJson(d),Definition.class)));
        next.runs=new LinkedHashMap<>(state.runs);next.actions=new LinkedHashMap<>(state.actions);next.returns=new LinkedHashMap<>(state.returns);return next;}
    private void persist(){try{save();}catch(IOException e){throw new IllegalStateException("State dungeon belum tersimpan. Permainan dihentikan agar progres tidak rusak.",e);}}
    public void create(String id,World world)throws IOException{
        if(state.definitions.containsKey(id))throw new IllegalArgumentException("ID sudah digunakan.");
        State next=draft();next.definitions.put(id,new Definition(id,world.getUID()));store.save(next);state=next;
    }
    public void assertEditable(String id){
        if(closed)throw new IllegalStateException("Modul dungeon sudah dimuat ulang. Ambil alat seleksi baru.");
        get(id);Run r=state.runs.get(id);
        if(scans.containsKey(id)||(r!=null&&(r.active()||r.restoring||outstanding.containsKey(id))))throw new IllegalStateException("Reset/tunggu sesi atau pemindaian selesai sebelum mengubah setup.");
    }
    public void edit(String id,Consumer<Definition> change)throws IOException{
        assertEditable(id);
        Run r=state.runs.get(id);if(r!=null && (r.active()||r.restoring||outstanding.containsKey(id)))throw new IllegalStateException("Selesaikan/reset sesi sebelum mengubah setup.");
        State next=draft();Definition d=next.definitions.get(id);if(d==null)throw new IllegalArgumentException("ID tidak ditemukan.");change.accept(d);d.enabled=false;d.validate();store.save(next);state=next;
    }
    public void enable(String id,boolean enabled)throws IOException{
        if(!enabled){Run r=state.runs.get(id);if(r!=null&&r.active())stop(id,"Dungeon dinonaktifkan.");}
        State next=draft();Definition d=next.definitions.get(id);if(d==null)throw new IllegalArgumentException("ID tidak ditemukan.");d.enabled=enabled;
        if(enabled){check(d);for(Definition other:next.definitions.values())if(other!=d&&other.enabled&&(d.world.equals(other.world)||d.entrance.overlaps(other.entrance)))throw new IllegalArgumentException("Satu sesi dungeon per world; portal masuk tidak boleh bertumpuk.");}
        store.save(next);state=next;if(enabled)Bukkit.getWorld(d.world).setGameRule(GameRule.DO_MOB_SPAWNING,false);
    }
    public void check(Definition d){
        boolean before=d.enabled;d.enabled=true;try{d.validate();}finally{d.enabled=before;}
        World world=Bukkit.getWorld(d.world);if(world==null)throw new IllegalArgumentException("World dungeon belum dimuat.");
        for(NpcData.Point p:List.of(d.entry,d.home,d.start,d.next))safe(p);
        for(String npc:List.of(d.glassNpc,d.minionNpc,d.bossNpc)){
            var n=npc().get(npc);if(!n.enabled())throw new IllegalArgumentException("NPC belum ON: "+npc);
            if(!d.world.equals(n.location().world()))throw new IllegalArgumentException("NPC harus di world dungeon: "+npc);
            if(!n.gift().empty()||n.quest().kind()!=NpcData.QuestKind.NONE)throw new IllegalArgumentException("NPC dungeon harus tanpa gift/quest lama. Hadiah diatur dengan /vdungeon <id> reward <tahap>.");
            safe(n.stand());
        }
        for(var pair:List.of(Map.entry(d.glassNpc,d.glassPage),Map.entry(d.minionNpc,d.minionPage),Map.entry(d.bossNpc,d.bossPage)))if(!pair.getValue().isEmpty()&&!npc().get(pair.getKey()).nodes().containsKey(pair.getValue()))throw new IllegalArgumentException("Halaman selamat tidak ada: "+pair.getValue());
        // Block validation is performed by prepare() with a per-tick budget.
        for(Mob m:d.minions.values())mobs.validate(m);mobs.validate(d.boss);
        for(List<String> items:d.rewards.values())for(String item:items)NpcService.decode(item);
    }
    private void safe(NpcData.Point point){Location l=location(point);if(l.getY()<l.getWorld().getMinHeight()||l.getY()+1>=l.getWorld().getMaxHeight()||!l.getBlock().isPassable()||!l.clone().add(0,1,0).getBlock().isPassable())throw new IllegalArgumentException("Lokasi teleport terhalang: "+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ());}
    private NpcService npc(){NpcService service=NpcModule.active(plugin);if(service==null)throw new IllegalStateException("Modul NPC belum aktif. /vnpc status");return service;}
    private record ScanPart(Box box,String kind){}
    private static com.bangzachery.vitae.vitaemanager.whisper.WhisperArea area(Box b){return com.bangzachery.vitae.vitaemanager.whisper.WhisperArea.between(b.world(),b.x1(),b.y1(),b.z1(),b.x2(),b.y2(),b.z2());}
    public void prepare(CommandSender sender,String id,String operation){
        Definition d=get(id);check(d);
        Run previous=state.runs.get(id);if(previous!=null&&(previous.active()||previous.restoring))throw new IllegalStateException("Reset sesi sebelum memeriksa/membuka setup kembali.");
        if(scans.containsKey(id))throw new IllegalStateException("Pemindaian masih berjalan. Tunggu pesan selesai atau /vitae seleksi cancel.");
        if(operation.equals("open")&&!d.enabled)throw new IllegalStateException("Dungeon belum ON.");
        var todo=new ArrayDeque<ScanPart>();for(Box box:d.bad.values())todo.add(new ScanPart(box,"kaca"));
        todo.add(new ScanPart(d.minionGate,"gerbang minion"));todo.add(new ScanPart(d.bossGate,"gerbang bos"));todo.add(new ScanPart(d.exit,"portal keluar"));
        tell(sender,"Memeriksa blok area bertahap. Tunggu pesan selesai sebelum command berikutnya.");
        scanNext(sender,d,operation,todo,new LinkedHashMap<>());
    }
    private void scanNext(CommandSender sender,Definition d,String operation,ArrayDeque<ScanPart> todo,Map<String,Block> snapshot){
        if(todo.isEmpty()){
            scans.remove(d.id);
            try{check(d);if(operation.equals("on")){enable(d.id,true);tell(sender,"Dungeon ON. Berikutnya /vdungeon "+d.id+" open.");}
                else if(operation.equals("open")){Run next=new Run(d.id);next.blocks.addAll(snapshot.values());State copy=draft();copy.runs.put(d.id,next);store.save(copy);state=copy;tell(sender,"Sesi dibuka. Peserta masuk melalui portal.");}
                else tell(sender,"Setup valid. Berikutnya /vdungeon "+d.id+" on lalu open.");
            }catch(Exception error){tell(sender,"Pemeriksaan gagal: "+error.getMessage());}return;
        }
        ScanPart part=todo.removeFirst();
        try{
            var handle=com.bangzachery.vitae.vitaemanager.selection.SelectionService.require().scanner().scan(area(part.box),
                ()->!closed&&state.definitions.get(d.id)==d&&(!(sender instanceof Player p)||p.isOnline()&&p.hasPermission("vitae.admin.dungeon")),
                (read,total)->{if(sender instanceof Player p)p.sendActionBar(Component.text("Memeriksa "+part.kind+": "+read+"/"+total,NamedTextColor.AQUA));},
                block->{Material type=block.getType();if(block.getState() instanceof TileState)throw new IllegalArgumentException(part.kind+" memuat blok penyimpan data.");
                    if(part.kind.equals("kaca")&&!type.name().endsWith("GLASS"))throw new IllegalArgumentException("Petak salah harus berupa blok kaca penuh.");
                    if(part.kind.equals("portal keluar")&&!type.isAir())throw new IllegalArgumentException("Bagian dalam portal keluar harus kosong/udara.");
                    if(part.kind.startsWith("gerbang")&&Set.of(Material.BEDROCK,Material.BARRIER,Material.END_PORTAL_FRAME,Material.REINFORCED_DEEPSLATE).contains(type))throw new IllegalArgumentException("Gerbang memuat blok khusus.");
                    if(operation.equals("open")){Block cell=new Block(block.getWorld().getUID(),block.getX(),block.getY(),block.getZ(),block.getBlockData().getAsString());snapshot.put(cell.key(),cell);}},
                ()->scanNext(sender,d,operation,todo,snapshot),
                reason->{scans.remove(d.id);if(sender instanceof Player p)p.sendActionBar(Component.empty());tell(sender,reason);});
            if(sender instanceof Player p)handle.owned(p.getUniqueId());scans.put(d.id,handle);
        }catch(RuntimeException e){scans.remove(d.id);tell(sender,e.getMessage());}
    }
    private Run run(Definition d){Run r=state.runs.get(d.id);if(r==null||!r.active())throw new IllegalStateException("Sesi belum dibuka. /vdungeon "+d.id+" open");return r;}
    public void join(Player p,String id){
        Definition d=get(id);if(!d.enabled)throw new IllegalStateException("Dungeon belum aktif.");Run r=run(d);
        if(!r.active()||r.phase!=Phase.WAITING)throw new IllegalStateException("Permainan sudah dimulai/selesai. Tunggu admin membuka sesi baru.");
        if(p.isDead()||p.getGameMode()==GameMode.SPECTATOR)throw new IllegalStateException("Pemain belum bisa masuk.");Participant before=r.participants.get(p.getUniqueId());if(before!=null&&before.status==Status.DEAD)throw new IllegalStateException("Kamu telah gagal dan tidak bisa masuk lagi pada sesi ini.");
        r.participants.putIfAbsent(p.getUniqueId(),new Participant());persist();Player player=p;
        fade.teleport(player,location(d.entry),d.fadeEntry,ok->{if(!ok)failed(player,"Teleport masuk ditolak.",false);else tell(player,"Terdaftar sebagai peserta dungeon. Tunggu permainan kaca dimulai.");});
    }
    public boolean carryBlocked(Player p){return definition(p.getWorld())!=null&&!bypass(p);}
    private Definition definition(World world){return state.definitions.values().stream().filter(d->d.enabled&&d.world.equals(world.getUID())).findFirst().orElse(null);}
    private boolean bypass(Player p){return p.hasPermission("vitae.dungeon.bypass");}
    private Participant participant(Run r,Player p){Participant v=r.participants.get(p.getUniqueId());if(v==null||v.status==Status.DEAD||v.status==Status.EXITED)throw new IllegalStateException("Kamu bukan peserta aktif sesi ini.");return v;}
    private boolean teleport(Player p,Location at){internal.add(p.getUniqueId());try{p.setFallDistance(0);p.setVelocity(new Vector());return p.teleport(at,PlayerTeleportEvent.TeleportCause.PLUGIN);}finally{internal.remove(p.getUniqueId());}}
    private void glass(Player actor,Definition d){
        Run r=run(d);if(r.phase!=Phase.WAITING)throw new IllegalStateException("Permainan kaca sudah dimulai.");
        for(Player p:Bukkit.getWorld(d.world).getPlayers())if(!p.isDead()&&p.getGameMode()!=GameMode.SPECTATOR&&!bypass(p))r.participants.putIfAbsent(p.getUniqueId(),new Participant());
        participant(r,actor);check(d);r.phase=Phase.CAMERA;
        for(var e:r.participants.entrySet()){Player p=Bukkit.getPlayer(e.getKey());Participant part=e.getValue();if(p==null||p.isDead()||part.status!=Status.ACTIVE){part.status=Status.DEAD;continue;}
            part.cameraDone=false;part.falling=false;part.glassFallY=null;part.glassReturnsUsed=0;part.recovery=d.start;part.mode=p.getGameMode().name();part.flight=p.getAllowFlight();part.flying=p.isFlying();}
        persist();
        for(var e:r.participants.entrySet())if(e.getValue().status==Status.ACTIVE){Player p=Bukkit.getPlayer(e.getKey());npc().cancelStoryDialog(p);
            if(p.getVehicle()!=null)p.leaveVehicle();p.eject();if(!teleport(p,location(d.start)))throw new IllegalStateException("Teleport start ditolak untuk "+p.getName());
            camera.start(p,location(d.camera),e.getValue(),d.cameraTicks,()->{e.getValue().cameraDone=true;title(p,"Pilihlah pijakan yang benar","Satu langkah salah, kamu jatuh.");});}
        persist();broadcast(r,"Pengenalan arena dimulai. Timer berjalan setelah kamera selesai. Jatah kembali ke start: "+d.retryLimit()+" kali per pemain.");
    }
    private void accept(Player actor,Definition d,boolean boss){
        Run r=run(d);Participant p=participant(r,actor);if(p.status!=Status.PASSED)throw new IllegalStateException("Selesaikan permainan kaca terlebih dahulu.");
        if(r.phase!=(boss?Phase.MINION_DONE:Phase.GLASS_DONE))throw new IllegalStateException(boss?"Tahap kaca dan minion belum selesai.":"Permainan kaca belum selesai.");
        if(outstanding.containsKey(d.id))throw new IllegalStateException("Gerbang masih dipulihkan.");
        r.phase=boss?Phase.BOSS_READY:Phase.MINION_READY;persist();
        breakArea(r,boss?d.bossGate:d.minionGate);broadcast(r,boss?"Gerbang bos terbuka. Bersiaplah!":"Gerbang minion terbuka. Lewati gerbang untuk memulai pertarungan.");
        if(!boss)r.awards.put("minion-request",request(actor,d));persist();
    }
    private Award request(Player p,Definition d){Award a=new Award();a.player=p.getUniqueId();a.stage="minion-request";a.npc=d.minionNpc;return a;}
    private void spawn(Run r,Definition d,boolean boss){
        if(outstanding.containsKey(d.id))return;r.phase=boss?Phase.BOSS:Phase.MINION;r.killed=0;r.total=boss?1:d.minions.values().stream().mapToInt(Mob::count).sum();persist();
        ArrayDeque<Mob> queue=new ArrayDeque<>();for(Mob m:boss?List.of(d.boss):d.minions.values())for(int i=0;i<m.count();i++)queue.add(m);
        spawnQueues.put(d.id,queue);broadcast(r,boss?"Bos dungeon telah muncul!":"Musnahkan seluruh minion: 0/"+r.total);
    }
    private void combatComplete(Run r,Definition d){
        boolean boss=r.phase==Phase.BOSS;r.phase=boss?Phase.REWARD:Phase.MINION_DONE;persist();removeBar(d.id);
        if(boss){for(Player p:players(r)){npc().cancelStoryDialog(p);if(!teleport(p,location(npc().get(d.bossNpc).stand())))throw new IllegalStateException("Teleport kembali NPC ditolak.");}}
        UUID prefer=boss?null:Optional.ofNullable(r.awards.remove("minion-request")).map(a->a.player).orElse(null);
        choose(r,d,boss?"boss":"minion",prefer);broadcast(r,boss?"Bos dikalahkan! Tunggu hadiah diberikan, lalu segera bersiap keluar.":"Semua minion telah dimusnahkan.");
    }
    private void choose(Run r,Definition d,String stage,UUID prefer){
        List<UUID> candidates=new ArrayList<>();for(var e:r.participants.entrySet()){Player p=Bukkit.getPlayer(e.getKey());if(p!=null&&!p.isDead()&&e.getValue().status==Status.PASSED)candidates.add(e.getKey());}
        Set<UUID> previous=new HashSet<>();for(Award a:r.awards.values())if(a.state==ReceiptState.CLAIMED)previous.add(a.player);
        var unused=candidates.stream().filter(id->!previous.contains(id)).toList();if(!unused.isEmpty())candidates=new ArrayList<>(unused);
        if(candidates.isEmpty()){tellAdmins("Tidak ada peserta online untuk hadiah "+stage+". Hadiah akan dipilih saat peserta kembali.");return;}
        UUID winner=prefer!=null&&candidates.contains(prefer)?prefer:candidates.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(candidates.size()));
        Award a=new Award();a.player=winner;a.stage=stage;a.npc=switch(stage){case "glass"->d.glassNpc;case "minion"->d.minionNpc;default->d.bossNpc;};
        a.page=switch(stage){case "glass"->d.glassPage;case "minion"->d.minionPage;default->d.bossPage;};a.items=new ArrayList<>(d.rewards.get(stage));a.chosenAt=System.currentTimeMillis();r.awards.put(stage,a);persist();
        showAward(r,d,a);
    }
    private void showAward(Run r,Definition d,Award a){
        Player p=Bukkit.getPlayer(a.player);if(p==null||p.isDead())return;NpcService service=npc();service.cancelStoryDialog(p);
        if(!teleport(p,location(service.get(a.npc).stand())))throw new IllegalStateException("Posisi hadiah NPC terhalang/teleport ditolak.");
        service.storyReward(p,a.npc,a.page);tell(p,"Kamu terpilih menerima hadiah "+a.stage+". Pilih Ambil hadiah pada dialog.");
    }
    private void claim(Player p,Definition d,String npc){
        Run r=run(d);participant(r,p);Award a=r.awards.values().stream().filter(v->v.npc.equals(npc)&&v.player.equals(p.getUniqueId())&&v.state!=ReceiptState.CLAIMED&&!v.stage.endsWith("request")).findFirst().orElseThrow(()->new IllegalStateException("Tidak ada hadiah yang dipilih untukmu."));
        if(a.state==ReceiptState.DELIVERING){if(hasReceipt(p,a.receipt)){a.state=ReceiptState.CLAIMED;persist();}else throw new IllegalStateException("Pengiriman sebelumnya belum terkonfirmasi; admin perlu /vdungeon <id> receipt.");}
        else {
            ItemStack[] items=a.items.stream().map(NpcService::decode).toArray(ItemStack[]::new);a.state=ReceiptState.DELIVERING;persist();
            List<Item> dropped=new ArrayList<>();ItemStack[] before=Arrays.stream(p.getInventory().getStorageContents()).map(i->i==null?null:i.clone()).toArray(ItemStack[]::new);
            String marker=p.getPersistentDataContainer().get(receiptKey,PersistentDataType.STRING);
            try {
                if(!hasReceipt(p,a.receipt)){for(ItemStack item:p.getInventory().addItem(items).values()){Item drop=p.getWorld().dropItem(p.getLocation(),item);drop.setOwner(p.getUniqueId());drop.setPickupDelay(0);dropped.add(drop);}markReceipt(p,a.receipt);p.saveData();}
                a.state=ReceiptState.CLAIMED;persist();
            }catch(RuntimeException error){p.getInventory().setStorageContents(before);for(Item drop:dropped)drop.remove();if(marker==null)p.getPersistentDataContainer().remove(receiptKey);else p.getPersistentDataContainer().set(receiptKey,PersistentDataType.STRING,marker);a.state=ReceiptState.RESERVED;try{save();}catch(IOException e){error.addSuppressed(e);}throw error;}
        }
        tell(p,"Hadiah diberikan. Item yang tidak muat jatuh di dekatmu.");
        if(a.stage.equals("boss")){r.phase=Phase.ESCAPE;r.deadline=System.currentTimeMillis()+d.escapeSeconds*1000L;persist();for(Player player:players(r)){npc().cancelStoryDialog(player);title(player,"DUNGEON MULAI RUNTUH!","Segera keluar sebelum waktu habis!");player.playSound(player.getLocation(),"minecraft:entity.wither.spawn",.6f,.8f);}broadcast(r,"Portal keluar akan terbuka setelah "+d.portalAfter+" detik.");}
        else if(a.stage.equals("glass"))teleport(p,location(d.next));
    }
    private boolean hasReceipt(Player p,UUID id){String text=p.getPersistentDataContainer().get(receiptKey,PersistentDataType.STRING);return text!=null&&Arrays.asList(text.split(",")).contains(id.toString());}
    private void markReceipt(Player p,UUID id){String text=p.getPersistentDataContainer().get(receiptKey,PersistentDataType.STRING);ArrayDeque<String> ids=new ArrayDeque<>(text==null?List.of():Arrays.asList(text.split(",")));ids.add(id.toString());while(ids.size()>64)ids.removeFirst();p.getPersistentDataContainer().set(receiptKey,PersistentDataType.STRING,String.join(",",ids));}
    public void receipt(CommandSender sender,String id,String stage,String outcome){
        Run r=state.runs.get(id);if(r==null)throw new IllegalArgumentException("Sesi tidak ditemukan.");Award a=r.awards.get(stage);if(a==null)throw new IllegalArgumentException("Hadiah tidak ditemukan.");
        if(outcome.equals("retry")){if(a.state!=ReceiptState.DELIVERING)throw new IllegalArgumentException("Hanya receipt DELIVERING yang bisa dikoreksi.");Player p=Bukkit.getPlayer(a.player);if(p!=null&&hasReceipt(p,a.receipt))throw new IllegalArgumentException("Receipt sudah ada di playerdata; gunakan delivered.");a.state=ReceiptState.RESERVED;persist();}
        else if(outcome.equals("delivered")){a.state=ReceiptState.CLAIMED;persist();if(stage.equals("boss")&&r.phase==Phase.REWARD){r.phase=Phase.ESCAPE;r.deadline=System.currentTimeMillis()+get(id).escapeSeconds*1000L;persist();}}
        else tell(sender,"Receipt "+stage+": "+a.state+", pemain "+a.player+". Koreksi retry hanya jika dipastikan item belum diterima; delivered jika sudah diterima.");
    }
    public void stop(String id,String reason){
        var scan=scans.remove(id);if(scan!=null)scan.cancel();
        Run r=state.runs.get(id);if(r==null||r.restoring)return;r.phase=Phase.ABORTED;r.restoring=true;
        spawnQueues.remove(id);finishes.remove(id);decideAt.remove(id);removeBar(id);
        worldWork.removeIf(job->job.dungeon.equals(id));outstanding.remove(id);
        for(var e:r.participants.entrySet()){Player p=Bukkit.getPlayer(e.getKey());if(p!=null)try{camera.stop(e.getKey(),false);camera.restore(p,e.getValue());npc().cancelStoryDialog(p);}catch(RuntimeException error){fault(error);}
            if(e.getValue().status!=Status.DEAD&&e.getValue().status!=Status.EXITED){e.getValue().status=Status.EXITED;state.returns.put(e.getKey(),get(id).home);}}
        try{save();}catch(IOException error){fault(error);}for(Player p:playersAll(r))recover(p);queueRestore(r);broadcast(r,reason);
    }
    private void queueRestore(Run r){
        releaseTickets(r.dungeon);
        for(UUID id:List.copyOf(r.mobs.keySet()))mobs.remove(id);r.mobs.clear();Definition d=get(r.dungeon);
        World w=Bukkit.getWorld(d.world);if(w!=null)for(Entity entity:w.getEntities())if(r.token.toString().equals(entity.getPersistentDataContainer().get(mobs.sessionKey,PersistentDataType.STRING)))mobs.remove(entity.getUniqueId());
        outstanding.put(d.id,r.blocks.size());
        for(Block block:r.blocks)worldWork.add(new WorldJob(r.dungeon,()->restoreBlock(r,block)));
        if(r.blocks.isEmpty()){r.restoring=false;outstanding.remove(d.id);}
    }
    private void restoreBlock(Run r,Block block){
        if(Bukkit.getWorld(block.world())==null){worldWork.add(new WorldJob(r.dungeon,()->restoreBlock(r,block)));return;}
        Location at=block.location();var bounds=new org.bukkit.util.BoundingBox(block.x(),block.y(),block.z(),block.x()+1,block.y()+1,block.z()+1);
        if(!Bukkit.createBlockData(block.data()).getMaterial().isAir()&&at.getWorld().getPlayers().stream().anyMatch(p->!p.isDead()&&p.getGameMode()!=GameMode.SPECTATOR&&p.getBoundingBox().overlaps(bounds))){worldWork.add(new WorldJob(r.dungeon,()->restoreBlock(r,block)));return;}
        at.getBlock().setBlockData(Bukkit.createBlockData(block.data()),false);int left=outstanding.compute(r.dungeon,(k,v)->v==null?0:v-1);if(left<=0){outstanding.remove(r.dungeon);r.restoring=false;persist();}
    }
    private void breakArea(Run r,Box area){
        List<Block> selected=r.blocks.stream().filter(b->area.contains(b.location())).toList();outstanding.merge(r.dungeon,selected.size(),Integer::sum);
        for(Block b:selected)worldWork.add(new WorldJob(r.dungeon,()->{var block=b.location().getBlock();block.getWorld().spawnParticle(Particle.BLOCK,block.getLocation().add(.5,.5,.5),8,.4,.4,.4,.1,block.getBlockData());block.setType(Material.AIR,false);
            int n=outstanding.compute(r.dungeon,(k,v)->v==null?0:v-1);if(n<=0)outstanding.remove(r.dungeon);}));
    }
    private Location breakTile(Player p,Run r,Definition d,String name,Box tile){
        Participant part=participant(r,p);part.falling=true;part.glassFallY=tile.y1()-1.0;r.broken.add(name);persist();
        // The failing player cannot preserve horizontal momentum to escape the hole.
        Location below=p.getLocation().clone();below.setX(Math.max(tile.x1()+.6,Math.min(tile.x2()+.4,below.getX())));below.setZ(Math.max(tile.z1()+.6,Math.min(tile.z2()+.4,below.getZ())));below.setY(tile.y1()-.35);
        if(tile.volume()<=128){for(int x=tile.x1();x<=tile.x2();x++)for(int z=tile.z1();z<=tile.z2();z++)p.getWorld().getBlockAt(x,tile.y1(),z).setType(Material.AIR,false);}
        else{for(int x=Math.max(tile.x1(),p.getLocation().getBlockX()-1);x<=Math.min(tile.x2(),p.getLocation().getBlockX()+1);x++)for(int z=Math.max(tile.z1(),p.getLocation().getBlockZ()-1);z<=Math.min(tile.z2(),p.getLocation().getBlockZ()+1);z++)p.getWorld().getBlockAt(x,tile.y1(),z).setType(Material.AIR,false);breakArea(r,tile);}
        p.setVelocity(new Vector(0,-1.1,0));p.playSound(p.getLocation(),"minecraft:block.glass.break",1,.75f);tell(p,"Pijakan salah! Kamu jatuh.");return below;
    }
    private double glassFloor(Definition d){
        double floor=d.start.y()-2.0;
        for(Box tile:d.bad.values())floor=Math.min(floor,tile.y1()-1.0);
        return floor;
    }
    private void glassFall(Player p,Run r,Definition d,Participant part){
        if(!r.active()||r.phase!=Phase.GLASS||part.status!=Status.ACTIVE||p.isDead())return;
        if(System.currentTimeMillis()>=r.deadline){failed(p,"Waktu kaca habis. Kamu gagal.",true);return;}
        if(part.glassReturnsUsed>=d.retryLimit()){failed(p,"Jatah kembali ke start habis. Kamu gugur dari permainan kaca.",true);return;}
        safe(d.start);
        part.glassReturnsUsed++;part.falling=false;part.glassFallY=null;persist();
        if(!teleport(p,location(d.start)))throw new IllegalStateException("Teleport ulang kaca ditolak untuk "+p.getName()+". Periksa lokasi start.");
        p.setFallDistance(0);p.setVelocity(new Vector());
        int remaining=d.retryLimit()-part.glassReturnsUsed;
        tell(p,"Kembali ke start. Sisa kesempatan ulang: "+remaining+"/"+d.retryLimit()+". Timer bersama tetap berjalan.");
        title(p,"KEMBALI KE START",remaining>0?"Sisa kesempatan ulang: "+remaining:"Kesempatan terakhir. Jatuh lagi berarti gugur.");
    }
    private void failed(Player p,String why,boolean kill){
        Definition d=definition(p.getWorld());if(d==null)return;Run r=state.runs.get(d.id);if(r==null)return;Participant part=r.participants.get(p.getUniqueId());if(part==null||part.status==Status.DEAD||part.status==Status.EXITED)return;
        camera.stop(p.getUniqueId(),false);part.status=Status.DEAD;persist();tell(p,why);if(kill&&!p.isDead()){hardDeaths.add(p.getUniqueId());p.setHealth(0);}
    }
    private void escape(Player p,Definition d,Run r){
        if(closed||r.phase!=Phase.ESCAPE||!r.broken.contains("exit-open"))return;Participant part=r.participants.get(p.getUniqueId());if(part==null||part.status!=Status.PASSED)return;part.status=Status.EXITED;persist();removePlayerBar(p,d.id);portals.put(p.getUniqueId(),tick+40);
        fade.teleport(p,location(d.home),d.fadeExit,ok->{if(ok)tell(p,"Kamu berhasil keluar dari dungeon.");else{part.status=Status.PASSED;persist();tell(p,"Teleport keluar ditolak. Coba portal lagi atau panggil admin.");}});
    }
    private void openExit(Run r,Definition d){
        r.broken.add("exit-open");persist();
        for(Block block:r.blocks)if(d.exit.contains(block.location())&&Bukkit.createBlockData(block.data()).getMaterial().isAir())worldWork.add(new WorldJob(d.id,()->block.location().getBlock().setType(Material.END_GATEWAY,false)));
        for(Player p:players(r)){title(p,"PORTAL KELUAR TERBUKA","Segera tinggalkan tempat ini!");p.playSound(p.getLocation(),"minecraft:block.end_portal.spawn",.75f,1);}
    }
    private void tick(){
        if(closed)return;tick++;
        try{camera.tick();}catch(DungeonCamera.CameraFailure error){fault(error);for(Run r:state.runs.values())if(r.active()&&r.participants.containsKey(error.player))stop(r.dungeon,"Kamera ditolak; sesi dipulihkan.");}
        int jobs=Math.min(128,worldWork.size());for(int i=0;i<jobs&&!worldWork.isEmpty();i++){WorldJob job=worldWork.remove();try{job.work.run();}catch(RuntimeException error){if(tick%100==0)fault(error);Run r=state.runs.get(job.dungeon);if(r!=null&&r.restoring)worldWork.add(job);else stop(job.dungeon,"Pemulihan diperlukan setelah perubahan blok gagal.");}}
        for(Definition d:List.copyOf(state.definitions.values())){
            Run r=state.runs.get(d.id);if(r==null||!r.active())continue;
            try{
                if(r.phase==Phase.CAMERA && r.participants.values().stream().filter(p->p.status==Status.ACTIVE).allMatch(p->p.cameraDone)){
                    r.phase=Phase.GLASS;r.deadline=System.currentTimeMillis()+d.glassSeconds*1000L;persist();broadcast(r,"Permainan kaca dimulai! Waktu "+clock(d.glassSeconds)+".");}
                if(decideAt.containsKey(d.id)&&tick>=decideAt.get(d.id))decideGlass(r,d);
                if(spawnQueues.containsKey(d.id)){ArrayDeque<Mob> queue=spawnQueues.get(d.id);for(int i=0;i<8&&!queue.isEmpty();i++){Mob spec=queue.remove();LivingEntity entity=mobs.spawn(spec,r.token);r.mobs.put(entity.getUniqueId(),spec.type());pin(d.id,entity.getLocation().getChunk());}if(queue.isEmpty()){spawnQueues.remove(d.id);persist();}else if(tick%20==0)persist();}
                if((r.phase==Phase.MINION||r.phase==Phase.BOSS)&&!spawnQueues.containsKey(d.id)&&r.killed>=r.total)combatComplete(r,d);
                if(tick%20==0){
                    if(r.phase==Phase.GLASS){bar(r,d,"Jembatan kaca",d.glassSeconds);if(System.currentTimeMillis()>=r.deadline){for(Player p:players(r))if(r.participants.get(p.getUniqueId()).status==Status.ACTIVE)failed(p,"Waktu kaca habis. Kamu gagal.",true);}
                        if(r.participants.values().stream().noneMatch(p->p.status==Status.ACTIVE)){r.phase=Phase.GLASS_DONE;persist();removeBar(d.id);broadcast(r,"Permainan kaca selesai. Peserta yang lolos dapat melanjutkan.");}}
                    else if(r.phase==Phase.ESCAPE){bar(r,d,"Dungeon runtuh • segera keluar",d.escapeSeconds);long left=r.deadline-System.currentTimeMillis();if(left<=(d.escapeSeconds-d.portalAfter)*1000L&&!r.broken.contains("exit-open"))openExit(r,d);
                        if(left<=0){for(Player p:players(r))if(p.getWorld().getUID().equals(d.world))failed(p,"Waktu keluar habis.",true);r.phase=Phase.COMPLETE;r.restoring=true;persist();removeBar(d.id);queueRestore(r);}}
                    else if(r.phase==Phase.MINION||r.phase==Phase.BOSS)combatBar(r,d);
                    for(Award a:List.copyOf(r.awards.values()))if(a.state==ReceiptState.RESERVED&&!a.stage.endsWith("request")&&(Bukkit.getPlayer(a.player)==null||Optional.ofNullable(r.participants.get(a.player)).map(p->p.status==Status.DEAD||p.status==Status.EXITED).orElse(true))&&System.currentTimeMillis()-a.chosenAt>=30_000){r.awards.remove(a.stage);choose(r,d,a.stage,null);}
                    if(r.phase==Phase.REWARD&&!r.awards.containsKey("boss"))choose(r,d,"boss",null);
                }
                if(tick%40==0&&(r.phase==Phase.MINION||r.phase==Phase.BOSS)&&!spawnQueues.containsKey(d.id))for(UUID id:r.mobs.keySet()){
                    Entity entity=Bukkit.getEntity(id);if(entity==null||(!entity.isValid()&&!entity.isDead()))throw new IllegalStateException("Mob sesi hilang tanpa kematian yang tercatat: "+id+". Sesi dihentikan; reset dan mulai ulang.");else if(!entity.isDead())pin(d.id,entity.getLocation().getChunk());}
                if(r.active()&&r.phase!=Phase.WAITING&&!r.participants.isEmpty()&&r.participants.values().stream().noneMatch(p->p.status==Status.ACTIVE||p.status==Status.PASSED)){
                    if(r.phase==Phase.ESCAPE&&r.participants.values().stream().anyMatch(p->p.status==Status.EXITED)){r.phase=Phase.COMPLETE;r.restoring=true;persist();removeBar(d.id);queueRestore(r);}
                    else stop(d.id,"Semua peserta gagal. Arena dipulihkan untuk sesi berikutnya.");continue;}
                double defaultFallY=r.phase==Phase.GLASS?glassFloor(d):0;
                for(Player p:players(r)){Participant part=r.participants.get(p.getUniqueId());if(!camera.busy(p.getUniqueId())&&!bypass(p)){if(p.isFlying())p.setFlying(false);if(p.isGliding())p.setGliding(false);}
                    if(r.phase==Phase.GLASS&&part.status==Status.ACTIVE&&!camera.busy(p.getUniqueId())&&!bypass(p)){
                        if(part.falling)p.setVelocity(new Vector(0,Math.min(-.5,p.getVelocity().getY()),0));
                        double floor=part.falling&&part.glassFallY!=null?part.glassFallY:defaultFallY;
                        if(p.getY()<floor)glassFall(p,r,d,part);
                    }}
            }catch(RuntimeException error){fault(error);try{stop(d.id,"Dungeon dihentikan karena error. Hubungi admin.");}catch(RuntimeException cleanup){plugin.getLogger().severe(cleanup.toString());}}
        }
    }
    private void pin(String id,Chunk chunk){if(tickets.computeIfAbsent(id,k->new HashSet<>()).add(chunk))chunk.addPluginChunkTicket(plugin);}
    private void releaseTickets(String id){Set<Chunk> set=tickets.remove(id);if(set!=null)for(Chunk chunk:set)chunk.removePluginChunkTicket(plugin);}
    private void decideGlass(Run r,Definition d){
        Set<UUID> set=finishes.remove(d.id);decideAt.remove(d.id);if(set==null||r.awards.containsKey("glass"))return;
        List<UUID> valid=set.stream().filter(id->{Participant p=r.participants.get(id);Player online=Bukkit.getPlayer(id);return p!=null&&p.status==Status.PASSED&&online!=null&&!online.isDead();}).toList();
        if(!valid.isEmpty())choose(r,d,"glass",valid.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(valid.size())));
    }
    private static String clock(long seconds){return seconds>=3600?String.format(Locale.ROOT,"%d:%02d:%02d",seconds/3600,(seconds%3600)/60,seconds%60):String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60);}
    private void bar(Run r,Definition d,String name,int seconds){BossBar b=bars.computeIfAbsent(d.id,id->Bukkit.createBossBar(name,BarColor.RED,BarStyle.SOLID));long remaining=Math.max(0,(r.deadline-System.currentTimeMillis()+999)/1000);b.setTitle(name+" • "+(r.phase==Phase.GLASS?clock(remaining):remaining+" detik"));b.setProgress(Math.max(0,Math.min(1,remaining/(double)seconds)));syncBar(b,r);}
    private void combatBar(Run r,Definition d){BossBar b=bars.computeIfAbsent(d.id,id->Bukkit.createBossBar("Dungeon",BarColor.PURPLE,BarStyle.SOLID));b.setTitle((r.phase==Phase.BOSS?"Bos":"Minion")+" • "+r.killed+"/"+r.total);b.setProgress(r.total==0?0:Math.max(0,Math.min(1,r.killed/(double)r.total)));syncBar(b,r);}
    private void syncBar(BossBar b,Run r){Set<Player> visible=new HashSet<>(players(r));for(Player p:List.copyOf(b.getPlayers()))if(!visible.contains(p))b.removePlayer(p);for(Player p:visible)b.addPlayer(p);}
    private void removeBar(String id){BossBar b=bars.remove(id);if(b!=null)b.removeAll();}
    private void removePlayerBar(Player p,String id){BossBar b=bars.get(id);if(b!=null)b.removePlayer(p);}
    private List<Player> players(Run r){return r.participants.entrySet().stream().filter(e->e.getValue().status!=Status.DEAD&&e.getValue().status!=Status.EXITED).map(e->Bukkit.getPlayer(e.getKey())).filter(p->p!=null&&!p.isDead()).toList();}
    private List<Player> playersAll(Run r){return r.participants.keySet().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).toList();}
    public void status(CommandSender sender,String id){Definition d=get(id);Run r=state.runs.get(id);tell(sender,d.id+" • "+(d.enabled?"ON":"OFF")+" • kaca "+clock(d.glassSeconds)+" • ulang "+d.retryLimit()+" kali • "+(r==null?"belum ada sesi":r.phase+" • peserta "+r.participants.size()+" • mob "+r.killed+"/"+r.total)+(r!=null&&r.restoring?" • memulihkan blok":""));}
    private void recover(Player p){
        try{for(Run r:state.runs.values()){Participant part=r.participants.get(p.getUniqueId());if(part!=null&&part.mode!=null)camera.restore(p,part);}
            NpcData.Point home=state.returns.get(p.getUniqueId());if(home!=null&&!p.isDead()&&teleport(p,location(home))){state.returns.remove(p.getUniqueId());persist();tell(p,"Sesi sebelumnya dihentikan; kamu dipulangkan dengan aman.");}}
        catch(RuntimeException error){fault(error);tell(p,"Pemulihan dungeon tertunda; minta admin memeriksa tujuan pulang.");}
    }
    private void title(Player p,String title,String sub){p.showTitle(Title.title(Component.text(title,NamedTextColor.RED),Component.text(sub,NamedTextColor.YELLOW),Title.Times.times(Duration.ofMillis(200),Duration.ofSeconds(3),Duration.ofMillis(500))));}
    private void broadcast(Run r,String text){for(Player p:playersAll(r))tell(p,text);}
    public static void tell(CommandSender s,String text){s.sendMessage(Component.text("[Vitae Dungeon] ",NamedTextColor.GOLD).append(Component.text(text,NamedTextColor.AQUA)));}
    private void tellAdmins(String text){plugin.getLogger().warning(text);for(Player p:Bukkit.getOnlinePlayers())if(p.hasPermission("vitae.admin.dungeon"))tell(p,text);}
    private void fault(Throwable e){plugin.getLogger().log(java.util.logging.Level.SEVERE,"Dungeon: "+e.getMessage(),e);}
    @EventHandler public void action(StoryNpcActionEvent e){
        Definition selected=null;Phase before=null;
        try{
            if(e.action().startsWith("ACTION_")){standalone(e);return;}
            Definition d=state.definitions.values().stream().filter(v->v.enabled&&List.of(v.glassNpc,v.minionNpc,v.bossNpc).contains(e.npc())).findFirst().orElseThrow(()->new IllegalStateException("NPC belum dihubungkan ke dungeon aktif."));
            selected=d;Run existing=state.runs.get(d.id);before=existing==null?null:existing.phase;
            switch(e.action()){
                case "DUNGEON_GLASS"->{if(!e.npc().equals(d.glassNpc))throw new IllegalStateException("NPC ini bukan pemberi kaca.");glass(e.player(),d);}
                case "DUNGEON_MINION"->{if(!e.npc().equals(d.minionNpc))throw new IllegalStateException("NPC ini bukan pemberi minion.");accept(e.player(),d,false);}
                case "DUNGEON_BOSS"->{if(!e.npc().equals(d.bossNpc))throw new IllegalStateException("NPC ini bukan pemberi bos.");accept(e.player(),d,true);}
                case "DUNGEON_REWARD"->claim(e.player(),d,e.npc());
                default->throw new IllegalArgumentException("Aksi dungeon tidak dikenal.");
            }
        }catch(RuntimeException error){tell(e.player(),Objects.requireNonNullElse(error.getMessage(),"Aksi tidak dapat dijalankan."));if(!(error instanceof IllegalArgumentException))plugin.getLogger().warning(error.toString());
            if(selected!=null){Run r=state.runs.get(selected.id);if(r!=null&&r.active()&&r.phase!=before)stop(selected.id,"Adegan gagal dimulai; peserta dan arena dipulihkan.");}}
    }
    @EventHandler public void dialog(StoryNpcDialogEvent e){
        Definition d=state.definitions.values().stream().filter(v->v.enabled&&List.of(v.glassNpc,v.minionNpc,v.bossNpc).contains(e.npc())).findFirst().orElse(null);
        if(d==null){if(state.actions.values().stream().anyMatch(a->a.enabled&&a.npc.equals(e.npc())))e.managed(true);return;}e.managed(true);Run r=state.runs.get(d.id);Participant part=r==null?null:r.participants.get(e.player().getUniqueId());
        String refusal=null;
        if(r==null||!r.active())refusal="Dungeon belum dibuka. Tunggu pengelola memulai sesi.";
        else if(part==null||part.status==Status.DEAD||part.status==Status.EXITED)refusal="Kamu bukan peserta aktif atau telah gagal pada sesi ini.";
        else {
            for(Award a:r.awards.values())if(a.npc.equals(e.npc())&&a.player.equals(e.player().getUniqueId())&&a.state==ReceiptState.RESERVED&&!a.stage.endsWith("request")){
                String text=a.page.isEmpty()?"Selamat! Tantanganmu selesai. Ambillah hadiahmu.":npc().get(a.npc).nodes().get(a.page).text();
                e.node(new NpcData.Node(text,List.of(new NpcData.Choice("Ambil hadiah","DUNGEON_REWARD"))));return;
            }
            if(e.npc().equals(d.glassNpc)&&r.phase!=Phase.WAITING)refusal="Permainan kaca sudah dimulai. Lanjutkan perjalananmu.";
            else if(e.npc().equals(d.minionNpc)&&r.phase!=Phase.GLASS_DONE)refusal=r.phase==Phase.MINION||r.phase==Phase.MINION_READY?"Musnahkan semua minion bersama rekanmu.":"Selesaikan permainan kaca terlebih dahulu.";
            else if(e.npc().equals(d.bossNpc)&&r.phase!=Phase.MINION_DONE)refusal=r.phase==Phase.BOSS||r.phase==Phase.BOSS_READY?"Bos menunggu. Selesaikan pertarungan bersama rekanmu.":"Selesaikan permainan kaca dan seluruh minion terlebih dahulu.";
        }
        if(refusal!=null)e.node(new NpcData.Node(refusal,List.of()));
    }
    public void actionEdit(Action action)throws IOException{NpcData.id(action.id);NpcData.id(action.npc);npc().get(action.npc);if(action.mob!=null)mobs.validate(action.mob);if(action.point!=null)safe(action.point);State copy=draft();copy.actions.put(action.id,action);copy.validate();store.save(copy);state=copy;}
    private void standalone(StoryNpcActionEvent e){Action a=state.actions.get(e.action().substring(7));if(a==null||!a.enabled||!a.npc.equals(e.npc()))throw new IllegalStateException("Aksi NPC tidak aktif atau ID tidak cocok.");
        if(tick-actionClicks.getOrDefault(e.player().getUniqueId(),-100L)<40)return;actionClicks.put(e.player().getUniqueId(),tick);
        if(a.kind.equals("teleport")){fade.teleport(e.player(),location(a.point),a.fade,ok->{if(!ok)tell(e.player(),"Teleport ditolak.");});}
        else {for(int i=0;i<a.mob.count();i++)mobs.spawn(a.mob,null);tell(e.player(),"Mob telah dipanggil.");}
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void move(PlayerMoveEvent e){
        if(e instanceof PlayerTeleportEvent||e.getTo()==null||internal.contains(e.getPlayer().getUniqueId()))return;Player p=e.getPlayer();if(bypass(p))return;
        try{
            if(!fade.busy(p)&&portals.getOrDefault(p.getUniqueId(),0L)<=tick)for(Definition d:state.definitions.values())if(d.enabled&&d.entrance.crosses(e.getFrom(),e.getTo())){portals.put(p.getUniqueId(),tick+40);e.setTo(e.getFrom());Bukkit.getScheduler().runTask(plugin,()->{try{join(p,d.id);}catch(RuntimeException error){tell(p,error.getMessage());}});return;}
            Definition d=definition(p.getWorld());if(d==null)return;Run r=state.runs.get(d.id);if(r==null||!r.active())return;Participant part=r.participants.get(p.getUniqueId());if(part==null||part.status==Status.DEAD||part.status==Status.EXITED)return;
            if(camera.busy(p.getUniqueId()))return;
            if(part.falling){Location to=e.getTo().clone();to.setX(e.getFrom().getX());to.setZ(e.getFrom().getZ());to.setY(Math.min(to.getY(),e.getFrom().getY()));e.setTo(to);return;}
            if(r.phase==Phase.GLASS&&part.status==Status.ACTIVE){
                if(System.currentTimeMillis()>=r.deadline){e.setTo(e.getFrom());failed(p,"Waktu kaca habis. Kamu gagal.",true);return;}
                for(var tile:d.bad.entrySet()){
                    Box b=tile.getValue();Box surface=new Box(b.world(),b.x1(),b.y1()+1,b.z1(),b.x2(),b.y2()+1,b.z2());
                    if(surface.crosses(e.getFrom(),e.getTo()) && e.getTo().getY()<=b.y1()+1.15 && e.getTo().getY()>=b.y1()-.1){
                        e.setTo(breakTile(p,r,d,tile.getKey(),b));return;}
                }
                if(d.finish.crosses(e.getFrom(),e.getTo())){part.status=Status.PASSED;persist();finishes.computeIfAbsent(d.id,id->new HashSet<>()).add(p.getUniqueId());decideAt.putIfAbsent(d.id,tick+1);e.setTo(e.getFrom());Bukkit.getScheduler().runTask(plugin,()->{if(r.active()&&part.status==Status.PASSED&&!p.isDead())teleport(p,location(d.next));});return;}
            }
            if(part.status==Status.PASSED){if(r.phase==Phase.MINION_READY&&d.minionTrigger.crosses(e.getFrom(),e.getTo()))spawn(r,d,false);else if(r.phase==Phase.BOSS_READY&&d.bossTrigger.crosses(e.getFrom(),e.getTo()))spawn(r,d,true);
                else if(r.phase==Phase.ESCAPE&&r.broken.contains("exit-open")&&d.exit.crosses(e.getFrom(),e.getTo())&&portals.getOrDefault(p.getUniqueId(),0L)<=tick){portals.put(p.getUniqueId(),tick+40);e.setTo(e.getFrom());Bukkit.getScheduler().runTask(plugin,()->escape(p,d,r));}}
        }catch(RuntimeException error){e.setTo(e.getFrom());warn(p,error.getMessage());}
    }
    private void warn(Player p,String text){if(tick-warns.getOrDefault(p.getUniqueId(),-100L)>40){tell(p,Objects.requireNonNullElse(text,"Aksi ditolak."));warns.put(p.getUniqueId(),tick);}}
    @EventHandler(priority=EventPriority.HIGHEST)public void teleportEvent(PlayerTeleportEvent e){
        Player p=e.getPlayer();if(bypass(p)||internal.contains(p.getUniqueId()))return;
        if(e.getCause()==PlayerTeleportEvent.TeleportCause.END_GATEWAY){for(Definition d:state.definitions.values())if(d.enabled&&(d.entrance.contains(e.getFrom())||d.exit.contains(e.getFrom()))){e.setCancelled(true);
            if(!fade.busy(p)&&portals.getOrDefault(p.getUniqueId(),0L)<=tick){Run r=state.runs.get(d.id);
                if(d.entrance.contains(e.getFrom())){portals.put(p.getUniqueId(),tick+40);Bukkit.getScheduler().runTask(plugin,()->{try{join(p,d.id);}catch(RuntimeException error){tell(p,error.getMessage());}});}
                else if(r!=null&&r.phase==Phase.ESCAPE&&r.broken.contains("exit-open")){portals.put(p.getUniqueId(),tick+40);Bukkit.getScheduler().runTask(plugin,()->escape(p,d,r));}}
            return;}}
        Definition d=definition(p.getWorld());if(d==null)return;Run r=state.runs.get(d.id);if(r==null||!r.active())return;
        Participant part=r.participants.get(p.getUniqueId());
        if(e.getCause()==PlayerTeleportEvent.TeleportCause.ENDER_PEARL||e.getCause()==PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT||camera.busy(p.getUniqueId())||(part!=null&&part.falling)){e.setCancelled(true);return;}
        // Successful external world changes are handled after the teleport completes.
    }
    @EventHandler public void worldChanged(PlayerChangedWorldEvent e){Player p=e.getPlayer();if(bypass(p)||internal.contains(p.getUniqueId())||fade.busy(p))return;Definition d=definition(e.getFrom());if(d==null)return;Run r=state.runs.get(d.id);if(r==null||!r.active())return;Participant part=r.participants.get(p.getUniqueId());if(part!=null&&part.status!=Status.DEAD&&part.status!=Status.EXITED){part.status=Status.DEAD;persist();tell(p,"Kamu meninggalkan dungeon dan dianggap gagal pada sesi ini.");}}
    @EventHandler(priority=EventPriority.HIGHEST)public void glassDamage(EntityDamageEvent e){
        if(!(e.getEntity() instanceof Player p)||bypass(p)||internal.contains(p.getUniqueId())||camera.busy(p.getUniqueId()))return;
        if(e.getCause()!=EntityDamageEvent.DamageCause.FALL&&e.getCause()!=EntityDamageEvent.DamageCause.VOID)return;
        Definition d=definition(p.getWorld());if(d==null)return;Run r=state.runs.get(d.id);if(r==null||!r.active()||r.phase!=Phase.GLASS)return;
        Participant part=r.participants.get(p.getUniqueId());if(part==null||part.status!=Status.ACTIVE)return;
        if(!part.falling&&e.getCause()!=EntityDamageEvent.DamageCause.VOID&&p.getY()>=glassFloor(d))return;
        // Return before vanilla fall/void damage can kill the player or consume a totem.
        e.setCancelled(true);
        try{glassFall(p,r,d,part);}catch(RuntimeException error){fault(error);stop(d.id,"Posisi kembali kaca tidak aman. Admin perlu memeriksa location start.");}
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)public void died(PlayerDeathEvent e){hardDeaths.remove(e.getEntity().getUniqueId());failed(e.getEntity(),"Kamu gugur dan tidak dapat masuk kembali pada sesi ini.",false);}
    @EventHandler(priority=EventPriority.HIGHEST)public void resurrect(EntityResurrectEvent e){if(hardDeaths.contains(e.getEntity().getUniqueId()))e.setCancelled(true);}
    @EventHandler public void quit(PlayerQuitEvent e){Player p=e.getPlayer();Definition d=definition(p.getWorld());if(d!=null){Run r=state.runs.get(d.id);if(r!=null&&r.phase!=Phase.WAITING){failed(p,"",false);state.returns.put(p.getUniqueId(),d.home);persist();}}portals.remove(p.getUniqueId());warns.remove(p.getUniqueId());}
    @EventHandler public void joined(PlayerJoinEvent e){Bukkit.getScheduler().runTask(plugin,()->recover(e.getPlayer()));}
    @EventHandler public void respawn(PlayerRespawnEvent e){hardDeaths.remove(e.getPlayer().getUniqueId());Bukkit.getScheduler().runTask(plugin,()->recover(e.getPlayer()));}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)public void mobDeath(EntityDeathEvent e){
        UUID id=e.getEntity().getUniqueId();for(Run r:state.runs.values())if(r.active()&&r.mobs.containsKey(id)){
            Bukkit.getScheduler().runTask(plugin,()->{if(closed||e.isCancelled()||!r.active()||r.mobs.remove(id)==null)return;r.killed++;persist();broadcast(r,"Progres bersama: "+r.killed+"/"+r.total);if(r.killed>=r.total&&!spawnQueues.containsKey(r.dungeon))combatComplete(r,get(r.dungeon));});return;
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void mobSpawn(CreatureSpawnEvent e){if(definition(e.getLocation().getWorld())!=null&&e.getSpawnReason()!=CreatureSpawnEvent.SpawnReason.CUSTOM)e.setCancelled(true);}
    @EventHandler public void worldLoad(WorldLoadEvent e){if(definition(e.getWorld())!=null)e.getWorld().setGameRule(GameRule.DO_MOB_SPAWNING,false);}
    @EventHandler public void chunkLoad(ChunkLoadEvent e){if(definition(e.getWorld())==null)return;for(Entity entity:e.getChunk().getEntities()){String token=entity.getPersistentDataContainer().get(mobs.sessionKey,PersistentDataType.STRING);if(token!=null&&state.runs.values().stream().noneMatch(r->r.active()&&r.token.toString().equals(token)))mobs.remove(entity.getUniqueId());}}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void blockBreak(BlockBreakEvent e){if(carryBlocked(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void blockPlace(BlockPlaceEvent e){if(carryBlocked(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void interact(PlayerInteractEvent e){if(!carryBlocked(e.getPlayer()))return;ItemStack i=e.getItem();if(i!=null&&(Set.of(Material.ENDER_PEARL,Material.CHORUS_FRUIT,Material.WATER_BUCKET,Material.LAVA_BUCKET,Material.BUCKET).contains(i.getType())||i.getType().name().endsWith("BOAT")||i.getType().name().endsWith("RAFT")||i.getType().name().endsWith("MINECART"))){e.setCancelled(true);warn(e.getPlayer(),"Item ini tidak boleh digunakan dalam dungeon.");}}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void consume(PlayerItemConsumeEvent e){if(carryBlocked(e.getPlayer())&&e.getItem().getType()==Material.CHORUS_FRUIT)e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void launch(ProjectileLaunchEvent e){if(e.getEntity() instanceof EnderPearl&&e.getEntity().getShooter() instanceof Player p&&carryBlocked(p))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void glide(EntityToggleGlideEvent e){if(e.getEntity() instanceof Player p&&carryBlocked(p)&&e.isGliding())e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void fly(PlayerToggleFlightEvent e){if(carryBlocked(e.getPlayer())&&!camera.busy(e.getPlayer().getUniqueId())&&e.isFlying())e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void mount(EntityMountEvent e){if(e.getEntity() instanceof Player p&&carryBlocked(p))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void explode(EntityExplodeEvent e){if(definition(e.getLocation().getWorld())!=null)e.blockList().clear();}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void explode(BlockExplodeEvent e){if(definition(e.getBlock().getWorld())!=null)e.blockList().clear();}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void entityBlock(EntityChangeBlockEvent e){if(definition(e.getBlock().getWorld())!=null)e.setCancelled(true);}
    public void rewardMenu(Player p,String id,String stage){get(id);if(!Set.of("glass","minion","boss").contains(stage))throw new IllegalArgumentException("Tahap glass|minion|boss.");Run r=state.runs.get(id);if(r!=null&&r.active())throw new IllegalStateException("Reset sesi sebelum mengganti hadiah.");p.openInventory(new RewardMenu(p.getUniqueId(),id,stage,StoryStore.JSON.toJson(get(id))).inventory);}
    private static final class RewardMenu implements VitaeMenu{final UUID admin;final String id,stage,before;final Inventory inventory;
        RewardMenu(UUID admin,String id,String stage,String before){this.admin=admin;this.id=id;this.stage=stage;this.before=before;inventory=Bukkit.createInventory(this,27,Component.text("Hadiah "+stage+" • tutup untuk simpan"));}@Override public Inventory getInventory(){return inventory;}}
    @EventHandler public void rewardClose(InventoryCloseEvent e){if(!(e.getInventory().getHolder() instanceof RewardMenu menu)||!(e.getPlayer() instanceof Player p))return;var items=new ArrayList<String>();
        for(ItemStack item:e.getInventory().getContents())if(item!=null&&!item.getType().isAir()){ItemStack copy=item.clone();items.add(Base64.getEncoder().encodeToString(copy.serializeAsBytes()));for(ItemStack overflow:p.getInventory().addItem(copy).values())p.getWorld().dropItem(p.getLocation(),overflow).setOwner(p.getUniqueId());}e.getInventory().clear();
        if(closed)return;try{if(!p.getUniqueId().equals(menu.admin)||!p.hasPermission("vitae.admin.dungeon")||!menu.before.equals(StoryStore.JSON.toJson(get(menu.id))))throw new IllegalStateException("Setup/izin berubah; hadiah tidak disimpan.");edit(menu.id,d->d.rewards.put(menu.stage,items));tell(p,"Hadiah "+menu.stage+" tersimpan. Item admin dikembalikan; dungeon OFF sampai setup diaktifkan.");}catch(Exception error){tell(p,error.getMessage());}}
    @Override public void close(){
        closed=true;for(var scan:scans.values())scan.cancel();scans.clear();if(task!=null)task.cancel();for(String id:List.copyOf(bars.keySet()))removeBar(id);
        for(Run r:state.runs.values())if(r.active()){r.phase=Phase.ABORTED;r.restoring=true;for(var e:r.participants.entrySet())if(e.getValue().status!=Status.DEAD&&e.getValue().status!=Status.EXITED)state.returns.put(e.getKey(),get(r.dungeon).home);}
        camera.close();try{save();}catch(IOException e){fault(e);}for(Run r:state.runs.values())if(r.restoring){for(UUID id:r.mobs.keySet())mobs.remove(id);}
        for(String id:List.copyOf(tickets.keySet()))releaseTickets(id);mobs.close();
        // Remaining block snapshots are deliberately retained for next startup recovery.
        HandlerList.unregisterAll(this);
    }
}
