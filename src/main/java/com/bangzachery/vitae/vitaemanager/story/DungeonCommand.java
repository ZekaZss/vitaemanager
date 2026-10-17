package com.bangzachery.vitae.vitaemanager.story;

import com.bangzachery.vitae.vitaemanager.npc.NpcData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;
import static com.bangzachery.vitae.vitaemanager.story.DungeonData.*;

public final class DungeonCommand implements CommandExecutor,TabCompleter {
    private record Selection(Location a,Location b){}
    private final DungeonService service;
    private final Map<UUID,Selection> selections=new HashMap<>();
    private static final List<String> TOPICS=List.of("portal","kaca","minion","bos","hadiah","aksi","sesi");
    private static final List<String> OPS=List.of("wand","pos1","pos2","region","location","bad","spawn","boss","npc","reward","time","retries","fade","check","on","off","open","stop","reset","status","join","receipt");
    public DungeonCommand(DungeonService service){this.service=service;}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){return execute(sender,args);}
    public boolean execute(CommandSender sender,String[] args){
        if(!sender.hasPermission("vitae.admin.dungeon")){sender.sendMessage(Component.text("Perlu izin vitae.admin.dungeon.",NamedTextColor.RED));return true;}
        try{
            if(args.length==0||args[0].equalsIgnoreCase("help")){help(sender,args.length>1?args[1]:"all");return true;}
            if(args[0].equalsIgnoreCase("list")){for(String id:service.current().definitions.keySet())service.status(sender,id);if(service.current().definitions.isEmpty())tell(sender,"Belum ada dungeon. /vdungeon create <id> <world>");return true;}
            if(args[0].equalsIgnoreCase("create")){arity(args,3,"/vdungeon create <id> <world>");World w=Bukkit.getWorld(args[2]);if(w==null)throw new IllegalArgumentException("World belum dimuat. Buat/muat world dahulu melalui pengelola world server.");service.create(args[1],w);tell(sender,"Dungeon dibuat dalam keadaan OFF. Berikutnya: /vdungeon help portal");return true;}
            if(args[0].equalsIgnoreCase("action")){action(sender,args);return true;}
            String id=NpcData.id(args[0]);service.get(id);if(args.length<2){service.status(sender,id);help(sender,"all");return true;}
            String op=args[1].toLowerCase(Locale.ROOT);
            switch(op){
                case "wand"->{
                    if(args.length<3||args.length>4)throw new IllegalArgumentException("/vdungeon <id> wand <area> | wand bad <nama-petak>");
                    String kind=args[2].toLowerCase(Locale.ROOT);String tile=kind.equals("bad")&&args.length==4?NpcData.id(args[3]):"";
                    if(kind.equals("bad")?tile.isEmpty():!List.of("entrance","arena","camera","finish","miniongate","miniontrigger","bossgate","bosstrigger","exit").contains(kind))throw new IllegalArgumentException("Pilih nama area atau bad <nama-petak>.");
                    if(!kind.equals("bad")&&args.length!=3)throw new IllegalArgumentException("/vdungeon <id> wand <area>");
                    service.assertEditable(id);
                    var mode=kind.equals("exit")?com.bangzachery.vitae.vitaemanager.selection.SelectionService.Mode.FACE:
                        Set.of("entrance","camera","finish","miniontrigger","bosstrigger").contains(kind)?com.bangzachery.vitae.vitaemanager.selection.SelectionService.Mode.WALK:com.bangzachery.vitae.vitaemanager.selection.SelectionService.Mode.BLOCKS;
                    com.bangzachery.vitae.vitaemanager.selection.SelectionService.require().arm(player(sender),"vitae.admin.dungeon",id+" • "+kind,mode,(p,a,b)->{
                        selections.put(p.getUniqueId(),new Selection(a,b));
                        execute(p,kind.equals("bad")?new String[]{id,"bad","add",tile}:new String[]{id,"region",kind});
                    });
                }
                case "pos1","pos2"->{arity(args,2,"/vdungeon <id> pos1|pos2");Player p=player(sender);Selection old=selections.getOrDefault(p.getUniqueId(),new Selection(null,null));var hit=p.getTargetBlockExact(6);Location at=hit==null?p.getLocation():hit.getLocation();selections.put(p.getUniqueId(),op.equals("pos1")?new Selection(at.clone(),old.b):new Selection(old.a,at.clone()));tell(sender,op+" = "+at.getBlockX()+", "+at.getBlockY()+", "+at.getBlockZ()+". Arahkan crosshair ke blok untuk memilih blok; lihat udara untuk memilih posisi kaki.");}
                case "region"->{arity(args,3,"/vdungeon <id> region entrance|arena|camera|finish|miniongate|miniontrigger|bossgate|bosstrigger|exit");Box b=selection(player(sender));service.edit(id,d->{switch(args[2].toLowerCase(Locale.ROOT)){case "entrance"->d.entrance=b;case "arena"->d.arena=b;case "camera"->d.cameraZone=b;case "finish"->d.finish=b;case "miniongate"->d.minionGate=b;case "miniontrigger"->d.minionTrigger=b;case "bossgate"->d.bossGate=b;case "bosstrigger"->d.bossTrigger=b;case "exit"->d.exit=b;default->throw new IllegalArgumentException("Jenis area tidak dikenal. /vdungeon help portal");}});tell(sender,"Area "+args[2]+" tersimpan ("+b.volume()+" blok). Dungeon OFF sampai setup selesai.");}
                case "location"->{arity(args,3,"/vdungeon <id> location entry|home|start|camera|next");NpcData.Point at=point(player(sender).getLocation());service.edit(id,d->{switch(args[2].toLowerCase(Locale.ROOT)){case "entry"->d.entry=at;case "home"->d.home=at;case "start"->d.start=at;case "camera"->d.camera=at;case "next"->d.next=at;default->throw new IllegalArgumentException("Jenis lokasi tidak dikenal.");}});tell(sender,"Lokasi "+args[2]+" dan arah pandangnya tersimpan.");}
                case "bad"->{arity(args,4,"/vdungeon <id> bad add|remove <nama-petak>");String tile=NpcData.id(args[3]);Box b=args[2].equalsIgnoreCase("add")?selection(player(sender)):null;service.edit(id,d->{if(args[2].equalsIgnoreCase("add")){if(b.y1()!=b.y2())throw new IllegalArgumentException("Seleksi petak kaca harus satu lapis.");d.bad.put(tile,b);}else if(args[2].equalsIgnoreCase("remove"))d.bad.remove(tile);else throw new IllegalArgumentException("bad add|remove");});tell(sender,"Pijakan salah "+tile+" diperbarui.");}
                case "spawn"->{if(args.length==4&&args[2].equalsIgnoreCase("remove")){service.edit(id,d->d.minions.remove(args[3]));tell(sender,"Spawn dihapus.");break;}
                    if(args.length<5||args.length>6)throw new IllegalArgumentException("/vdungeon <id> spawn <nama-titik> vanilla:COW|mythic:ID <jumlah> [level]");String name=NpcData.id(args[2]);Mob m=new Mob(args[3],number(args[4],1,4096),args.length==6?Double.parseDouble(args[5]):1,point(player(sender).getLocation()));service.edit(id,d->d.minions.put(name,m));tell(sender,"Spawn "+name+" tersimpan. Total minion: "+service.get(id).minions.values().stream().mapToInt(Mob::count).sum());}
                case "boss"->{if(args.length<3||args.length>4)throw new IllegalArgumentException("/vdungeon <id> boss mythic:ID|vanilla:TYPE [level]");Mob m=new Mob(args[2],1,args.length==4?Double.parseDouble(args[3]):1,point(player(sender).getLocation()));service.edit(id,d->d.boss=m);tell(sender,"Lokasi dan jenis bos tersimpan.");}
                case "npc"->{if(args.length<4||args.length>5)throw new IllegalArgumentException("/vdungeon <id> npc glass|minion|boss <id-npc> [halaman-selamat]");String npc=NpcData.id(args[3]),page=args.length==5?NpcData.id(args[4]):"";service.edit(id,d->{switch(args[2].toLowerCase(Locale.ROOT)){case "glass"->{d.glassNpc=npc;d.glassPage=page;}case "minion"->{d.minionNpc=npc;d.minionPage=page;}case "boss"->{d.bossNpc=npc;d.bossPage=page;}default->throw new IllegalArgumentException("Tahap glass|minion|boss.");}});tell(sender,"NPC "+npc+" dihubungkan ke "+args[2]+". Buat pilihan DUNGEON_"+args[2].toUpperCase(Locale.ROOT)+" di dialog /vnpc.");}
                case "reward"->{arity(args,3,"/vdungeon <id> reward glass|minion|boss");service.rewardMenu(player(sender),id,args[2].toLowerCase(Locale.ROOT));}
                case "time"->{arity(args,4,"/vdungeon <id> time glass|escape|portal <detik>");String kind=args[2].toLowerCase(Locale.ROOT);if(!Set.of("glass","escape","portal").contains(kind))throw new IllegalArgumentException("Waktu glass|escape|portal.");int n=number(args[3],kind.equals("glass")?10:1,kind.equals("glass")?Integer.MAX_VALUE:300);service.edit(id,d->{switch(kind){case "glass"->d.glassSeconds=n;case "escape"->d.escapeSeconds=n;case "portal"->d.portalAfter=n;}});tell(sender,"Waktu "+kind+" = "+n+" detik. Kaca 10 jam: time glass 36000. Timer tetap bersama dan tidak diulang saat retry.");}
                case "retries"->{arity(args,3,"/vdungeon <id> retries <jumlah-kembali-ke-start>");int n=number(args[2],0,Integer.MAX_VALUE);service.edit(id,d->d.glassRetries=n);tell(sender,"Kaca: maksimal "+n+" kali kembali ke start per pemain. Jatuh setelah jatah habis berarti mati; 0 mematikan retry.");}
                case "fade"->{arity(args,4,"/vdungeon <id> fade entry|exit on|off");if(!Set.of("on","off").contains(args[3].toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Pilih on atau off.");boolean on=args[3].equalsIgnoreCase("on");service.edit(id,d->{if(args[2].equalsIgnoreCase("entry"))d.fadeEntry=on;else if(args[2].equalsIgnoreCase("exit"))d.fadeExit=on;else throw new IllegalArgumentException("Fade hanya entry|exit.");});tell(sender,"Fade "+args[2]+" "+args[3]+". Sakelar utama tetap /vitae fade on|off.");}
                case "check"->{arity(args,2,"/vdungeon <id> check");service.prepare(sender,id,"check");}
                case "on","off"->{arity(args,2,"/vdungeon <id> on|off");if(op.equals("on"))service.prepare(sender,id,"on");else{service.enable(id,false);tell(sender,"Dungeon OFF.");}}
                case "open"->{arity(args,2,"/vdungeon <id> open");service.prepare(sender,id,"open");}
                case "stop","reset"->{arity(args,2,"/vdungeon <id> stop|reset");service.stop(id,"Sesi dihentikan admin.");tell(sender,"Peserta dipulangkan dan blok dipulihkan bertahap. Cek status sebelum open kembali.");}
                case "status"->{arity(args,2,"/vdungeon <id> status");service.status(sender,id);}
                case "join"->{if(args.length>3)throw new IllegalArgumentException("/vdungeon <id> join [pemain]");Player target=args.length==3?Bukkit.getPlayerExact(args[2]):player(sender);if(target==null)throw new IllegalArgumentException("Pemain tidak online.");service.join(target,id);}
                case "receipt"->{if(args.length<3||args.length>4)throw new IllegalArgumentException("/vdungeon <id> receipt glass|minion|boss [retry|delivered]");service.receipt(sender,id,args[2],args.length==4?args[3]:"status");}
                default->{help(sender,"all");throw new IllegalArgumentException("Subcommand tidak dikenal: "+op);}
            }
        }catch(Exception error){sender.sendMessage(Component.text("[Vitae Dungeon] "+Objects.requireNonNullElse(error.getMessage(),"Periksa log server."),NamedTextColor.RED));}
        return true;
    }
    private void action(CommandSender sender,String[] args)throws Exception{
        if(args.length<5)throw new IllegalArgumentException("/vdungeon action <id> <id-npc> teleport|spawn ... • /vdungeon help aksi");Action a=new Action();a.id=NpcData.id(args[1]);if(a.id.length()>41)throw new IllegalArgumentException("ID aksi maksimal 41 huruf agar target pilihan NPC muat.");a.npc=NpcData.id(args[2]);a.kind=args[3].toLowerCase(Locale.ROOT);a.enabled=true;
        if(a.kind.equals("teleport")){if(args.length<5||args.length>6)throw new IllegalArgumentException("/vdungeon action <id> <npc> teleport here [on|off untuk fade]");if(!args[4].equalsIgnoreCase("here"))throw new IllegalArgumentException("Berdiri di tujuan, gunakan teleport here.");if(args.length==6&&!Set.of("on","off").contains(args[5].toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Fade on|off.");a.point=point(player(sender).getLocation());a.fade=args.length==6&&args[5].equalsIgnoreCase("on");}
        else if(a.kind.equals("spawn")){if(args.length<6||args.length>7)throw new IllegalArgumentException("/vdungeon action <id> <npc> spawn vanilla:TYPE|mythic:ID <jumlah> [level]");a.mob=new Mob(args[4],number(args[5],1,64),args.length==7?Double.parseDouble(args[6]):1,point(player(sender).getLocation()));}
        else throw new IllegalArgumentException("Aksi teleport|spawn.");service.actionEdit(a);tell(sender,"Aksi tersimpan. Pilihan NPC memakai target ACTION_"+a.id+". Contoh: /vnpc "+a.npc+" option start ACTION_"+a.id+" Lanjutkan");
    }
    private Box selection(Player p){Selection s=selections.get(p.getUniqueId());if(s==null||s.a==null||s.b==null)throw new IllegalArgumentException("Tandai /vdungeon <id> pos1 dan pos2 dahulu.");return Box.between(s.a,s.b);}
    private Player player(CommandSender s){if(!(s instanceof Player p))throw new IllegalArgumentException("Perintah lokasi/seleksi/GUI harus dilakukan di dalam game.");return p;}
    private int number(String s,int min,int max){int n=Integer.parseInt(s);if(n<min||n>max)throw new IllegalArgumentException("Angka harus "+min+"–"+max);return n;}
    private void arity(String[] a,int n,String usage){if(a.length!=n)throw new IllegalArgumentException(usage);}
    private void tell(CommandSender s,String text){DungeonService.tell(s,text);}
    private void help(CommandSender s,String topic){
        s.sendMessage(Component.empty());s.sendMessage(Component.text("━━ Vitae Dungeon • "+topic+" ━━",NamedTextColor.GOLD));
        switch(topic.toLowerCase(Locale.ROOT)){
            case "portal"->{tell(s,"create <id> <world> • <id> pos1|pos2 • <id> region entrance|exit");tell(s,"Berdiri di tujuan: <id> location entry|home. Isi portal keluar harus udara; bingkai tidak ikut diseleksi.");tell(s,"<id> fade entry|exit on|off • <id> time escape 30 • <id> time portal 15");}
            case "kaca"->{tell(s,"<id> region arena|camera|finish • <id> location start|camera|next");tell(s,"Posisi start wajib di area pemicu camera. Lokasi camera merekam posisi, yaw, dan pitch admin; durasi 5 detik.");tell(s,"Pilih dua sudut petak salah satu lapis, ukuran bebas: <id> bad add <nama> • bad remove <nama>");tell(s,"<id> time glass 36000 = 10 jam • <id> retries 3 = tiga kali kembali ke start; jatuh berikutnya mati.");tell(s,"Retry menjaga inventory dan tidak mengulang kamera, timer bersama, atau kaca yang sudah pecah.");tell(s,"<id> npc glass <npc> [page-selamat] • target dialog DUNGEON_GLASS");}
            case "minion"->{tell(s,"<id> region miniongate|miniontrigger • <id> npc minion <npc> [page-selamat]");tell(s,"Di lokasi spawn: <id> spawn <nama-titik> mythic:ID|vanilla:COW <jumlah> [level]. Ulangi untuk tiap titik.");tell(s,"Target dialog DUNGEON_MINION. Progres semua peserta sama; hanya mob sesi yang dihitung.");}
            case "bos"->{tell(s,"<id> region bossgate|bosstrigger • <id> boss mythic:ID [level]");tell(s,"<id> npc boss <npc> [page-selamat]. Target dialog DUNGEON_BOSS. Syarat kaca dan minion selesai.");}
            case "hadiah"->{tell(s,"<id> reward glass|minion|boss → letakkan item dalam GUI → tutup untuk simpan; item admin dikembalikan.");tell(s,"Halaman selamat memakai teks dari NPC; pilihan Ambil hadiah diberikan otomatis untuk penerima terpilih.");tell(s,"Belum pernah menerima diprioritaskan; jika tidak ada, peserta sebelumnya boleh mendapat lagi.");}
            case "aksi"->{tell(s,"action <id> <npc> teleport here • action <id> <npc> spawn mythic:ID|vanilla:TYPE <jumlah> [level]");tell(s,"/vnpc <npc> option <halaman> ACTION_<id> <label pilihan>. Aksi ini bisa digunakan di luar dungeon.");}
            case "sesi"->{tell(s,"<id> check → on → open. Saat satu peserta menerima kaca, semua peserta aktif di world mengikuti.");tell(s,"<id> status|stop|reset • list • <id> join [pemain]. Pemain mati/disconnect saat permainan tidak bisa masuk lagi.");tell(s,"Reset memulihkan blok bertahap. Setelah selesai, open membuat sesi baru.");}
            default->{tell(s,"/vdungeon create <id> <world> • /vdungeon list");tell(s,"/vdungeon help portal|kaca|minion|bos|hadiah|aksi|sesi");tell(s,"Urutan: world/portal → kaca/kamera → NPC/minion/bos → hadiah → check → on → open.");}
        }
        tell(s,"Alat seleksi: /vdungeon <id> wand <area> • klik kiri pos1, klik kanan pos2; otomatis tersimpan.");
        s.sendMessage(Component.empty());
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){
        if(!sender.hasPermission("vitae.admin.dungeon"))return List.of();List<String> values=List.of();
        if(args.length==1){var v=new ArrayList<>(service.current().definitions.keySet());v.addAll(List.of("help","create","list","action"));values=v;}
        else if(args.length==2)values=args[0].equalsIgnoreCase("help")?TOPICS:OPS;
        else if(args.length==3)values=switch(args[1].toLowerCase(Locale.ROOT)){case "wand"->List.of("entrance","arena","camera","finish","miniongate","miniontrigger","bossgate","bosstrigger","exit","bad");case "region"->List.of("entrance","arena","camera","finish","miniongate","miniontrigger","bossgate","bosstrigger","exit");case "location"->List.of("entry","home","start","camera","next");case "reward","npc","receipt"->List.of("glass","minion","boss");case "bad"->List.of("add","remove");case "time"->List.of("glass","escape","portal");case "fade"->List.of("entry","exit");default->List.of();};
        else if(args.length==4&&args[1].equalsIgnoreCase("fade"))values=List.of("on","off");
        else if(args.length==3&&args[1].equalsIgnoreCase("retries"))values=List.of("0","3","5");
        else if(args.length==4&&args[1].equalsIgnoreCase("time")&&args[2].equalsIgnoreCase("glass"))values=List.of("180","36000");
        String prefix=args[args.length-1].toLowerCase(Locale.ROOT);return values.stream().filter(v->v.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
