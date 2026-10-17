package com.bangzachery.vitae.vitaemanager.story;

import com.bangzachery.vitae.vitaemanager.npc.NpcData;
import org.bukkit.*;
import java.util.*;

public final class DungeonData {
    private DungeonData() {}
    public enum Phase { WAITING, CAMERA, GLASS, GLASS_DONE, MINION_READY, MINION, MINION_DONE, BOSS_READY, BOSS, REWARD, ESCAPE, COMPLETE, ABORTED }
    public enum Status { ACTIVE, PASSED, DEAD, EXITED }
    public enum ReceiptState { RESERVED, DELIVERING, CLAIMED }
    public record Box(UUID world, int x1, int y1, int z1, int x2, int y2, int z2) {
        public Box {
            Objects.requireNonNull(world);
            if (x1 > x2 || y1 > y2 || z1 > z2 || Math.abs((long)x1) > 30_000_000 || Math.abs((long)x2) > 30_000_000
                    || Math.abs((long)z1) > 30_000_000 || Math.abs((long)z2) > 30_000_000 || y1 < -4096 || y2 > 4096)
                throw new IllegalArgumentException("Seleksi area tidak valid.");
        }
        public static Box between(Location a, Location b) {
            if (!a.getWorld().equals(b.getWorld())) throw new IllegalArgumentException("Pos1 dan pos2 harus satu world.");
            return new Box(a.getWorld().getUID(), Math.min(a.getBlockX(),b.getBlockX()), Math.min(a.getBlockY(),b.getBlockY()), Math.min(a.getBlockZ(),b.getBlockZ()),
                    Math.max(a.getBlockX(),b.getBlockX()), Math.max(a.getBlockY(),b.getBlockY()), Math.max(a.getBlockZ(),b.getBlockZ()));
        }
        public boolean contains(Location p) { return p.getWorld() != null && world.equals(p.getWorld().getUID()) && contains(p.getX(),p.getY(),p.getZ()); }
        public boolean contains(double x,double y,double z) { return x >= x1 && x < x2+1.0 && y >= y1 && y < y2+1.0 && z >= z1 && z < z2+1.0; }
        public boolean contains(Box b) { return world.equals(b.world) && x1 <= b.x1 && x2 >= b.x2 && y1 <= b.y1 && y2 >= b.y2 && z1 <= b.z1 && z2 >= b.z2; }
        public boolean overlaps(Box b) { return world.equals(b.world) && x1 <= b.x2 && x2 >= b.x1 && y1 <= b.y2 && y2 >= b.y1 && z1 <= b.z2 && z2 >= b.z1; }
        public long volume() { return com.bangzachery.vitae.vitaemanager.selection.AreaScanner.volume(com.bangzachery.vitae.vitaemanager.whisper.WhisperArea.between(world,x1,y1,z1,x2,y2,z2)); }
        public boolean crosses(Location a, Location b) {
            if (a.getWorld() == null || b.getWorld() == null || !world.equals(a.getWorld().getUID()) || !world.equals(b.getWorld().getUID())) return false;
            double lo=0,hi=1; double[] start={a.getX(),a.getY(),a.getZ()}, end={b.getX(),b.getY(),b.getZ()}, min={x1,y1,z1}, max={x2+1.0,y2+1.0,z2+1.0};
            for(int i=0;i<3;i++) { double d=end[i]-start[i]; if(Math.abs(d)<1e-9) { if(start[i]<min[i] || start[i]>=max[i])return false; }
                else { double t1=(min[i]-start[i])/d,t2=(max[i]-start[i])/d;lo=Math.max(lo,Math.min(t1,t2));hi=Math.min(hi,Math.max(t1,t2));if(lo>hi)return false; } }
            return true;
        }
    }
    public record Block(UUID world,int x,int y,int z,String data) {
        public Block{Objects.requireNonNull(world);if(Math.abs((long)x)>30_000_000||Math.abs((long)z)>30_000_000||Math.abs((long)y)>4096||data==null||data.isBlank()||data.length()>4096)throw new IllegalArgumentException("Snapshot blok tidak valid.");}
        public String key() { return world+":"+x+":"+y+":"+z; }
        public Location location() { World w=Bukkit.getWorld(world); if(w==null)throw new IllegalStateException("World snapshot belum dimuat.");return new Location(w,x,y,z); }
    }
    public record Mob(String type, int count, double level, NpcData.Point point) {
        public Mob { if(type==null || !type.matches("(vanilla|mythic):[A-Za-z0-9_-]{1,100}") || count<1 || count>4096 || !Double.isFinite(level) || level<=0 || level>10000 || point==null)
            throw new IllegalArgumentException("Mob: vanilla:COW atau mythic:ID, jumlah 1–4096, level > 0."); }
    }
    public static final class Definition {
        public String id;
        public boolean enabled;
        public UUID world;
        public Box entrance,arena,cameraZone,finish,minionGate,minionTrigger,bossGate,bossTrigger,exit;
        public NpcData.Point entry,home,start,camera,next;
        public Map<String,Box> bad = new LinkedHashMap<>();
        public Map<String,Mob> minions = new LinkedHashMap<>();
        public Map<String,List<String>> rewards = new LinkedHashMap<>();
        public Mob boss;
        public String glassNpc="",minionNpc="",bossNpc="";
        public String glassPage="",minionPage="",bossPage="";
        public int glassSeconds=180,cameraTicks=100,escapeSeconds=30,portalAfter=15;
        // Nullable so old JSON without this field keeps the default, including Gson's unsafe construction.
        public Integer glassRetries=3;
        public int retryLimit() { return glassRetries==null?3:glassRetries; }
        public boolean fadeEntry,fadeExit;
        public Definition(String id,UUID world) { this.id=NpcData.id(id);this.world=world; }
        public void validate() {
            NpcData.id(id);Objects.requireNonNull(world,"World dungeon belum diatur.");
            if(bad==null || minions==null || rewards==null || glassSeconds<10 || retryLimit()<0 || cameraTicks!=100 || escapeSeconds<5 || escapeSeconds>300 || portalAfter<1 || portalAfter>=escapeSeconds)
                throw new IllegalArgumentException("Pengaturan waktu dungeon tidak valid.");
            if(!enabled)return;
            Object[] required={entrance,arena,cameraZone,finish,minionGate,minionTrigger,bossGate,bossTrigger,exit,entry,home,start,camera,next,boss};
            String[] names={"region entrance","region arena","region camera","region finish","region miniongate","region miniontrigger","region bossgate","region bosstrigger","region exit","location entry","location home","location start","location camera","location next","boss"};
            var missing=new ArrayList<String>();for(int i=0;i<required.length;i++)if(required[i]==null)missing.add(names[i]);
            if(!missing.isEmpty())throw new IllegalArgumentException("Setup belum lengkap: "+String.join(", ",missing));
            for(Box b:List.of(arena,cameraZone,finish,minionGate,minionTrigger,bossGate,bossTrigger,exit)) if(!world.equals(b.world))throw new IllegalArgumentException("Area permainan harus berada di world dungeon.");
            for(NpcData.Point p:List.of(entry,start,camera,next,boss.point()))if(!world.equals(p.world()))throw new IllegalArgumentException("Lokasi permainan harus berada di world dungeon.");
            if(world.equals(home.world()))throw new IllegalArgumentException("Lokasi home harus di luar world dungeon.");
            if(!cameraZone.contains(location(start)))throw new IllegalArgumentException("Area kamera harus mencakup posisi start agar pengenalan dimulai serentak.");
            if(!arena.contains(cameraZone) || !arena.contains(finish) || !arena.contains(location(start)))throw new IllegalArgumentException("Start, kamera pemicu, dan finish harus di dalam arena kaca.");
            if(minionGate.overlaps(bossGate) || exit.overlaps(minionGate) || exit.overlaps(bossGate))throw new IllegalArgumentException("Gerbang dan portal keluar tidak boleh bertumpuk.");
            if(bad.isEmpty() || minions.isEmpty() || minions.values().stream().mapToLong(Mob::count).sum()>8192)throw new IllegalArgumentException("Isi pijakan salah dan spawn minion (total maks. 8192). Batas ini hanya mob aktif, bukan luas world.");
            for(Box b:bad.values()) if(!arena.contains(b) || b.y1!=b.y2 || b.overlaps(finish))throw new IllegalArgumentException("Pijakan salah harus satu lapis kaca di arena dan tidak menimpa finish.");
            var list=new ArrayList<>(bad.values());for(int i=0;i<list.size();i++)for(int j=i+1;j<list.size();j++)if(list.get(i).overlaps(list.get(j)))throw new IllegalArgumentException("Pijakan salah bertumpuk.");
            for(Mob m:minions.values())if(!world.equals(m.point.world()))throw new IllegalArgumentException("Spawn minion berada di world lain.");
            for(String npc:List.of(glassNpc,minionNpc,bossNpc))NpcData.id(npc);
            for(String stage:List.of("glass","minion","boss"))if(!rewards.containsKey(stage)||rewards.get(stage).isEmpty()||rewards.get(stage).size()>27)throw new IllegalArgumentException("Hadiah item "+stage+" belum disetel.");
            if(new HashSet<>(List.of(glassNpc,minionNpc,bossNpc)).size()!=3)throw new IllegalArgumentException("Gunakan tiga NPC berbeda untuk tiga tahap.");
        }
    }
    public static final class Participant {
        public Status status=Status.ACTIVE;
        public boolean falling,cameraDone;
        public int glassReturnsUsed;
        public Double glassFallY;
        public NpcData.Point recovery;
        public String mode;
        public boolean flight,flying;
        public Participant() {}
    }
    public static final class Award {
        public UUID receipt=UUID.randomUUID(),player;
        public String stage,npc,page="";
        public List<String> items=new ArrayList<>();
        public ReceiptState state=ReceiptState.RESERVED;
        public long chosenAt;
    }
    public static final class Run {
        public UUID token=UUID.randomUUID();
        public String dungeon;
        public Phase phase=Phase.WAITING;
        public Map<UUID,Participant> participants=new LinkedHashMap<>();
        public List<Block> blocks=new ArrayList<>();
        public Map<UUID,String> mobs=new LinkedHashMap<>();
        public Map<String,Award> awards=new LinkedHashMap<>();
        public Set<String> broken=new HashSet<>();
        public long deadline;
        public int killed,total;
        public boolean restoring;
        public Run(String id) {dungeon=id;}
        public boolean active() {return phase!=Phase.COMPLETE && phase!=Phase.ABORTED;}
    }
    public static final class Action {
        public String id,npc,kind;
        public boolean fade;
        public NpcData.Point point;
        public Mob mob;
        public boolean enabled;
    }
    public static final class State {
        public int version=1;
        public Map<String,Definition> definitions=new LinkedHashMap<>();
        public Map<String,Run> runs=new LinkedHashMap<>();
        public Map<String,Action> actions=new LinkedHashMap<>();
        public Map<UUID,NpcData.Point> returns=new LinkedHashMap<>();
        public void validate() {
            if(version!=1 || definitions==null || runs==null || actions==null || returns==null)throw new IllegalArgumentException("Format dungeon tidak valid.");
            definitions.forEach((id,d)->{if(!id.equals(d.id))throw new IllegalArgumentException("ID dungeon tidak cocok.");d.validate();});
            runs.forEach((id,r)->{if(!id.equals(r.dungeon) || !definitions.containsKey(id) || r.token==null || r.phase==null || r.participants==null || r.blocks==null || r.mobs==null || r.awards==null || r.broken==null || r.killed<0 || r.total<0 || r.killed>r.total)
                throw new IllegalArgumentException("State sesi dungeon tidak valid.");
                r.participants.forEach((uuid,p)->{Objects.requireNonNull(uuid);if(p==null||p.status==null||p.glassReturnsUsed<0||(p.glassFallY!=null&&!Double.isFinite(p.glassFallY)))throw new IllegalArgumentException("Peserta sesi tidak valid.");if(p.mode!=null)GameMode.valueOf(p.mode);});
                r.awards.forEach((stage,a)->{if(a==null||a.receipt==null||a.player==null||a.state==null||!stage.equals(a.stage)||a.items==null||a.items.size()>27)throw new IllegalArgumentException("Receipt hadiah tidak valid.");});
            });
            actions.forEach((id,a)->{NpcData.id(id);NpcData.id(a.npc);if(!id.equals(a.id) || !("teleport".equals(a.kind)||"spawn".equals(a.kind)) || ("teleport".equals(a.kind)?a.point==null:a.mob==null))throw new IllegalArgumentException("Aksi NPC tidak valid.");});
        }
    }
    public static NpcData.Point point(Location l) {return new NpcData.Point(l.getWorld().getUID(),l.getX(),l.getY(),l.getZ(),l.getYaw(),l.getPitch());}
    public static Location location(NpcData.Point p) {
        if(p==null)throw new IllegalArgumentException("Lokasi belum disetel.");World w=Bukkit.getWorld(p.world());
        if(w==null)throw new IllegalStateException("World belum dimuat: "+p.world());return new Location(w,p.x(),p.y(),p.z(),p.yaw(),p.pitch());
    }
}
