package com.bangzachery.vitae.vitaemanager.magic.domain;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Immutable write-ahead image. Snapshot is retained even after verified restoration. */
public final class DomainJournal {
    private static final int MAGIC = 0x56444D31;
    public record Image(UUID id, UUID owner, UUID world, int x,int y,int z,int radius,List<byte[]> blocks) {}
    private DomainJournal() {}
    public static void save(Path path, Image image) throws IOException {
        if(image.blocks.size()!=DomainGeometry.shell(image.radius).size())throw new IOException("Jumlah snapshot domain tidak lengkap");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)) {
            out.writeInt(MAGIC);out.writeUTF(image.id.toString());out.writeUTF(image.owner.toString());out.writeUTF(image.world.toString());
            out.writeInt(image.x);out.writeInt(image.y);out.writeInt(image.z);out.writeInt(image.radius);out.writeInt(image.blocks.size());
            for(byte[] block:image.blocks){out.writeInt(block.length);out.write(block);}
        }
        byte[] payload=bytes.toByteArray();
        ByteArrayOutputStream framed=new ByteArrayOutputStream();framed.write(payload);framed.write(digest(payload));
        atomic(path,framed.toByteArray());
        load(path); // Verify the exact durable image before the first world mutation.
    }
    public static Image load(Path path) throws IOException {
        byte[] bytes=Files.readAllBytes(path);
        if(bytes.length<36)throw new IOException("Journal domain terpotong: "+path);
        byte[] payload=Arrays.copyOf(bytes,bytes.length-32);
        if(!MessageDigest.isEqual(digest(payload),Arrays.copyOfRange(bytes,bytes.length-32,bytes.length)))throw new IOException("Checksum journal domain tidak cocok: "+path);
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(payload))) {
            if(in.readInt()!=MAGIC)throw new IOException("Versi journal domain tidak dikenal");
            UUID id=UUID.fromString(in.readUTF()), owner=UUID.fromString(in.readUTF()), world=UUID.fromString(in.readUTF());
            int x=in.readInt(), y=in.readInt(), z=in.readInt(), radius=in.readInt(), count=in.readInt();
            if(count!=DomainGeometry.shell(radius).size())throw new IOException("Jumlah snapshot domain tidak lengkap");
            List<byte[]> blocks=new ArrayList<>();
            for(int n=0;n<count;n++){int length=in.readInt();if(length<1||length>16_777_216||length>in.available())throw new IOException("Snapshot blok tidak lengkap");blocks.add(in.readNBytes(length));}
            if(in.available()!=0)throw new IOException("Journal domain memiliki trailing data");
            return new Image(id,owner,world,x,y,z,radius,List.copyOf(blocks));
        } catch(IllegalArgumentException ex){throw new IOException("Journal domain tidak valid",ex);}
    }
    static byte[] digest(byte[] bytes) {try{return MessageDigest.getInstance("SHA-256").digest(bytes);}catch(NoSuchAlgorithmException ex){throw new AssertionError(ex);}}
    public static void atomic(Path path,byte[] bytes)throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());Path tmp=path.resolveSibling(path.getFileName()+".tmp");
        try(FileChannel channel=FileChannel.open(tmp,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE)) {
            ByteBuffer b=ByteBuffer.wrap(bytes);while(b.hasRemaining())channel.write(b);channel.force(true);
        }
        Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
    }
}
