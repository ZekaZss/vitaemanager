package com.bangzachery.vitae.vitaemanager.story;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;

/** Optional MythicMobs 5.x adapter: its classes are loaded only from MythicMobs. */
public final class DungeonMobs implements AutoCloseable {
    private final JavaPlugin plugin;
    private final Listener spawnListener=new Listener(){};
    public final NamespacedKey sessionKey;
    public DungeonMobs(JavaPlugin plugin){this.plugin=plugin;sessionKey=new NamespacedKey(plugin,"dungeon_session");}
    @SuppressWarnings("unchecked")
    public void guardNaturalSpawns(Predicate<World> protectedWorld){
        Plugin mythic=Bukkit.getPluginManager().getPlugin("MythicMobs");if(mythic==null||!mythic.isEnabled())return;
        try{
            Class<?> event=Class.forName("io.lumine.mythic.bukkit.events.MythicMobSpawnEvent",true,mythic.getClass().getClassLoader());
            Method location=event.getMethod("getLocation"),fromSpawner=event.getMethod("isFromMythicSpawner"),reason=event.getMethod("getSpawnReason");
            Bukkit.getPluginManager().registerEvent((Class<? extends Event>)event,spawnListener,EventPriority.HIGHEST,(listener,e)->{
                try{
                    Location at=(Location)location.invoke(e);if(at==null||!protectedWorld.test(at.getWorld()))return;
                    Object why=reason.invoke(e);boolean natural=Boolean.TRUE.equals(fromSpawner.invoke(e));
                    if(why!=null){Object bukkitReason=why.getClass().getMethod("getBukkitReason").invoke(why);natural|=bukkitReason==CreatureSpawnEvent.SpawnReason.NATURAL||bukkitReason==CreatureSpawnEvent.SpawnReason.SPAWNER;}
                    if(natural)((Cancellable)e).setCancelled(true);
                }catch(ReflectiveOperationException error){throw new EventException(error);}
            },plugin,true);
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Pelindung spawn MythicMobs tidak cocok.",error);}
    }
    private Object manager()throws ReflectiveOperationException{
        Plugin mythic=Bukkit.getPluginManager().getPlugin("MythicMobs");if(mythic==null || !mythic.isEnabled())throw new IllegalStateException("MythicMobs belum aktif.");
        Class<?> api=Class.forName("io.lumine.mythic.bukkit.MythicBukkit",true,mythic.getClass().getClassLoader());
        Object core=api.getMethod("inst").invoke(null);return api.getMethod("getMobManager").invoke(core);
    }
    public void validate(DungeonData.Mob m){
        try{
            String name=m.type().substring(m.type().indexOf(':')+1);
            if(m.type().startsWith("vanilla:")){EntityType type=EntityType.valueOf(name.toUpperCase(Locale.ROOT));if(!type.isAlive()||!type.isSpawnable()||type==EntityType.PLAYER)throw new IllegalArgumentException("Mob vanilla tidak dapat dipanggil.");}
            else {Object manager=manager();Object found=manager.getClass().getMethod("getMythicMob",String.class).invoke(manager,name);if(!(found instanceof Optional<?> opt)||opt.isEmpty())throw new IllegalArgumentException("ID MythicMob tidak ditemukan: "+name);}
        }catch(ReflectiveOperationException e){throw new IllegalStateException("API MythicMobs belum cocok: "+root(e),e);}
    }
    public LivingEntity spawn(DungeonData.Mob m,UUID session){
        Location at=DungeonData.location(m.point());at.getChunk().load();Entity entity;
        try{
            String name=m.type().substring(m.type().indexOf(':')+1);
            if(m.type().startsWith("vanilla:"))entity=at.getWorld().spawnEntity(at,EntityType.valueOf(name.toUpperCase(Locale.ROOT)),CreatureSpawnEvent.SpawnReason.CUSTOM);
            else {Object manager=manager();Object active=manager.getClass().getMethod("spawnMob",String.class,Location.class,double.class).invoke(manager,name,at,m.level());
                if(active==null)throw new IllegalStateException("MythicMobs menolak spawn "+name);Object wrapped=active.getClass().getMethod("getEntity").invoke(active);entity=(Entity)wrapped.getClass().getMethod("getBukkitEntity").invoke(wrapped);}
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Pemanggilan MythicMob gagal: "+root(e),e);}
        if(!(entity instanceof LivingEntity living)||!entity.isValid()||living.isDead()){if(entity!=null)entity.remove();throw new IllegalStateException("Spawn dibatalkan atau mob bukan LivingEntity.");}
        living.setPersistent(true);living.setRemoveWhenFarAway(false);if(session!=null)living.getPersistentDataContainer().set(sessionKey,PersistentDataType.STRING,session.toString());return living;
    }
    public void remove(UUID id){Entity entity=Bukkit.getEntity(id);if(entity==null)return;
        Plugin mythic=Bukkit.getPluginManager().getPlugin("MythicMobs");
        if(mythic!=null && mythic.isEnabled())try{Object manager=manager();Object found=manager.getClass().getMethod("getActiveMob",UUID.class).invoke(manager,id);
            if(found instanceof Optional<?> opt && opt.isPresent()){Object active=opt.get();try{active.getClass().getMethod("remove").invoke(active);}catch(NoSuchMethodException ignored){entity.remove();}}}
        catch(ReflectiveOperationException e){plugin.getLogger().warning("Cleanup MythicMob: "+root(e));}
        if(entity.isValid())entity.remove();
    }
    private String root(Throwable e){while(e.getCause()!=null)e=e.getCause();return Objects.requireNonNullElse(e.getMessage(),e.getClass().getSimpleName());}
    @Override public void close(){HandlerList.unregisterAll(spawnListener);}
}
