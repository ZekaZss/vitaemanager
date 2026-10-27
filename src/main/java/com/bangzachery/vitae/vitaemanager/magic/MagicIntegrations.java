package com.bangzachery.vitae.vitaemanager.magic;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.lang.reflect.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

/** LuckPerms membership and passive legacy GSit recovery inspection. Tunduk uses native Paper pose. */
final class MagicIntegrations {
 private Class<?> lpApi,lpUser,query,group; private Object lp;
 private Method crawlGet;
 String lpProblem="LuckPerms tidak tersedia",crawlProblem="GSit tidak tersedia";
 MagicIntegrations(JavaPlugin plugin){
  Plugin l=Bukkit.getPluginManager().getPlugin("LuckPerms");
  if(l!=null&&l.isEnabled())try{
   ClassLoader cl=l.getClass().getClassLoader();lpApi=cl.loadClass("net.luckperms.api.LuckPerms");lp=cl.loadClass("net.luckperms.api.LuckPermsProvider").getMethod("get").invoke(null);
   lpUser=cl.loadClass("net.luckperms.api.model.user.User");query=cl.loadClass("net.luckperms.api.query.QueryOptions");group=cl.loadClass("net.luckperms.api.model.group.Group");lpProblem=null;
  }catch(ReflectiveOperationException e){lpProblem="API LuckPerms tidak kompatibel: "+l.getDescription().getVersion();}
  Plugin g=Bukkit.getPluginManager().getPlugin("GSit");
  if(g!=null&&g.isEnabled())try{
   ClassLoader cl=g.getClass().getClassLoader();Class<?> api=cl.loadClass("dev.geco.gsit.api.GSitAPI");
   crawlGet=api.getMethod("getCrawlByPlayer",Player.class);
   crawlProblem=null;
  }catch(ReflectiveOperationException|IllegalArgumentException e){crawlProblem="API GSit tidak kompatibel: "+g.getDescription().getVersion();}
 }
 boolean antihero(Player player){
  if(lpProblem!=null)throw new IllegalStateException(lpProblem);
  try {
   Object manager=lpApi.getMethod("getUserManager").invoke(lp);Class<?> managerApi=lpApi.getClassLoader().loadClass("net.luckperms.api.model.user.UserManager");
   Object user=managerApi.getMethod("getUser",UUID.class).invoke(manager,player.getUniqueId());if(user==null)throw new IllegalStateException("Data grup LuckPerms belum siap");
   Object options=lpUser.getMethod("getQueryOptions").invoke(user);
   Collection<?> groups=(Collection<?>)lpUser.getMethod("getInheritedGroups",query).invoke(user,options);
   for(Object value:groups)if("antihero".equalsIgnoreCase((String)group.getMethod("getName").invoke(value)))return true;return false;
  }catch(ReflectiveOperationException e){throw new IllegalStateException("Pemeriksaan grup LuckPerms gagal",e);}
 }
 Object crawl(Player p){try{return crawlGet.invoke(null,p);}catch(ReflectiveOperationException e){throw new IllegalStateException("GSit gagal",e);}}
}
