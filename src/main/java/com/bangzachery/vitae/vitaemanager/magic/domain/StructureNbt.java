package com.bangzachery.vitae.vitaemanager.magic.domain;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Semantic comparison: compound order and gzip headers are not part of a block's state. */
final class StructureNbt {
    private StructureNbt() {}
    static boolean equivalent(byte[] a,byte[] b)throws IOException{return read(a).equals(read(b));}
    private static Object read(byte[] compressed)throws IOException {
        try(DataInputStream in=new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(compressed)))) {
            int type=in.readUnsignedByte();if(type!=10)throw new IOException("Structure NBT root must be compound");in.readUTF();
            Object result=value(in,type,0);if(in.read()!=-1)throw new IOException("Trailing structure NBT");return result;
        }
    }
    private static Object value(DataInputStream in,int type,int depth)throws IOException {
        if(depth>64)throw new IOException("Structure NBT nesting limit");
        return switch(type) {
            case 1 -> in.readByte();case 2 -> in.readShort();case 3 -> in.readInt();case 4 -> in.readLong();
            case 5 -> in.readFloat();case 6 -> in.readDouble();
            case 7 -> {int n=size(in);byte[] bytes=in.readNBytes(n);if(bytes.length!=n)throw new EOFException();yield HexFormat.of().formatHex(bytes);}
            case 8 -> in.readUTF();
            case 9 -> {int child=in.readUnsignedByte(),n=size(in);List<Object> list=new ArrayList<>();list.add(child);for(int i=0;i<n;i++)list.add(value(in,child,depth+1));yield list;}
            case 10 -> {Map<String,Object> map=new TreeMap<>();for(int child;(child=in.readUnsignedByte())!=0;){String key=in.readUTF();if(map.put(key,List.of(child,value(in,child,depth+1)))!=null)throw new IOException("Duplicate NBT key");}yield map;}
            case 11 -> {int n=size(in);List<Integer> values=new ArrayList<>();for(int i=0;i<n;i++)values.add(in.readInt());yield values;}
            case 12 -> {int n=size(in);List<Long> values=new ArrayList<>();for(int i=0;i<n;i++)values.add(in.readLong());yield values;}
            default -> throw new IOException("Unknown NBT type "+type);
        };
    }
    private static int size(DataInputStream in)throws IOException{int n=in.readInt();if(n<0||n>16_777_216)throw new IOException("Structure NBT length limit");return n;}
}
