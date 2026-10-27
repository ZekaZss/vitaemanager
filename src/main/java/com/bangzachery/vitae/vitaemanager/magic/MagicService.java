package com.bangzachery.vitae.vitaemanager.magic;

import com.bangzachery.vitae.vitaemanager.magic.domain.DomainManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.*;
import org.bukkit.potion.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import java.io.IOException;
import java.time.Duration;
import java.util.*;

/** Main-thread spell orchestration. Durable ownership and use markers precede player/world changes. */
public final class MagicService implements Listener,CommandExecutor,TabCompleter,AutoCloseable {
 private final JavaPlugin plugin; private final MagicState state; private final VictimJournal victimJournal; private final InternalCrawlPose poses; private final DomainManager domains;
 private final Map<String,SpellMechanic> mechanics=new LinkedHashMap<>();
 private final Map<SpellSoundDelivery.Key,Set<SpellSoundDelivery>> soundLoops=new HashMap<>();
 private MagicIntegrations integrations; private Map<String,SpellConfig> spells; private BukkitTask task;
 private java.util.function.Predicate<Player> carryRelease=p->true;
 public void carryRelease(java.util.function.Predicate<Player> release){this.carryRelease=Objects.requireNonNull(release);}
 private final Map<UUID,Use> uses=new LinkedHashMap<>(); private final Map<UUID,Lock> locks=new HashMap<>();
 private final Set<UUID> teleporting=new HashSet<>(),alteringPotion=new HashSet<>();
 private final Map<UUID,Map<PotionEffectType,Lease>> leases=new HashMap<>(); private long ticks; private boolean closed,startupRecovering; private final Set<UUID> recoveryPending=new HashSet<>();
 private static final String[] TITLES={"ᴛᴜɴᴅᴜᴋʟᴀʜ","ʟᴀɴɢɪᴛ ᴍᴜʟᴀɪ ʀᴜɴᴛᴜʜ","ʜᴀʀᴀᴘᴀɴ ᴛᴇʟᴀʜ ᴍᴀᴛɪ","ɪɴɪʟᴀʜ ᴡɪʟᴀʏᴀʜᴋᴜ","ᴅᴏᴍᴀɪɴ ᴇxᴘᴀɴꜱɪᴏɴ","ᴜɴʟɪᴍɪᴛᴇᴅ ᴍɪꜱᴇʀʏ"};
 public MagicService(JavaPlugin plugin)throws IOException {this.plugin=plugin;for(String id:List.of("unlimited_misery","telekinesis","tunduk"))registerMechanic(new SpellMechanic(){public String id(){return id;}public void tick(MagicService service,Use use)throws Exception{service.update(use);}});victimJournal=new VictimJournal(plugin.getDataFolder().toPath().resolve("magic/recovery/players.properties"));poses=new InternalCrawlPose(victimJournal);state=new MagicState(plugin.getDataFolder().toPath().resolve("magic/data/state.properties"));domains=new DomainManager(plugin,plugin.getDataFolder().toPath().resolve("magic/recovery/domains"));}
 void registerMechanic(SpellMechanic mechanic){if(task!=null)throw new IllegalStateException("Daftar mekanik sudah aktif");if(mechanics.putIfAbsent(mechanic.id(),mechanic)!=null)throw new IllegalArgumentException("ID sihir duplikat");}
 public void start()throws IOException {
  spells=SpellConfig.load(plugin,mechanics.keySet());integrations=new MagicIntegrations(plugin);domains.setEvacuationTeleport((p,safe)->{teleporting.add(p.getUniqueId());try{boolean moved=p.teleport(safe,PlayerTeleportEvent.TeleportCause.PLUGIN);if(moved){Lock l=locks.get(p.getUniqueId());if(l!=null){l.anchor.setX(safe.getX());l.anchor.setY(safe.getY());l.anchor.setZ(safe.getZ());l.position=safe.clone();}}return moved;}finally{teleporting.remove(p.getUniqueId());}});domains.start();startupRecovering=domains.recovering();recoveryPending.addAll(domains.recoveryOwners()); for(UUID id:state.activeOwners())if(!recoveryPending.contains(id))state.finish(id,System.currentTimeMillis());
  Bukkit.getPluginManager().registerEvents(this,plugin);PluginCommand command=Objects.requireNonNull(plugin.getCommand("sihir"));command.setExecutor(this);command.setTabCompleter(this);
  for(Player p:Bukkit.getOnlinePlayers())recoverVictim(p);task=Bukkit.getScheduler().runTaskTimer(plugin,this::tick,1,1);
 }
 public boolean isAbilityBlocked(UUID id){Lock l=locks.get(id);return l!=null&&l.items;}
 private void tell(CommandSender p,String text){p.sendMessage(Component.text(text));}
 @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){try{
  if(args.length==0){if(sender instanceof Player p)collection(p);else tell(sender,"Gunakan command admin /sihir.");return true;}
  String action=args[0].toLowerCase(Locale.ROOT);
  if(action.equals("pakai")||action.equals("info")){
   if(!(sender instanceof Player p)||args.length!=2){tell(sender,"/sihir "+action+" <id>");return true;}
   SpellConfig spell=spells.get(args[1]);if(spell==null||!state.owned(p.getUniqueId()).contains(args[1])){tell(p,"Kamu tidak memiliki sihir itu.");return true;}
   if(action.equals("pakai"))activate(p,spell);else tell(p,spell.name()+": "+spell.description()+" ("+displayDuration(spell)+" detik). "+status(p.getUniqueId()));return true;
  }
  if(!sender.hasPermission("vitae.sihir.admin")){tell(sender,"Kamu tidak memiliki izin admin sihir.");return true;}
  if(action.equals("reload")){spells=SpellConfig.load(plugin,mechanics.keySet());if(uses.isEmpty()){integrations=new MagicIntegrations(plugin);}tell(sender,"Konfigurasi dimuat. Penggunaan aktif memakai konfigurasi awalnya.");return true;}
  boolean reset=action.equals("cooldown")&&args.length==3&&args[1].equals("reset");
  if(args.length<2){help(sender);return true;}OfflinePlayer target=known(reset?args[2]:args[1]);if(target==null){tell(sender,"Identitas pemain belum dikenal server.");return true;}UUID id=target.getUniqueId();
  switch(action){
   case "beri","hapus" -> {if(args.length!=3||!spells.containsKey(args[2])){help(sender);break;}boolean changed=action.equals("beri")?state.grant(id,args[2]):state.revoke(id,args[2]);tell(sender,changed?"Kepemilikan diperbarui.":"Tidak berubah: duplikat, batas sembilan, atau sihir tidak dimiliki.");}
   case "cek" -> tell(sender,"Koleksi: "+String.join(", ",state.owned(id))+"; "+status(id));
   case "hentikan" -> {Use use=uses.get(id);if(use!=null)stop(use);tell(sender,"Penghentian dan pemulihan diminta.");}
   case "cooldown" -> {if(reset){state.reset(id);tell(sender,"Cooldown direset; penggunaan aktif tetap terkunci.");}else help(sender);}
   default -> help(sender);
  }
 }catch(Exception ex){plugin.getLogger().log(java.util.logging.Level.WARNING,"Sihir gagal",ex);tell(sender,"Sihir ditolak/gagal: "+ex.getMessage());}return true;}
 private void help(CommandSender p){tell(p,"/sihir [info|pakai <id>] | beri/hapus <player> <id> | cek/hentikan <player> | cooldown reset <player> | reload");}
 private OfflinePlayer known(String name){for(OfflinePlayer p:Bukkit.getOfflinePlayers())if(p.getName()!=null&&p.getName().equalsIgnoreCase(name)&&(p.hasPlayedBefore()||p.isOnline()))return p;return null;}
 private String status(UUID id){return uses.containsKey(id)?(uses.get(id).started<0?"Memvalidasi lokasi domain":"Sedang menggunakan "+uses.get(id).spell.name()):state.active(id)?"Menunggu pemulihan "+state.using(id):state.cooldown(id)>System.currentTimeMillis()?"Cooldown "+((state.cooldown(id)-System.currentTimeMillis()+999)/1000)+" detik":"Siap";}
 private int displayDuration(SpellConfig spell){return spell.duration()+(spell.id().equals("unlimited_misery")?40:0);}
 private ItemStack icon(SpellConfig s,UUID id){ItemStack item=new ItemStack(s.icon(),1);var meta=item.getItemMeta();meta.displayName(Component.text(s.name()));meta.lore(List.of(Component.text(s.description()),Component.text("Durasi: "+displayDuration(s)+" detik"),Component.text(status(id))));item.setItemMeta(meta);return item;}
 private void collection(Player p){Menu menu=new Menu(null);Inventory inv=Bukkit.createInventory(menu,9,Component.text("Koleksi sihir"));menu.inventory=inv;int slot=0;for(String id:state.owned(p.getUniqueId()))if(spells.containsKey(id)){menu.ids.put(slot,id);inv.setItem(slot++,icon(spells.get(id),p.getUniqueId()));}p.openInventory(inv);}
 private void confirm(Player p,String id){if(!spells.containsKey(id))return;Menu m=new Menu(id);m.inventory=Bukkit.createInventory(m,9,Component.text("Konfirmasi sihir"));m.inventory.setItem(2,button(Material.RED_WOOL,"BATAL"));m.inventory.setItem(4,icon(spells.get(id),p.getUniqueId()));m.inventory.setItem(6,button(Material.LIME_WOOL,"GUNAKAN"));p.openInventory(m.inventory);}
 private ItemStack button(Material type,String text){ItemStack i=new ItemStack(type);var meta=i.getItemMeta();meta.displayName(Component.text(text));i.setItemMeta(meta);return i;}
 @EventHandler(priority=EventPriority.HIGHEST) public void click(InventoryClickEvent e){if(isAbilityBlocked(e.getWhoClicked().getUniqueId())){e.setCancelled(true);return;}if(!(e.getView().getTopInventory().getHolder() instanceof Menu m))return;e.setCancelled(true);if(!(e.getWhoClicked() instanceof Player p)||e.getRawSlot()<0||e.getRawSlot()>8)return;
  Bukkit.getScheduler().runTask(plugin,()->{if(p.getOpenInventory().getTopInventory()!=m.inventory)return;if(m.selected==null){String id=m.ids.get(e.getRawSlot());if(id!=null)confirm(p,id);}else if(e.getRawSlot()==2)collection(p);else if(e.getRawSlot()==6){try{activate(p,spells.get(m.selected));}catch(Exception ex){tell(p,"Aktivasi ditolak: "+ex.getMessage());}}});}
 @EventHandler public void drag(InventoryDragEvent e){if(isAbilityBlocked(e.getWhoClicked().getUniqueId())||e.getView().getTopInventory().getHolder() instanceof Menu)e.setCancelled(true);}
 private void activate(Player p,SpellConfig s)throws IOException {
  UUID id=p.getUniqueId();if(s==null||!state.owned(id).contains(s.id()))throw new IllegalStateException("Sihir tidak dimiliki");if(closed||startupRecovering||uses.containsKey(id)||recoveryPending.contains(id)||p.isDead()||isAbilityBlocked(id)||locks.containsKey(id)||state.active(id)||state.cooldown(id)>System.currentTimeMillis())throw new IllegalStateException("Sedang terkunci atau cooldown");
  SpellSoundPlan soundPlan=s.soundPlan();
  SpellMechanic mechanic=Objects.requireNonNull(mechanics.get(s.id()),"Mekanik sihir belum terdaftar");mechanic.validate(p,s);DomainManager.Handle domain=null;
  if(s.id().equals("unlimited_misery")){if(integrations.lpProblem!=null)throw new IllegalStateException(integrations.lpProblem);for(Player target:near(p.getLocation(),s.radius()))integrations.antihero(target);domain=domains.prepare(p,s.radius());}
  List<Player> victims=near(p.getLocation(),s.radius()).stream().filter(v->v!=p).toList();
  if(s.id().equals("tunduk")){for(Player v:victims){if(locks.containsKey(v.getUniqueId()))throw new IllegalStateException("Target sudah memiliki penguncian lain");poses.validate(v);if(integrations.crawlProblem==null&&integrations.crawl(v)!=null)throw new IllegalStateException("Target memiliki crawl GSit lain");}}
  if(s.id().equals("telekinesis"))for(Player v:victims)if(locks.containsKey(v.getUniqueId())||v.isInsideVehicle()||!v.getPassengers().isEmpty())throw new IllegalStateException("Target sedang terikat mekanik lain");
  try{if(domain==null)state.begin(id,s.id());}catch(IOException ex){if(domain!=null)domain.emergencyRestore();throw ex;}
  Use use=new Use(id,s,p.getLocation(),domain);use.delivery=new SpellSoundDelivery(soundLoops);use.audio=new SpellAudio(soundPlan,cue->emitSound(use,cue),use.delivery::stopRepeats);uses.put(id,use);p.closeInventory();
  try{if(domain==null){if(s.id().equals("telekinesis")||s.id().equals("tunduk"))for(Player v:victims){Lock lock=lock(use,v,s.id().equals("tunduk"));if(lock.items){lock.pose=true;poses.acquire(v,use.owner);}}use.started=ticks;mechanic.begin(this,use,p);use.audio.start(s.id().equals("telekinesis")?"formation":"active",ticks);circle(use);}tell(p,s.config().getString("messages.started","Sihir dimulai: {name}").replace("{name}",s.name()));}catch(RuntimeException e){use.cancelledBeforeStart=true;stop(use);throw e;}
 }
 private List<Player> near(Location center,int radius){return center.getWorld().getPlayers().stream().filter(p->!p.isDead()&&p.getLocation().distanceSquared(center)<=radius*radius).toList();}
 private Lock lock(Use use,Player p,boolean items){if(use.domain!=null){if(!carryRelease.test(p))throw new IllegalStateException("Carry target tidak dapat dilepas dengan aman");if(p.isInsideVehicle()&&!p.leaveVehicle())throw new IllegalStateException("Target tidak dapat turun dari kendaraan");if(!p.getPassengers().isEmpty())throw new IllegalStateException("Target mempunyai penumpang dari mekanik lain");}if(items)p.closeInventory();Lock l=new Lock(use,p.getLocation(),items);locks.put(p.getUniqueId(),l);use.victims.add(p.getUniqueId());return l;}
 private void tick(){ticks++;if(startupRecovering&&!domains.recovering())startupRecovering=false;if(ticks%20==0)checkpointVictims();if(!domains.recovering()&&!recoveryPending.isEmpty()){for(UUID id:List.copyOf(recoveryPending))try{state.finish(id,System.currentTimeMillis());recoveryPending.remove(id);}catch(IOException e){plugin.getLogger().severe("Cooldown pemulihan belum tersimpan: "+e.getMessage());}}for(Use use:List.copyOf(uses.values()))try{mechanics.get(use.spell.id()).tick(this,use);if(use.audio!=null)use.audio.tick(ticks);}catch(Exception ex){plugin.getLogger().log(java.util.logging.Level.SEVERE,"Penggunaan sihir dihentikan",ex);stop(use);}}
 private void update(Use u)throws IOException {
  if(u.stopping){if(u.domain!=null&&!u.domain.fullyRestored()){u.domain.emergencyRestore();return;}finish(u);return;}
  Player owner=Bukkit.getPlayer(u.owner);if(owner==null||owner.isDead()){stop(u);return;}
  if(u.domain!=null){if(u.domain.failedReason()!=null){tell(owner,u.domain.failedReason());if(u.started<0)u.cancelledBeforeStart=true;stop(u);return;}if(!u.domain.ready())return;if(u.started<0){if(!state.owned(u.owner).contains(u.spell.id())){u.cancelledBeforeStart=true;stop(u);return;}state.begin(u.owner,u.spell.id());u.started=ticks;u.audio.start("formation",ticks);}
   long age=ticks-u.started;int full=u.spell.duration()*20;
   if(age<=400){u.domain.buildFraction(age/400d);if(age%80==0){int index=(int)(age/80);if(index<TITLES.length&&index>u.lastTitle){title(u,TITLES[index]);u.lastTitle=index;}}if(age%10==0)circle(u);
    for(Player p:near(u.center,u.spell.radius())){boolean exempt=p.getUniqueId().equals(u.owner)||integrations.antihero(p);Lock current=locks.get(p.getUniqueId());if(exempt&&current!=null&&current.use==u)release(u,p.getUniqueId());else if(!exempt&&current==null)lock(u,p,true);}
    if(age==400&&!u.domain.fullyBuilt()){u.started++;return;}if(age==400){releaseLocks(u);u.audio.phase("active",ticks);}
   }else if(age<=400+full){if(age%20==0)domainEffects(u);if(u.domain.contains(owner.getLocation())&&(age-400)%Math.max(1,u.spell.config().getInt("healing.interval-ticks",20))==0)owner.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH,1,Math.max(0,u.spell.config().getInt("healing.level",10)-1)));}
   else {if(!u.effectsEnded){u.effectsEnded=true;clearLeases(u);releaseAudio(u);}u.domain.restoreFraction(Math.min(1,(age-400-full)/400d));if(age>=800+full&&u.domain.fullyRestored()){finish(u);return;}}
  }else {long age=ticks-u.started;if(age>=u.spell.duration()*20L){stop(u);return;}if(u.spell.id().equals("telekinesis")&&age>=Math.ceil(u.spell.config().getDouble("maximum-rise",7)/u.spell.config().getDouble("rise-per-tick",.05)))u.audio.phase("active",ticks);for(UUID victim:List.copyOf(u.victims)){Player p=Bukkit.getPlayer(victim);Lock l=locks.get(victim);if(p==null||p.isDead()||l==null){release(u,victim);continue;}
    if(u.spell.id().equals("telekinesis")){double limit=Math.min(7,u.spell.config().getDouble("maximum-rise",7));Location next=l.anchor.clone().add(0,Math.min(limit,age*u.spell.config().getDouble("rise-per-tick",0.05)),0);
     if(passable(p,next))l.position=next;teleport(p,l.position);p.setVelocity(new Vector());if(age%Math.max(1,u.spell.config().getInt("camera.interval-ticks",10))==0)p.playHurtAnimation((float)((age*19)%360));particles(p,u.spell);}
    else if(l.pose)poses.check(p);
    if(u.spell.id().equals("tunduk")&&age%5==0)particles(p,u.spell);
   }}
 }
 private void emitSound(Use u,SpellSoundPlan.Cue cue){
  List<Player> victims=new ArrayList<>();
  if(u.domain!=null){for(Player p:near(u.center,u.spell.radius()))if(!p.getUniqueId().equals(u.owner))victims.add(p);}
  else for(UUID id:u.victims){Player p=Bukkit.getPlayer(id);if(p!=null&&p.isOnline()&&!p.isDead())victims.add(p);}
  try{u.delivery.emit(cue,Bukkit.getPlayer(u.owner),u.center,victims);}
  catch(RuntimeException error){try{if(u.audio!=null)u.audio.finish(false);}catch(RuntimeException cleanup){error.addSuppressed(cleanup);}plugin.getLogger().log(java.util.logging.Level.WARNING,"Sound sihir dihentikan untuk penggunaan "+u.owner+" / "+u.spell.id(),error);}
 }
 private void releaseAudio(Use u){
  if(u.releasingAudio)return;u.releasingAudio=true;
  if(u.audio==null)return;
  Player owner=Bukkit.getPlayer(u.owner);
  try{if(closed||u.cancelledBeforeStart||owner==null||owner.isDead())u.audio.finish(false);
  else u.audio.phase("release",ticks);}catch(RuntimeException error){plugin.getLogger().log(java.util.logging.Level.WARNING,"Cleanup sound gagal; cleanup mekanik dilanjutkan",error);}
 }
 private boolean passable(Player p,Location next){var box=p.getBoundingBox().shift(next.getX()-p.getLocation().getX(),next.getY()-p.getLocation().getY(),next.getZ()-p.getLocation().getZ());for(int x=(int)Math.floor(box.getMinX());x<=Math.floor(box.getMaxX());x++)for(int y=(int)Math.floor(box.getMinY());y<=Math.floor(box.getMaxY());y++)for(int z=(int)Math.floor(box.getMinZ());z<=Math.floor(box.getMaxZ());z++)if(!next.getWorld().getBlockAt(x,y,z).isPassable())return false;return true;}
 private void teleport(Player p,Location loc){Location next=loc.clone();next.setYaw(p.getYaw());next.setPitch(p.getPitch());teleporting.add(p.getUniqueId());try{if(!p.teleport(next,PlayerTeleportEvent.TeleportCause.PLUGIN))throw new IllegalStateException("Teleport penguncian ditolak");}finally{teleporting.remove(p.getUniqueId());}}
 private void title(Use u,String text){for(Player p:near(u.center,u.spell.radius()))p.showTitle(Title.title(MiniMessage.miniMessage().deserialize("<gradient:#8B1538:#E34B60:#FFE4E8>"+text+"</gradient>"),Component.empty(),Title.Times.times(Duration.ofMillis(250),Duration.ofSeconds(3),Duration.ofMillis(500))));}
 private void circle(Use u){int count=Math.max(32,u.spell.radius()*10);for(int i=0;i<count;i++){double a=Math.PI*2*i/count;u.center.getWorld().spawnParticle(Particle.DUST,u.center.clone().add(Math.cos(a)*u.spell.radius(),0.1,Math.sin(a)*u.spell.radius()),1,new Particle.DustOptions(Color.fromRGB(139,21,56),1.5f));}}
 private void particles(Player p,SpellConfig s){for(double y=0;y<1.9;y+=.3){Location l=p.getLocation().add(0,y,0);p.getWorld().spawnParticle(Particle.DUST,l,s.config().getInt("particles.count",8),.35,.18,.35,0,new Particle.DustOptions(Color.fromRGB(12,0,2),1.8f));p.getWorld().spawnParticle(Particle.DUST,l,3,.3,.15,.3,0,new Particle.DustOptions(Color.fromRGB(139,21,56),1.4f));}}
 private void domainEffects(Use u){Set<UUID> inside=new HashSet<>();for(Player p:near(u.center,u.spell.radius())){inside.add(p.getUniqueId());boolean friendly=p.getUniqueId().equals(u.owner)||integrations.antihero(p);Boolean previous=u.roles.put(p.getUniqueId(),friendly);if(previous!=null&&previous!=friendly)clearLease(u,p.getUniqueId());if(friendly){effect(u,p,PotionEffectType.RESISTANCE,1);effect(u,p,PotionEffectType.FIRE_RESISTANCE,1);}else{effect(u,p,PotionEffectType.SLOWNESS,Math.max(0,u.spell.config().getInt("debuff.slowness-level",3)-1));effect(u,p,PotionEffectType.DARKNESS,0);effect(u,p,PotionEffectType.NAUSEA,0);}}for(UUID id:List.copyOf(u.affected))if(!inside.contains(id)){clearLease(u,id);u.affected.remove(id);}}
 private void effect(Use u,Player p,PotionEffectType type,int amplifier){Map<PotionEffectType,Lease> map=leases.computeIfAbsent(p.getUniqueId(),k->new HashMap<>());Lease existing=map.get(type);if(existing!=null&&existing.use!=u)return;
  PotionEffect current=p.getPotionEffect(type);if(existing==null){if(current!=null&&current.getAmplifier()>=amplifier)return;existing=new Lease(u,current,ticks,amplifier);try{victimJournal.put(p.getUniqueId(),type.getKey().getKey(),leaseRecord(existing));}catch(IOException e){throw new IllegalStateException("Journal efek gagal disimpan",e);}map.put(type,existing);}else if(existing.foreign)return;
  alteringPotion.add(p.getUniqueId());try{p.addPotionEffect(new PotionEffect(type,45,amplifier,true,false,true));}finally{alteringPotion.remove(p.getUniqueId());}u.affected.add(p.getUniqueId());
 }
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void potion(EntityPotionEffectEvent e){if(!(e.getEntity() instanceof Player p)||alteringPotion.contains(p.getUniqueId()))return;var map=leases.get(p.getUniqueId());if(map!=null){Lease l=map.get(e.getModifiedType());if(l!=null){try{victimJournal.remove(p.getUniqueId(),e.getModifiedType().getKey().getKey());l.foreign=true;}catch(IOException ex){e.setCancelled(true);plugin.getLogger().severe("Efek eksternal ditunda karena journal gagal: "+ex.getMessage());}}}}
 private String leaseRecord(Lease l){PotionEffect p=l.before;return l.amplifier+";"+(p==null?"none":p.getAmplifier()+";"+(p.isInfinite()?-1:Math.max(0,p.getDuration()-(int)(ticks-l.tick)))+";"+p.isAmbient()+";"+p.hasParticles()+";"+p.hasIcon());}
 private void checkpointVictims(){Map<String,String> values=new HashMap<>();for(var player:leases.entrySet())for(var entry:player.getValue().entrySet())if(!entry.getValue().foreign)values.put(player.getKey()+"."+entry.getKey().getKey().getKey(),leaseRecord(entry.getValue()));try{victimJournal.checkpoint(values);}catch(IOException ex){plugin.getLogger().severe("Checkpoint efek gagal; menghentikan sihir: "+ex.getMessage());for(Use u:List.copyOf(uses.values()))stop(u);}}
 @EventHandler public void join(PlayerJoinEvent e){recoverVictim(e.getPlayer());}
 private void recoverVictim(Player p){try{poses.recover(p);}catch(Exception ex){plugin.getLogger().severe("Journal pose dipertahankan: "+p.getUniqueId()+": "+ex.getMessage());}for(var entry:victimJournal.records(p.getUniqueId()).entrySet())try{
  if(entry.getKey().equals("tunduk_pose"))continue;
  if(entry.getKey().equals("crawl")){if(integrations.crawlProblem!=null)throw new IllegalStateException("Pemulihan GSit menunggu API: "+integrations.crawlProblem);if(integrations.crawl(p)!=null)throw new IllegalStateException("Ada crawl aktif saat pemulihan; sumber tidak dapat diverifikasi, tidak dihapus otomatis");victimJournal.remove(p.getUniqueId(),"crawl");continue;}
  PotionEffectType type=Registry.EFFECT.get(NamespacedKey.minecraft(entry.getKey()));if(type==null)throw new IllegalArgumentException("Efek tidak dikenal: "+entry.getKey());String[] parts=entry.getValue().split(";");int amplifier=Integer.parseInt(parts[0]);PotionEffect current=p.getPotionEffect(type);
  // Only our short ambient, particle-free lease fingerprint may be removed. A different source is left intact.
  if(current!=null&&current.getAmplifier()==amplifier&&current.getDuration()<=45&&current.isAmbient()&&!current.hasParticles()&&current.hasIcon()){
   alteringPotion.add(p.getUniqueId());try{p.removePotionEffect(type);if(!parts[1].equals("none")){int duration=Integer.parseInt(parts[2]);if(duration>0||duration==-1)p.addPotionEffect(new PotionEffect(type,duration,Integer.parseInt(parts[1]),Boolean.parseBoolean(parts[3]),Boolean.parseBoolean(parts[4]),Boolean.parseBoolean(parts[5])));}}finally{alteringPotion.remove(p.getUniqueId());}
  }
  victimJournal.remove(p.getUniqueId(),entry.getKey());
 }catch(Exception ex){plugin.getLogger().severe("Journal pemain dipertahankan untuk pemeriksaan: "+p.getUniqueId()+": "+ex.getMessage());}}
 private void clearLease(Use u,UUID id){var map=leases.get(id);if(map==null)return;Player p=Bukkit.getPlayer(id);for(var entry:List.copyOf(map.entrySet())){Lease lease=entry.getValue();if(lease.use!=u)continue;if(p!=null&&!lease.foreign){alteringPotion.add(id);try{p.removePotionEffect(entry.getKey());PotionEffect before=lease.before;if(before!=null){int remaining=before.isInfinite()?-1:before.getDuration()-(int)(ticks-lease.tick);if(remaining>0||remaining==-1)p.addPotionEffect(new PotionEffect(before.getType(),remaining,before.getAmplifier(),before.isAmbient(),before.hasParticles(),before.hasIcon()));}}finally{alteringPotion.remove(id);}}try{victimJournal.remove(id,entry.getKey().getKey().getKey());}catch(IOException ex){throw new IllegalStateException("Journal cleanup efek gagal",ex);}map.remove(entry.getKey());}if(map.isEmpty())leases.remove(id);}
 private void clearLeases(Use u){for(UUID id:List.copyOf(u.affected))clearLease(u,id);u.affected.clear();}
 private void release(Use u,UUID id){if(u.delivery!=null)try{u.delivery.releaseVictim(id);}catch(RuntimeException error){plugin.getLogger().log(java.util.logging.Level.WARNING,"Sound korban gagal dihentikan; cleanup korban dilanjutkan",error);}Lock lock=locks.get(id);if(lock!=null&&lock.use==u){Player p=Bukkit.getPlayer(id);if(p!=null&&lock.pose)poses.release(p);locks.remove(id);}u.victims.remove(id);clearLease(u,id);}
 private void releaseLocks(Use u){for(UUID id:List.copyOf(u.victims))release(u,id);}
 void stop(Use u){releaseAudio(u);u.stopping=true;if(u.started<0&&!state.active(u.owner))u.cancelledBeforeStart=true;try{mechanics.get(u.spell.id()).cleanup(this,u);releaseLocks(u);clearLeases(u);if(u.domain!=null)u.domain.emergencyRestore();if(u.domain==null||u.domain.fullyRestored())finish(u);}catch(Exception e){plugin.getLogger().log(java.util.logging.Level.SEVERE,"Cleanup sihir tertunda; marker tetap disimpan",e);}}
 private void finish(Use u)throws IOException {releaseLocks(u);clearLeases(u);if(u.cancelledBeforeStart)state.abort(u.owner);else state.finish(u.owner,System.currentTimeMillis());uses.remove(u.owner);if(u.audio!=null)u.audio.finish(!closed&&!u.cancelledBeforeStart&&Bukkit.getPlayer(u.owner)!=null&&!Bukkit.getPlayer(u.owner).isDead());Player owner=Bukkit.getPlayer(u.owner);if(owner!=null)tell(owner,u.cancelledBeforeStart?"Aktivasi dibatalkan sebelum dimulai.":u.spell.config().getString("messages.ended","Sihir selesai. Cooldown bersama lima menit dimulai."));}
 @EventHandler(priority=EventPriority.MONITOR)public void pose(EntityPoseChangeEvent e){if(e.getEntity() instanceof Player p)poses.externalChange(p,e.getPose());}
 @EventHandler(priority=EventPriority.HIGHEST)public void swim(EntityToggleSwimEvent e){if(e.getEntity() instanceof Player p&&!e.isSwimming()&&poses.holdsSwimming(p))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST)public void mount(EntityMountEvent e){if(locks.containsKey(e.getEntity().getUniqueId())||locks.containsKey(e.getMount().getUniqueId()))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST)public void vehicle(VehicleEnterEvent e){if(locks.containsKey(e.getEntered().getUniqueId()))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST)public void move(PlayerMoveEvent e){Lock l=locks.get(e.getPlayer().getUniqueId());if(l!=null&&e.hasChangedPosition()){Location to=l.position.clone();to.setYaw(e.getTo().getYaw());to.setPitch(e.getTo().getPitch());e.setTo(to);}}
 @EventHandler(priority=EventPriority.HIGHEST)public void teleport(PlayerTeleportEvent e){if(locks.containsKey(e.getPlayer().getUniqueId())&&!teleporting.contains(e.getPlayer().getUniqueId()))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST)public void interact(PlayerInteractEvent e){Lock l=locks.get(e.getPlayer().getUniqueId());if(l!=null&&(l.items||e.getMaterial()==Material.ENDER_PEARL))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST)public void interactEntity(PlayerInteractEntityEvent e){if(isAbilityBlocked(e.getPlayer().getUniqueId()))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST)public void drop(PlayerDropItemEvent e){if(isAbilityBlocked(e.getPlayer().getUniqueId()))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST)public void swap(PlayerSwapHandItemsEvent e){if(isAbilityBlocked(e.getPlayer().getUniqueId()))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST)public void held(PlayerItemHeldEvent e){if(isAbilityBlocked(e.getPlayer().getUniqueId()))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST)public void consume(PlayerItemConsumeEvent e){if(isAbilityBlocked(e.getPlayer().getUniqueId()))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST)public void shoot(ProjectileLaunchEvent e){if(e.getEntity().getShooter() instanceof Player p){Lock l=locks.get(p.getUniqueId());if(l!=null&&(l.items||e.getEntity() instanceof EnderPearl))e.setCancelled(true);}}
 @EventHandler(priority=EventPriority.HIGHEST)public void damage(EntityDamageEvent e){Entity source=e.getDamageSource().getCausingEntity(),direct=e.getDamageSource().getDirectEntity();Player attacker=source instanceof Player p?p:direct instanceof Player p?p:direct instanceof Projectile projectile&&projectile.getShooter() instanceof Player p?p:null;if(attacker==null)return;if(isAbilityBlocked(attacker.getUniqueId())){e.setCancelled(true);return;}double multiplier=1;for(Use u:uses.values())if(u.domain!=null&&!u.stopping&&u.started>=0&&ticks-u.started>400&&ticks-u.started<=400+u.spell.duration()*20L&&u.domain.contains(attacker.getLocation())&&!attacker.getUniqueId().equals(u.owner)&&!integrations.antihero(attacker))multiplier=Math.min(multiplier,Math.max(0,Math.min(1,u.spell.config().getDouble("debuff.damage-multiplier",.5))));e.setDamage(e.getDamage()*multiplier);}
 @EventHandler(priority=EventPriority.HIGHEST)public void breakBlock(BlockBreakEvent e){if(isAbilityBlocked(e.getPlayer().getUniqueId())){e.setCancelled(true);return;}for(Use u:uses.values())if(u.domain!=null&&!u.stopping&&u.started>=0&&ticks-u.started>400&&ticks-u.started<=400+u.spell.duration()*20L&&u.domain.contains(e.getBlock().getLocation())&&!e.getPlayer().getUniqueId().equals(u.owner)&&!integrations.antihero(e.getPlayer()))e.setCancelled(true);}
 private void departing(Player p){Use own=uses.get(p.getUniqueId());if(own!=null)stop(own);for(Use u:List.copyOf(uses.values()))release(u,p.getUniqueId());}
 @EventHandler public void worldChange(PlayerChangedWorldEvent e){departing(e.getPlayer());}
 @EventHandler public void quit(PlayerQuitEvent e){Use own=uses.get(e.getPlayer().getUniqueId());if(own!=null&&own.audio!=null)try{own.audio.finish(false);}catch(RuntimeException error){plugin.getLogger().log(java.util.logging.Level.WARNING,"Sound logout gagal dihentikan",error);}departing(e.getPlayer());}
 @EventHandler public void death(PlayerDeathEvent e){departing(e.getEntity());}
 @Override public void close(){closed=true;if(task!=null)task.cancel();for(Use u:List.copyOf(uses.values()))stop(u);try{domains.close();}catch(Exception ex){plugin.getLogger().log(java.util.logging.Level.SEVERE,"Journal domain dipertahankan; shutdown recovery gagal",ex);}for(Use u:List.copyOf(uses.values()))if(u.domain==null||u.domain.fullyRestored())try{finish(u);}catch(Exception e){plugin.getLogger().severe("Cooldown belum tersimpan: "+e.getMessage());}for(Player p:Bukkit.getOnlinePlayers())if(p.getOpenInventory().getTopInventory().getHolder() instanceof Menu)p.closeInventory();HandlerList.unregisterAll(this);}
 @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){List<String> out=new ArrayList<>();if(args.length==1){out.addAll(List.of("info","pakai"));if(sender.hasPermission("vitae.sihir.admin"))out.addAll(List.of("beri","hapus","cek","hentikan","cooldown","reload"));}else if(args.length==2&&(args[0].equals("info")||args[0].equals("pakai"))&&sender instanceof Player p)out.addAll(state.owned(p.getUniqueId()));else if(sender.hasPermission("vitae.sihir.admin")){if(args.length==3&&(args[0].equals("beri")||args[0].equals("hapus")))out.addAll(spells.keySet());else if(args[0].equals("cooldown")&&args.length==2)out.add("reset");else if(args.length==2||(args[0].equals("cooldown")&&args.length==3))for(OfflinePlayer p:Bukkit.getOfflinePlayers())if(p.getName()!=null&&(p.hasPlayedBefore()||p.isOnline()))out.add(p.getName());}String prefix=args[args.length-1].toLowerCase(Locale.ROOT);return out.stream().filter(s->s.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();}
 private static final class Menu implements InventoryHolder {Inventory inventory;final String selected;final Map<Integer,String> ids=new HashMap<>();Menu(String selected){this.selected=selected;}public Inventory getInventory(){return inventory;}}
 static final class Use {final UUID owner;final SpellConfig spell;final Location center;final DomainManager.Handle domain;final Set<UUID> victims=new HashSet<>(),affected=new HashSet<>();final Map<UUID,Boolean> roles=new HashMap<>();SpellAudio audio;SpellSoundDelivery delivery;long started=-1;int lastTitle=-1;boolean stopping,effectsEnded,cancelledBeforeStart,releasingAudio;Use(UUID owner,SpellConfig spell,Location center,DomainManager.Handle domain){this.owner=owner;this.spell=spell;this.center=domain==null?center.clone():domain.center();this.domain=domain;}}
 private static final class Lock {final Use use;final Location anchor;Location position;final boolean items;boolean pose;Lock(Use use,Location anchor,boolean items){this.use=use;this.anchor=anchor.clone();this.position=anchor.clone();this.items=items;}}
 private static final class Lease {final Use use;final PotionEffect before;final long tick;final int amplifier;boolean foreign;Lease(Use use,PotionEffect before,long tick,int amplifier){this.use=use;this.before=before;this.tick=tick;this.amplifier=amplifier;}}
}
