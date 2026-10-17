package com.bangzachery.vitae.vitaemanager.story;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import java.util.*;
import java.util.function.BiPredicate;

/** Cameras move only their owner, with recoverable mode/location snapshots. */
public final class DungeonCamera implements Listener {
    public static final class CameraFailure extends IllegalStateException {
        public final UUID player;
        CameraFailure(Player player,RuntimeException cause){super("Kamera gagal untuk "+player.getName(),cause);this.player=player.getUniqueId();}
    }
    private record Shot(Player player,Location from,Location view,Location back,DungeonData.Participant participant,long until,int length,Runnable done){}
    private final JavaPlugin plugin;
    private final BiPredicate<Player,Location> teleport;
    private final Map<UUID,Shot> shots=new HashMap<>();
    private long tick;
    public DungeonCamera(JavaPlugin plugin,BiPredicate<Player,Location> teleport){this.plugin=plugin;this.teleport=teleport;Bukkit.getPluginManager().registerEvents(this,plugin);}
    public boolean busy(UUID id){return shots.containsKey(id);}
    public void start(Player p,Location view,DungeonData.Participant participant,int length,Runnable done){
        stop(p.getUniqueId(),false);Location back=p.getLocation().clone();
        participant.recovery=DungeonData.point(back);participant.mode=p.getGameMode().name();participant.flight=p.getAllowFlight();participant.flying=p.isFlying();
        if(!p.getWorld().equals(view.getWorld()))throw new IllegalArgumentException("Kamera harus satu world.");
        Shot shot=new Shot(p,back.clone(),view.clone(),back,participant,tick+length,length,done);shots.put(p.getUniqueId(),shot);
        p.setGameMode(GameMode.SPECTATOR);p.setSpectatorTarget(null);p.setVelocity(new Vector());
        if(p.getGameMode()!=GameMode.SPECTATOR){shots.remove(p.getUniqueId());throw new IllegalStateException("Plugin lain menolak mode kamera.");}
    }
    public void tick(){
        tick++;for(Shot s:List.copyOf(shots.values())){
            try{
            if(!s.player.isOnline()||s.player.isDead()){stop(s.player.getUniqueId(),false);continue;}
            int age=s.length-(int)(s.until-tick);double t=Math.min(1,age/20.0),ease=t*t*(3-2*t);
            Location at=s.from.clone();at.setX(at.getX()+(s.view.getX()-at.getX())*ease);at.setY(at.getY()+(s.view.getY()-at.getY())*ease);at.setZ(at.getZ()+(s.view.getZ()-at.getZ())*ease);
            float delta=(s.view.getYaw()-s.from.getYaw())%360;delta=(delta+540)%360-180;at.setYaw(s.from.getYaw()+delta*(float)ease);at.setPitch(s.from.getPitch()+(s.view.getPitch()-s.from.getPitch())*(float)ease);
            if(!teleport.test(s.player,at)){stop(s.player.getUniqueId(),false);throw new IllegalStateException("Teleport kamera ditolak untuk "+s.player.getName());}
            if(tick>=s.until)stop(s.player.getUniqueId(),true);
            }catch(RuntimeException error){try{stop(s.player.getUniqueId(),false);}catch(RuntimeException recovery){error.addSuppressed(recovery);}throw new CameraFailure(s.player,error);}
        }
    }
    public void stop(UUID id,boolean complete){
        Shot s=shots.remove(id);if(s==null)return;
        if(s.player.isOnline()&&!s.player.isDead()){
            if(s.player.getGameMode()==GameMode.SPECTATOR)s.player.setSpectatorTarget(null);
            s.player.setGameMode(GameMode.valueOf(s.participant.mode));s.player.setAllowFlight(s.participant.flight);s.player.setFlying(s.participant.flight&&s.participant.flying);
            if(s.player.getGameMode()!=GameMode.valueOf(s.participant.mode))throw new IllegalStateException("Plugin lain menolak pemulihan mode kamera.");
            s.player.setVelocity(new Vector());s.player.setFallDistance(0);
            if(!teleport.test(s.player,s.back))throw new IllegalStateException("Lokasi kembali kamera ditolak untuk "+s.player.getName());
            s.participant.recovery=null;s.participant.mode=null;
            if(complete)s.done.run();
        }
    }
    public void restore(Player p,DungeonData.Participant participant){
        if(participant.mode==null)return;if(p.getGameMode()==GameMode.SPECTATOR)p.setSpectatorTarget(null);
        p.setGameMode(GameMode.valueOf(participant.mode));p.setAllowFlight(participant.flight);p.setFlying(participant.flight&&participant.flying);
        if(p.getGameMode()!=GameMode.valueOf(participant.mode))throw new IllegalStateException("Mode pemain belum berhasil dipulihkan.");
        p.setFallDistance(0);if(participant.recovery!=null&&participant.status!=DungeonData.Status.DEAD&&!teleport.test(p,DungeonData.location(participant.recovery)))throw new IllegalStateException("Pemulihan posisi kamera ditolak.");participant.mode=null;participant.recovery=null;
    }
    @EventHandler(priority=EventPriority.HIGHEST)public void move(PlayerMoveEvent e){if(busy(e.getPlayer().getUniqueId())&&!(e instanceof PlayerTeleportEvent))e.setTo(e.getFrom());}
    @EventHandler(priority=EventPriority.HIGHEST)public void damage(EntityDamageEvent e){if(e.getEntity() instanceof Player p&&busy(p.getUniqueId()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST)public void command(PlayerCommandPreprocessEvent e){if(busy(e.getPlayer().getUniqueId())&&!e.getPlayer().hasPermission("vitae.dungeon.bypass")){e.setCancelled(true);e.getPlayer().sendMessage("Tunggu pengenalan kamera selesai.");}}
    @EventHandler(priority=EventPriority.HIGHEST)public void interact(PlayerInteractEvent e){if(busy(e.getPlayer().getUniqueId()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST)public void interact(PlayerInteractEntityEvent e){if(busy(e.getPlayer().getUniqueId()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST)public void sneak(PlayerToggleSneakEvent e){if(busy(e.getPlayer().getUniqueId()))e.setCancelled(true);}
    public void close(){for(UUID id:List.copyOf(shots.keySet()))try{stop(id,false);}catch(RuntimeException error){plugin.getLogger().warning(error.getMessage());}HandlerList.unregisterAll(this);}
}
