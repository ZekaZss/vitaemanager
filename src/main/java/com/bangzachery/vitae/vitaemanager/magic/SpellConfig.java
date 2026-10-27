package com.bangzachery.vitae.vitaemanager.magic;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.util.*;
record SpellConfig(String id,String name,String description,Material icon,int radius,int duration,YamlConfiguration config){
 static Map<String,SpellConfig> load(JavaPlugin plugin,Collection<String> ids){
  Map<String,SpellConfig> result=new LinkedHashMap<>();
  for(String id:ids){
   String path="magic/"+id+".yml";File file=new File(plugin.getDataFolder(),path);if(!file.exists())plugin.saveResource(path,false);
   var c=new YamlConfiguration();try{c.load(file);}catch(Exception e){throw new IllegalArgumentException("YAML sihir tidak valid: "+id,e);}Material icon=Material.matchMaterial(c.getString("icon","NETHER_STAR"));
   int radius=c.getInt("radius",id.equals("unlimited_misery")?30:20),duration=c.getInt("duration-seconds",id.equals("unlimited_misery")?300:id.equals("telekinesis")?30:20);
   if(radius<1||radius>64||duration<1||duration>3600||icon==null||!icon.isItem())throw new IllegalArgumentException("Konfigurasi sihir tidak valid: "+id);
   range(c,"maximum-rise",7,0,7);range(c,"rise-per-tick",0.05,0.001,0.25);range(c,"particles.count",8,0,64);range(c,"particles.size",1.8,0.1,4);range(c,"debuff.damage-multiplier",.5,0,1);range(c,"healing.level",10,1,20);range(c,"healing.interval-ticks",20,1,72000);range(c,"debuff.slowness-level",3,1,128);range(c,"camera.interval-ticks",10,1,1200);
   var spell=new SpellConfig(id,c.getString("name",id),c.getString("description",""),icon,radius,duration,c);
   var plan=spell.soundPlan();for(String phase:SpellSoundPlan.PHASES)for(var cue:plan.cues(phase))soundKey(cue);
   result.put(id,spell);
  }return Collections.unmodifiableMap(result);
 }
 void sound(Location location,String phase){var s=config.getConfigurationSection("sounds."+phase);if(s==null||!s.getBoolean("enabled",true))return;String sound=s.getString("id","minecraft:entity.warden.sonic_boom");
  if(s.getString("type","vanilla").equals("vanilla"))try{sound=Registry.SOUNDS.getKeyOrThrow(Sound.valueOf(sound.toUpperCase(Locale.ROOT))).toString();}catch(IllegalArgumentException ignored){sound=sound.toLowerCase(Locale.ROOT);}
  location.getWorld().playSound(location,sound,SoundCategory.PLAYERS,(float)s.getDouble("volume",1),(float)s.getDouble("pitch",1));
 }
 static void validateSoundSyntax(String type,String id){if(!type.equals("vanilla")&&!type.equals("custom"))throw new IllegalArgumentException("Jenis sound tidak valid");String value=type.equals("vanilla")?id.toLowerCase(Locale.ROOT):id;if(!value.matches("(?:[a-z0-9._-]+:)?[a-z0-9/._-]+"))throw new IllegalArgumentException("ID sound tidak valid: "+id);}
 static void range(YamlConfiguration c,String key,double def,double min,double max){double value=c.getDouble(key,def);if(!Double.isFinite(value)||value<min||value>max)throw new IllegalArgumentException("Nilai konfigurasi tidak valid: "+key);}
 int interval(){return Math.max(1,config.getInt("sounds.active.interval-ticks",50));}
 SpellSoundPlan soundPlan(){
  var defaults=new YamlConfiguration();
  try(var resource=SpellConfig.class.getResourceAsStream("/magic/"+id+".yml")){
   if(resource!=null)defaults.load(new InputStreamReader(resource,java.nio.charset.StandardCharsets.UTF_8));
  }catch(Exception ex){throw new IllegalArgumentException("Default sound tidak dapat dimuat: "+id,ex);}
  return SpellSoundPlan.parse(config,defaults);
 }
 static String soundKey(SpellSoundPlan.Cue cue){
  if(cue.type().equals("custom"))return cue.id();
  try{return Registry.SOUNDS.getKeyOrThrow(Sound.valueOf(cue.id().toUpperCase(Locale.ROOT))).toString();}
  catch(IllegalArgumentException ex){var key=NamespacedKey.fromString(cue.id().toLowerCase(Locale.ROOT));if(key==null||Registry.SOUNDS.get(key)==null)throw new IllegalArgumentException("Sound vanilla tidak dikenal: "+cue.id());return key.toString();}
 }
}
