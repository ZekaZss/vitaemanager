package com.bangzachery.vitae.vitaemanager.magic;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

/** Single authoritative UUID ownership/use/cooldown store. Writes publish only after atomic durable replace. */
public final class MagicState {
 private final Path file; private Properties data=new Properties();
 public MagicState(Path file) throws IOException {
  this.file=file; if(Files.exists(file)) try(var in=Files.newInputStream(file)){data.load(in);}
  for(String key:data.stringPropertyNames()) {
   if(key.endsWith(".cooldown")) Long.parseLong(data.getProperty(key));
   if(key.endsWith(".owned") && owned(UUID.fromString(key.substring(0,key.indexOf('.')))).size()>9) throw new IOException("Kepemilikan melebihi sembilan: "+key);
  }
 }
 public Set<String> owned(UUID id){String s=data.getProperty(id+".owned",""); return s.isEmpty()?Set.of():Collections.unmodifiableSet(new LinkedHashSet<>(List.of(s.split(","))));}
 public String using(UUID id){return data.getProperty(id+".active");}
 public Set<UUID> activeOwners(){var result=new HashSet<UUID>();for(String key:data.stringPropertyNames())if(key.endsWith(".active"))result.add(UUID.fromString(key.substring(0,key.indexOf('.'))));return Set.copyOf(result);}
 public void abort(UUID id)throws IOException {var p=copy();p.remove(id+".active");save(p);}
 public boolean active(UUID id){return using(id)!=null;}
 public long cooldown(UUID id){return Long.parseLong(data.getProperty(id+".cooldown","0"));}
 public boolean grant(UUID id,String spell)throws IOException {var set=new LinkedHashSet<>(owned(id));if(set.size()>=9||!set.add(spell))return false;var p=copy();p.setProperty(id+".owned",String.join(",",set));save(p);return true;}
 public boolean revoke(UUID id,String spell)throws IOException {var set=new LinkedHashSet<>(owned(id));if(!set.remove(spell))return false;var p=copy();p.setProperty(id+".owned",String.join(",",set));save(p);return true;}
 public void begin(UUID id,String spell)throws IOException {if(active(id))throw new IllegalStateException("Penggunaan masih aktif");var p=copy();p.setProperty(id+".active",spell);save(p);}
 public void finish(UUID id,long now)throws IOException {if(!active(id))return;var p=copy();p.remove(id+".active");p.setProperty(id+".cooldown",Long.toString(Math.addExact(now,300000)));save(p);}
 public void reset(UUID id)throws IOException {var p=copy();p.remove(id+".cooldown");save(p);}
 public void recover(long now)throws IOException {var p=copy();for(String key:data.stringPropertyNames())if(key.endsWith(".active")){p.remove(key);p.setProperty(key.replace(".active",".cooldown"),Long.toString(now+300000));}if(!p.equals(data))save(p);}
 private Properties copy(){var p=new Properties();p.putAll(data);return p;}
 private void save(Properties next)throws IOException {
  Files.createDirectories(file.getParent());Path temp=file.resolveSibling(file.getFileName()+".tmp");
  try(var out=Files.newOutputStream(temp)){next.store(out,"Vitae magic state v1 - do not edit while running");}
  try(var channel=FileChannel.open(temp,StandardOpenOption.WRITE)){channel.force(true);}
  Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);data=next;
 }
}
