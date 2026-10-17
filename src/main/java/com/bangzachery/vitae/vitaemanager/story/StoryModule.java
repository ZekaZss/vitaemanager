package com.bangzachery.vitae.vitaemanager.story;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

/** Isolates startup errors to new story modules; old economy/NPC data are untouched. */
public final class StoryModule implements AutoCloseable {
    private final JavaPlugin plugin;
    private FadeService fade;
    private DungeonService dungeon;
    private DungeonCommand command;
    private String failure="";
    public StoryModule(JavaPlugin plugin){this.plugin=plugin;}
    public void start(){
        try{fade=new FadeService(plugin);fade.start();dungeon=new DungeonService(plugin,fade);dungeon.start();command=new DungeonCommand(dungeon);failure="";}
        catch(Exception|LinkageError e){close();failure=Objects.requireNonNullElse(e.getMessage(),e.getClass().getSimpleName());plugin.getLogger().log(java.util.logging.Level.SEVERE,"Modul story belum aktif; modul dan data lama tetap tersedia.",e);}
        PluginCommand cmd=plugin.getCommand("vdungeon");if(cmd!=null){cmd.setExecutor((s,c,l,a)->execute(s,"dungeon",a));cmd.setTabCompleter((s,c,l,a)->command==null?List.of("reload"):command.onTabComplete(s,c,l,a));}
    }
    public boolean carryBlocked(Player p){return dungeon!=null&&dungeon.carryBlocked(p);}
    public boolean execute(CommandSender sender,String topic,String[] args){
        if(topic.equals("fade")&&args.length==1&&args[0].equalsIgnoreCase("clear")){
            if(!sender.hasPermission("vitae.admin.fade")){sender.sendMessage(Component.text("Perlu izin vitae.admin.fade.",NamedTextColor.RED));return true;}
            if(fade!=null)fade.clearAll();else for(Player p:Bukkit.getOnlinePlayers())p.clearTitle();DungeonService.tell(sender,"Layar fade seluruh pemain dibersihkan.");return true;
        }
        if(args.length==1&&args[0].equalsIgnoreCase("reload")){
            if(!sender.hasPermission("vitae.admin.dungeon")){sender.sendMessage(Component.text("Perlu izin vitae.admin.dungeon.",NamedTextColor.RED));return true;}close();start();DungeonService.tell(sender,command==null?"Belum aktif: "+failure:"Modul story dimuat. Sesi sebelumnya dihentikan dengan pemulihan.");return true;
        }
        if(command==null){sender.sendMessage(Component.text("Story belum aktif: "+failure+". Periksa log, lalu /vitae dungeon reload.",NamedTextColor.RED));return true;}
        return topic.equals("fade")?fade.command(sender,args):command.execute(sender,args);
    }
    public List<String> complete(CommandSender s,String topic,String[] args){if(topic.equals("fade"))return args.length==1?List.of("help","on","off","clear","times","preview","pack").stream().filter(v->v.startsWith(args[0].toLowerCase(Locale.ROOT))).toList():List.of();return command==null?List.of("reload"):command.onTabComplete(s,null,"vdungeon",args);}
    @Override public void close(){try{if(dungeon!=null)dungeon.close();}catch(RuntimeException|LinkageError error){plugin.getLogger().warning("Pembersihan dungeon: "+error.getMessage());}
        finally{try{if(fade!=null)fade.close();}catch(RuntimeException|LinkageError error){plugin.getLogger().warning("Pembersihan fade: "+error.getMessage());}dungeon=null;fade=null;command=null;}}
}
