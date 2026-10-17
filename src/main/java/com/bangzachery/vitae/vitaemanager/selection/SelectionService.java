package com.bangzachery.vitae.vitaemanager.selection;

import com.bangzachery.vitae.vitaemanager.whisper.WhisperArea;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

/** A single temporary, owner-bound tool shared by story setup commands. */
public final class SelectionService implements Listener, AutoCloseable {
    public enum Mode { BLOCKS, WALK, VOLUME, FACE }
    @FunctionalInterface public interface Saved { void save(Player player, Location a, Location b) throws Exception; }
    private static final class Session {
        final UUID token = UUID.randomUUID();
        final String permission, label; final Mode mode; final Saved saved;
        Location a, b;
        Session(String permission, String label, Mode mode, Saved saved) {
            this.permission=permission;this.label=label;this.mode=mode;this.saved=saved;
        }
    }
    private final JavaPlugin plugin;
    private final NamespacedKey key;
    private final Map<UUID, Session> sessions=new HashMap<>();
    private final Map<UUID, Long> suppress=new HashMap<>();
    private final AreaScanner scanner;
    public SelectionService(JavaPlugin plugin) {
        this.plugin=plugin;key=new NamespacedKey(plugin,"selection_wand");scanner=new AreaScanner(plugin);
    }
    public void start() {
        Bukkit.getServicesManager().register(SelectionService.class,this,plugin,ServicePriority.Normal);
        Bukkit.getPluginManager().registerEvents(this,plugin);scanner.start();
        for(Player p:Bukkit.getOnlinePlayers())clean(p);
    }
    public static SelectionService require() {
        SelectionService s=Bukkit.getServicesManager().load(SelectionService.class);
        if(s==null)throw new IllegalStateException("Alat seleksi belum aktif. Periksa log startup.");return s;
    }
    public AreaScanner scanner(){return scanner;}
    public static boolean selecting(Player p){SelectionService s=Bukkit.getServicesManager().load(SelectionService.class);return s!=null&&s.protecting(p);}
    public void arm(Player p,String permission,String label,Mode mode,Saved saved) {
        if(!p.hasPermission(permission))throw new IllegalArgumentException("Perlu izin "+permission+".");
        if(p.getOpenInventory().getTopInventory().getType()!=InventoryType.CRAFTING)
            throw new IllegalArgumentException("Tutup GUI dahulu sebelum mengambil alat seleksi.");
        int empty=p.getInventory().firstEmpty();
        if(empty<0&&!tagged(p.getInventory().getItemInMainHand()))
            throw new IllegalArgumentException("Kosongkan satu slot inventory dahulu; item milikmu tidak diganti atau dibuang.");
        cancel(p);Session s=new Session(permission,label,mode,saved);sessions.put(p.getUniqueId(),s);
        ItemStack wand=new ItemStack(Material.PRISMARINE_SHARD);var meta=wand.getItemMeta();
        meta.displayName(Component.text("Seleksi Vitae • "+label,NamedTextColor.AQUA));
        meta.lore(List.of(Component.text("Klik kiri: pos1 • Klik kanan: pos2",NamedTextColor.YELLOW),
            Component.text("Hilang setelah kedua titik dipilih",NamedTextColor.GRAY),
            Component.text("Batal: /vitae seleksi cancel",NamedTextColor.GRAY)));
        meta.getPersistentDataContainer().set(key,PersistentDataType.STRING,s.token.toString());wand.setItemMeta(meta);
        int hand=p.getInventory().getHeldItemSlot();ItemStack old=p.getInventory().getItem(hand);
        if(old!=null&&!old.getType().isAir())p.getInventory().setItem(p.getInventory().firstEmpty(),old);
        p.getInventory().setItem(hand,wand);
        say(p,"Seleksi "+label+": klik kiri sudut pertama, klik kanan sudut seberangnya. Blok tidak dihancurkan.");
        say(p,switch(mode){case WALK->"Klik lantai di kedua sudut; ruang di atas lantai otomatis dipilih untuk pemain lewat.";
            case VOLUME->"Klik blok sudut area; seleksi juga mencakup dua blok di atasnya.";
            case FACE->"Klik sisi bingkai yang menghadap ruang kosong portal, di dua sudut diagonal bagian dalam.";
            case BLOCKS->"Klik dua blok sudut diagonal; blok yang diklik ikut dalam seleksi.";});
    }
    private boolean tagged(ItemStack item){return item!=null&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(key,PersistentDataType.STRING);}
    private void clean(Player p){for(int i=0;i<p.getInventory().getSize();i++)if(tagged(p.getInventory().getItem(i)))p.getInventory().setItem(i,null);if(tagged(p.getItemOnCursor()))p.setItemOnCursor(null);}
    public void cancel(Player p){sessions.remove(p.getUniqueId());clean(p);}
    public void status(Player p){Session s=sessions.get(p.getUniqueId());say(p,s==null?"Tidak ada seleksi aktif.":s.label+" • pos1 "+(s.a==null?"belum":"dipilih")+" • pos2 "+(s.b==null?"belum":"dipilih"));}
    private boolean protecting(Player p){return sessions.containsKey(p.getUniqueId())||suppress.getOrDefault(p.getUniqueId(),0L)>System.currentTimeMillis();}
    @EventHandler(priority=EventPriority.LOWEST)
    public void click(PlayerInteractEvent e){
        Player p=e.getPlayer();if(!tagged(e.getItem()))return;
        e.setCancelled(true);e.setUseInteractedBlock(Event.Result.DENY);e.setUseItemInHand(Event.Result.DENY);
        Session s=sessions.get(p.getUniqueId());
        if(s==null||!Objects.equals(e.getItem().getItemMeta().getPersistentDataContainer().get(key,PersistentDataType.STRING),s.token.toString())){clean(p);return;}
        if(e.getHand()!=EquipmentSlot.HAND)return;
        if(!p.hasPermission(s.permission)){cancel(p);say(p,"Seleksi dibatalkan karena izin admin berubah.");return;}
        if(e.getClickedBlock()==null)return;
        if(e.getAction()!=Action.LEFT_CLICK_BLOCK&&e.getAction()!=Action.RIGHT_CLICK_BLOCK)return;
        Location at=e.getClickedBlock().getLocation();
        if(s.mode==Mode.FACE){BlockFace face=e.getBlockFace();if(face==BlockFace.SELF){say(p,"Klik sisi bingkai yang menghadap bagian dalam.");return;}at.add(face.getModX(),face.getModY(),face.getModZ());}
        boolean first=e.getAction()==Action.LEFT_CLICK_BLOCK;Location other=first?s.b:s.a;
        if(other!=null&&!other.getWorld().equals(at.getWorld())){say(p,"Kedua sudut harus satu world. Seleksi masih aktif.");return;}
        if(first)s.a=at;else s.b=at;
        suppress.put(p.getUniqueId(),System.currentTimeMillis()+500);
        say(p,(first?"Pos1":"Pos2")+" = "+at.getWorld().getName()+" "+at.getBlockX()+", "+at.getBlockY()+", "+at.getBlockZ());
        p.playSound(p.getLocation(),"minecraft:block.note_block.hat",.4f,first?1:1.3f);
        if(s.a==null||s.b==null)return;
        Location a=s.a.clone(),b=s.b.clone();
        if(s.mode==Mode.WALK){int lo=Math.min(a.getBlockY(),b.getBlockY()),hi=Math.max(a.getBlockY(),b.getBlockY())+2;a.setY(lo);b.setY(hi);}
        else if(s.mode==Mode.VOLUME){int lo=Math.min(a.getBlockY(),b.getBlockY()),hi=Math.max(a.getBlockY(),b.getBlockY())+2;a.setY(lo);b.setY(hi);}
        sessions.remove(p.getUniqueId());clean(p);
        // Avoid starting GUI/snapshot work inside the interaction event.
        Bukkit.getScheduler().runTask(plugin,()->{if(!p.isOnline()||!p.hasPermission(s.permission))return;
            try{s.saved.save(p,a,b);}catch(Exception error){say(p,"Seleksi belum disimpan: "+Objects.requireNonNullElse(error.getMessage(),"periksa log server"));}});
    }
    public static WhisperArea area(Location a,Location b){if(!a.getWorld().equals(b.getWorld()))throw new IllegalArgumentException("Kedua titik harus satu world.");return WhisperArea.between(a.getWorld().getUID(),a.getBlockX(),a.getBlockY(),a.getBlockZ(),b.getBlockX(),b.getBlockY(),b.getBlockZ());}
    public static void say(Player p,String text){p.sendMessage(Component.text("[Vitae Seleksi] ",NamedTextColor.GOLD).append(Component.text(text,NamedTextColor.AQUA)));}
    @EventHandler(priority=EventPriority.HIGHEST)public void preventUse(PlayerInteractEvent e){if(tagged(e.getItem())){e.setCancelled(true);e.setUseInteractedBlock(Event.Result.DENY);e.setUseItemInHand(Event.Result.DENY);}}
    @EventHandler(priority=EventPriority.HIGHEST)public void damage(BlockDamageEvent e){if(protecting(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST)public void breaking(BlockBreakEvent e){if(protecting(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST)public void place(BlockPlaceEvent e){if(tagged(e.getItemInHand()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST)public void drop(PlayerDropItemEvent e){if(tagged(e.getItemDrop().getItemStack()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST)public void swap(PlayerSwapHandItemsEvent e){if(tagged(e.getMainHandItem())||tagged(e.getOffHandItem()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST)public void inventory(InventoryClickEvent e){if(e.getWhoClicked() instanceof Player p&&(sessions.containsKey(p.getUniqueId())||tagged(e.getCurrentItem())||tagged(e.getCursor())))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST)public void drag(InventoryDragEvent e){if(e.getWhoClicked() instanceof Player p&&(sessions.containsKey(p.getUniqueId())||tagged(e.getOldCursor())))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST)public void interactEntity(PlayerInteractEntityEvent e){if(tagged(e.getPlayer().getInventory().getItemInMainHand()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.LOWEST)public void attack(EntityDamageByEntityEvent e){if(e.getDamager() instanceof Player p&&tagged(p.getInventory().getItemInMainHand()))e.setCancelled(true);}
    @EventHandler public void quit(PlayerQuitEvent e){cancel(e.getPlayer());scanner.cancelOwner(e.getPlayer().getUniqueId());suppress.remove(e.getPlayer().getUniqueId());}
    @EventHandler public void join(PlayerJoinEvent e){clean(e.getPlayer());}
    @EventHandler public void death(PlayerDeathEvent e){e.getDrops().removeIf(this::tagged);cancel(e.getEntity());}
    @Override public void close(){for(Player p:Bukkit.getOnlinePlayers())cancel(p);sessions.clear();suppress.clear();scanner.close();HandlerList.unregisterAll(this);Bukkit.getServicesManager().unregister(SelectionService.class,this);}
}
