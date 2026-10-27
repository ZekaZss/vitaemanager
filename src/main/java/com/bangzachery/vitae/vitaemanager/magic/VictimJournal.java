package com.bangzachery.vitae.vitaemanager.magic;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
/** Write-ahead player-effect evidence, separate from permanent ownership. No Bukkit access. */
final class VictimJournal {
 private final Path file;private Properties data=new Properties();
 VictimJournal(Path file)throws IOException{this.file=file;if(Files.exists(file))try(var in=Files.newInputStream(file)){data.load(in);}}
 Map<String,String> records(UUID victim){Map<String,String> result=new HashMap<>();String prefix=victim+".";for(String key:data.stringPropertyNames())if(key.startsWith(prefix))result.put(key.substring(prefix.length()),data.getProperty(key));return result;}
 void put(UUID victim,String type,String value)throws IOException{Properties next=copy();next.setProperty(victim+"."+type,value);save(next);}
 void remove(UUID victim,String type)throws IOException{String key=victim+"."+type;if(!data.containsKey(key))return;Properties next=copy();next.remove(key);save(next);}
 void checkpoint(Map<String,String> entries)throws IOException{Properties next=copy();for(var entry:entries.entrySet())next.setProperty(entry.getKey(),entry.getValue());if(!next.equals(data))save(next);}
 private Properties copy(){Properties copy=new Properties();copy.putAll(data);return copy;}
 private void save(Properties next)throws IOException{Files.createDirectories(file.getParent());Path tmp=file.resolveSibling(file.getFileName()+".tmp");try(var out=Files.newOutputStream(tmp)){next.store(out,"Vitae player effect recovery v1");}try(var c=FileChannel.open(tmp,StandardOpenOption.WRITE)){c.force(true);}Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);data=next;}
}
