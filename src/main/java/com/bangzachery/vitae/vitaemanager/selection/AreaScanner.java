package com.bangzachery.vitae.vitaemanager.selection;

import com.bangzachery.vitae.vitaemanager.whisper.WhisperArea;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;

/** Shared main-thread budget; only the currently visited chunk stays pinned. */
public final class AreaScanner implements AutoCloseable {
    public static final class Cursor {
        private final WhisperArea area;
        private int cx,cz,x,y,z; private long visited; private boolean done;
        public Cursor(WhisperArea area){this.area=area;cx=area.minX()>>4;cz=area.minZ()>>4;first();}
        private void first(){x=Math.max(area.minX(),cx<<4);z=Math.max(area.minZ(),cz<<4);y=area.minY();}
        public int x(){return x;}public int y(){return y;}public int z(){return z;}
        public boolean done(){return done;}public long visited(){return visited;}
        public void next(){if(done)return;if(visited<Long.MAX_VALUE)visited++;if(++y<=area.maxY())return;y=area.minY();
            if(++x<=Math.min(area.maxX(),(cx<<4)+15))return;x=Math.max(area.minX(),cx<<4);
            if(++z<=Math.min(area.maxZ(),(cz<<4)+15))return;
            if(++cx>(area.maxX()>>4)){cx=area.minX()>>4;cz++;}if(cz>(area.maxZ()>>4)){done=true;return;}first();}
    }
    public static long volume(WhisperArea a){
        long n=1;for(long d:new long[]{(long)a.maxX()-a.minX()+1,(long)a.maxY()-a.minY()+1,(long)a.maxZ()-a.minZ()+1}){
            if(n>Long.MAX_VALUE/d)return Long.MAX_VALUE;n*=d;}return n;
    }
    private final JavaPlugin plugin;private final Deque<Job> jobs=new ArrayDeque<>();private final Map<Chunk,Integer> pins=new HashMap<>();private BukkitTask task;private boolean closed;
    public final class Handle {private final Job job;private Handle(Job job){this.job=job;}public Handle owned(UUID owner){job.owner=owner;return this;}public void cancel(){job.cancelled=true;jobs.remove(job);release(job);}public boolean done(){return job.done||job.cancelled;}}
    private static final class Job {
        final WhisperArea area;final Cursor cursor;final BooleanSupplier active;final BiConsumer<Long,Long> progress;
        final Consumer<Block> visit;final Runnable success;final Consumer<String> failure;
        UUID owner;Chunk pinned;CompletableFuture<Chunk> loading;long since,lastProgress;boolean done,cancelled;
        Job(WhisperArea a,BooleanSupplier active,BiConsumer<Long,Long> progress,Consumer<Block> visit,Runnable success,Consumer<String> failure){area=a;cursor=new Cursor(a);this.active=active;this.progress=progress;this.visit=visit;this.success=success;this.failure=failure;}
    }
    public AreaScanner(JavaPlugin plugin){this.plugin=plugin;}
    public void start(){task=Bukkit.getScheduler().runTaskTimer(plugin,this::tick,1,1);}
    public Handle scan(WhisperArea area,BooleanSupplier active,BiConsumer<Long,Long> progress,Consumer<Block> visit,Runnable success,Consumer<String> failure){
        if(closed)throw new IllegalStateException("Pemrosesan area sedang ditutup.");World w=Objects.requireNonNull(Bukkit.getWorld(area.world()),"World area belum dimuat.");
        if(area.minY()<w.getMinHeight()||area.maxY()>=w.getMaxHeight())throw new IllegalArgumentException("Seleksi harus di dalam ketinggian world: "+w.getMinHeight()+" sampai "+(w.getMaxHeight()-1)+".");
        Job j=new Job(area,active,progress,visit,success,failure);jobs.add(j);return new Handle(j);
    }
    public void cancelOwner(UUID owner){for(Job j:List.copyOf(jobs))if(owner.equals(j.owner)){j.cancelled=true;jobs.remove(j);release(j);j.failure.accept("Pemindaian dibatalkan admin; setup lama dipertahankan.");}}
    private void pin(Job j,Chunk c){if(pins.merge(c,1,Integer::sum)==1)c.addPluginChunkTicket(plugin);j.pinned=c;}
    private void release(Job j){if(j.pinned!=null){Chunk c=j.pinned;if(pins.compute(c,(k,n)->n==null||n<=1?null:n-1)==null)c.removePluginChunkTicket(plugin);j.pinned=null;}}
    private boolean ready(Job j,World w){
        int cx=j.cursor.x()>>4,cz=j.cursor.z()>>4;if(j.pinned!=null&&j.pinned.getX()==cx&&j.pinned.getZ()==cz)return true;
        release(j);
        if(j.loading!=null){if(!j.loading.isDone()){if(System.currentTimeMillis()-j.since>60000)throw new IllegalStateException("Chunk area belum selesai dimuat. Coba kembali setelah world siap.");return false;}
            Chunk c=j.loading.join();j.loading=null;if(c==null)throw new IllegalStateException("Chunk belum tersedia. Bangun area tersebut dahulu.");pin(j,c);return true;}
        if(w.isChunkLoaded(cx,cz)){pin(j,w.getChunkAt(cx,cz));return true;}
        j.since=System.currentTimeMillis();j.loading=w.getChunkAtAsync(cx,cz,false);return false;
    }
    private void tick(){
        int count=0,idle=0;long until=System.nanoTime()+3_000_000;
        while(!jobs.isEmpty()&&count<512&&System.nanoTime()<until){Job j=jobs.removeFirst();if(j.cancelled){release(j);continue;}
            try{if(!j.active.getAsBoolean())throw new IllegalStateException("Pemrosesan dibatalkan; setup lama dipertahankan.");World w=Bukkit.getWorld(j.area.world());if(w==null)throw new IllegalStateException("World area tidak lagi dimuat.");
                int used=0;while(!j.cursor.done()&&used<32&&count<512&&System.nanoTime()<until){if(!ready(j,w))break;j.visit.accept(w.getBlockAt(j.cursor.x(),j.cursor.y(),j.cursor.z()));j.cursor.next();used++;count++;}
                long now=System.currentTimeMillis();if(now-j.lastProgress>=1000||j.cursor.done()){j.progress.accept(j.cursor.visited(),volume(j.area));j.lastProgress=now;}
                if(j.cursor.done()){j.done=true;release(j);j.success.run();idle=0;}else{jobs.addLast(j);idle=used==0?idle+1:0;if(idle>=jobs.size())break;}
            }catch(RuntimeException e){j.cancelled=true;release(j);j.failure.accept(Objects.requireNonNullElse(e.getMessage(),"Pemrosesan area gagal."));}}
    }
    @Override public void close(){closed=true;if(task!=null)task.cancel();for(Job j:jobs){j.cancelled=true;release(j);}jobs.clear();}
}
