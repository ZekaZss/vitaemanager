package com.bangzachery.vitae.vitaemanager.story;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

/** Fullscreen glyph uses its own font. No global Minecraft shader is replaced. */
public final class FadeService implements Listener, AutoCloseable {
    public static final class Settings { public int version=1;public boolean enabled;public int inTicks=15,holdTicks=5,outTicks=15; }
    private static final class Playback {
        final UUID token=UUID.randomUUID(); final Player player; final Location destination;final Consumer<Boolean> done;
        final long middle,end; boolean moved;
        Playback(Player player,Location destination,Consumer<Boolean> done,long middle,long end) {this.player=player;this.destination=destination;this.done=done;this.middle=middle;this.end=end;}
    }
    private final JavaPlugin plugin;
    private final StoryStore<Settings> store;
    private Settings settings;
    private final Map<UUID,Playback> active=new HashMap<>();
    private BukkitTask task;
    private long tick;
    private boolean closed;
    public FadeService(JavaPlugin plugin)throws IOException {
        this.plugin=plugin;store=new StoryStore<>(plugin.getDataFolder().toPath().resolve("fade.json"),Settings.class);settings=store.load(new Settings());
        if(settings.version!=1 || settings.inTicks<1 || settings.inTicks>100 || settings.holdTicks<1 || settings.holdTicks>100 || settings.outTicks<1 || settings.outTicks>100)throw new IOException("fade.json tidak valid; file dipertahankan.");
    }
    public void start(){Bukkit.getPluginManager().registerEvents(this,plugin);task=Bukkit.getScheduler().runTaskTimer(plugin,this::tick,1,1);}
    public boolean enabled(){return settings.enabled;}
    public boolean busy(Player p){return active.containsKey(p.getUniqueId());}
    public void teleport(Player p,Location destination,boolean requested,Consumer<Boolean> done){
        clear(p,true);if(closed || !p.isOnline() || p.isDead()){finish(done,false);return;}
        // A missing/declined pack must never delay gameplay or show the missing-glyph box.
        if(!requested || !settings.enabled || !p.hasResourcePack()){finish(done,p.teleport(destination));return;}
        play(p,destination,done);
    }
    private void play(Player p,Location destination,Consumer<Boolean> done){
        Playback b=new Playback(p,destination,done,tick+settings.inTicks,tick+settings.inTicks+settings.holdTicks+settings.outTicks);
        active.put(p.getUniqueId(),b);
        Component glyph=Component.text("\uE001",NamedTextColor.WHITE).font(Key.key("vitae_cinematic:screen"));
        p.showTitle(Title.title(glyph,Component.empty(),Title.Times.times(Duration.ofMillis(settings.inTicks*50L),Duration.ofMillis(settings.holdTicks*50L),Duration.ofMillis(settings.outTicks*50L))));
    }
    private void tick(){
        tick++;for(Playback b:List.copyOf(active.values())) {
            if(!b.player.isOnline() || b.player.isDead()){clear(b.player,false);continue;}
            if(!b.moved && tick>=b.middle){b.moved=true;try{boolean success=b.destination==null || b.player.teleport(b.destination);finish(b.done,success);if(!success){clear(b.player,false);continue;}}catch(RuntimeException error){plugin.getLogger().warning("Fade dihentikan: "+error.getMessage());finish(b.done,false);clear(b.player,false);continue;}}
            if(tick>=b.end+2)clear(b.player,false);
        }
    }
    private void clear(Player p,boolean completeTeleport){
        Playback b=active.remove(p.getUniqueId());if(b==null)return;clearTitle(p);
        if(!b.moved){b.moved=true;boolean success=false;try{success=completeTeleport && p.isOnline() && !p.isDead() && (b.destination==null || p.teleport(b.destination));}finally{finish(b.done,success);}}
    }
    private void finish(Consumer<Boolean> done,boolean success){try{done.accept(success);}catch(RuntimeException error){plugin.getLogger().warning("Callback fade: "+error.getMessage());}}
    private void clearTitle(Player p){try{p.clearTitle();}catch(RuntimeException error){plugin.getLogger().warning("Pembersihan title "+p.getName()+": "+error.getMessage());}}
    public void clearAll(){for(Player p:Bukkit.getOnlinePlayers()){try{clear(p,true);}catch(RuntimeException e){plugin.getLogger().warning("Pembersihan fade: "+e.getMessage());}clearTitle(p);}}
    public boolean command(CommandSender sender,String[] args){
        if(!sender.hasPermission("vitae.admin.fade")){sender.sendMessage(Component.text("Perlu izin vitae.admin.fade.",NamedTextColor.RED));return true;}
        try{
            if(args.length==1 && args[0].equalsIgnoreCase("clear")){clearAll();tell(sender,"Fade seluruh pemain dibersihkan. Teleport yang menunggu tetap diselesaikan.");}
            else if(args.length==1 && Set.of("on","off").contains(args[0].toLowerCase(Locale.ROOT))){Settings next=store.copy(settings);next.enabled=args[0].equalsIgnoreCase("on");store.save(next);settings=next;if(!settings.enabled)clearAll();tell(sender,"Fade "+(settings.enabled?"aktif":"mati")+".");}
            else if(args.length==4 && args[0].equalsIgnoreCase("times")){Settings next=store.copy(settings);next.inTicks=duration(args[1]);next.holdTicks=duration(args[2]);next.outTicks=duration(args[3]);store.save(next);settings=next;tell(sender,"Waktu fade tersimpan.");}
            else if(args.length>=1 && args[0].equalsIgnoreCase("preview")){Player p=args.length==2?Bukkit.getPlayerExact(args[1]):sender instanceof Player pl?pl:null;if(p==null)throw new IllegalArgumentException("/vitae fade preview [pemain-online]");if(!settings.enabled)throw new IllegalStateException("Aktifkan /vitae fade on terlebih dahulu.");if(!p.hasResourcePack())throw new IllegalStateException("Pemain belum memuat resource pack.");clear(p,false);play(p,null,ok->{});tell(sender,"Preview fade dimulai untuk "+p.getName()+".");}
            else if(args.length==1 && args[0].equalsIgnoreCase("pack")){exportPack();tell(sender,"Asset diekspor ke plugins/vitaemanager/fade-resourcepack/ dan dipasang ke contents/vitae_cinematic_fade/resourcepack/ jika ItemsAdder tersedia. Jalankan /iazip lalu muat ulang pack pemain.");}
            else {sender.sendMessage(Component.empty());tell(sender,"Fade: "+(settings.enabled?"ON":"OFF")+" • /vitae fade on|off|clear|pack|preview [pemain]");tell(sender,"/vitae fade times <gelap-detik> <tahan-detik> <terang-detik> (0.05–5 masing-masing)");sender.sendMessage(Component.empty());}
        }catch(Exception e){sender.sendMessage(Component.text("[Vitae Fade] "+Objects.requireNonNullElse(e.getMessage(),"Periksa log."),NamedTextColor.RED));}
        return true;
    }
    private int duration(String value){double s=Double.parseDouble(value);if(!Double.isFinite(s)||s<.05||s>5)throw new IllegalArgumentException("Durasi 0.05–5 detik.");return (int)Math.round(s*20);}
    public void exportPack()throws IOException{
        for(String name:List.of("assets/vitae_cinematic/font/screen.json","assets/vitae_cinematic/textures/font/black_screen.png")){
            Path to=plugin.getDataFolder().toPath().resolve("fade-resourcepack").resolve(name);Files.createDirectories(to.getParent());
            try(var input=plugin.getResource("fade-pack/"+name)){if(input==null)throw new IOException("Asset fade tidak ada dalam JAR.");Files.copy(input,to,StandardCopyOption.REPLACE_EXISTING);}
            org.bukkit.plugin.Plugin ia=Bukkit.getPluginManager().getPlugin("ItemsAdder");
            if(ia!=null){Path content=ia.getDataFolder().toPath().resolve("contents/vitae_cinematic_fade");Path pack=content.resolve("resourcepack").resolve(name);Files.createDirectories(pack.getParent());Files.copy(to,pack,StandardCopyOption.REPLACE_EXISTING);
                Path config=content.resolve("configs/fade.yml");Files.createDirectories(config.getParent());if(!Files.exists(config))Files.writeString(config,"info:\n  namespace: vitae_cinematic\n",StandardOpenOption.CREATE_NEW);}
        }
    }
    private void tell(CommandSender s,String text){s.sendMessage(Component.text("[Vitae Fade] ",NamedTextColor.GOLD).append(Component.text(text,NamedTextColor.AQUA)));}
    @EventHandler public void quit(PlayerQuitEvent e){clear(e.getPlayer(),false);}
    @EventHandler public void death(org.bukkit.event.entity.PlayerDeathEvent e){clear(e.getEntity(),false);}
    @Override public void close(){closed=true;if(task!=null)task.cancel();clearAll();HandlerList.unregisterAll(this);}
}
