package com.bangzachery.vitae.vitaemanager.magic.domain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import static org.junit.jupiter.api.Assertions.*;
class DomainJournalTest {
    @TempDir Path directory;
    private DomainJournal.Image image(){return new DomainJournal.Image(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),1,65,-8,2,DomainGeometry.shell(2).stream().map(c->new byte[]{1,2,3}).toList());}
    @Test void durableImageReloadsAllBlocksAndKeepsExactOwner()throws Exception {Path file=directory.resolve("image.snapshot");var image=image();DomainJournal.save(file,image);var read=DomainJournal.load(file);assertEquals(image.owner(),read.owner());assertEquals(image.blocks().size(),read.blocks().size());for(int n=0;n<image.blocks().size();n++)assertArrayEquals(image.blocks().get(n),read.blocks().get(n));}
    @Test void interruptedTemporaryWriteCannotReplaceCommittedImage()throws Exception {Path file=directory.resolve("image.snapshot");var original=image();DomainJournal.save(file,original);Files.writeString(directory.resolve("image.snapshot.tmp"),"partial crash");assertEquals(original.id(),DomainJournal.load(file).id());}
    @Test void truncatedOrCorruptSnapshotFailsClosed()throws Exception {Path file=directory.resolve("image.snapshot");DomainJournal.save(file,image());byte[] bytes=Files.readAllBytes(file);bytes[30]^=2;Files.write(file,bytes);assertThrows(IOException.class,()->DomainJournal.load(file));Files.write(file,new byte[]{1,2});assertThrows(IOException.class,()->DomainJournal.load(file));}
    @Test void incompleteSnapshotSetCannotBeCommitted() {var original=image();var incomplete=new DomainJournal.Image(original.id(),original.owner(),original.world(),1,65,-8,2,List.of(new byte[]{1}));assertThrows(IOException.class,()->DomainJournal.save(directory.resolve("bad.snapshot"),incomplete));}
    private byte[] nbt(boolean reverse,int value)throws IOException {ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(new GZIPOutputStream(bytes))){out.writeByte(10);out.writeUTF("");if(reverse){out.writeByte(8);out.writeUTF("b");out.writeUTF("data");}out.writeByte(3);out.writeUTF("a");out.writeInt(value);if(!reverse){out.writeByte(8);out.writeUTF("b");out.writeUTF("data");}out.writeByte(0);}return bytes.toByteArray();}
    @Test void semanticVerificationIgnoresCompoundOrderingButDetectsChangedState()throws Exception{assertTrue(StructureNbt.equivalent(nbt(false,5),nbt(true,5)));assertFalse(StructureNbt.equivalent(nbt(false,5),nbt(true,6)));assertThrows(IOException.class,()->StructureNbt.equivalent(new byte[]{1},nbt(false,5)));}
    private byte[] containerNbt(int count)throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(new GZIPOutputStream(bytes))) {
            out.writeByte(10);out.writeUTF("");
            out.writeByte(10);out.writeUTF("nbt");
            out.writeByte(9);out.writeUTF("Items");out.writeByte(10);out.writeInt(1);
            out.writeByte(1);out.writeUTF("Slot");out.writeByte(0);
            out.writeByte(8);out.writeUTF("id");out.writeUTF("minecraft:diamond");
            out.writeByte(3);out.writeUTF("count");out.writeInt(count);
            out.writeByte(10);out.writeUTF("components");
            out.writeByte(8);out.writeUTF("minecraft:custom_name");out.writeUTF("{\"text\":\"Item tersimpan\"}");
            out.writeByte(10);out.writeUTF("minecraft:custom_data");out.writeByte(8);out.writeUTF("plugin-key");out.writeUTF("original");out.writeByte(0);
            out.writeByte(0);out.writeByte(0);out.writeByte(0);out.writeByte(0);
        }
        return bytes.toByteArray();
    }
    @Test void fullContainerItemComponentsSurviveJournalAndInterruptedRecoveryReplay()throws Exception {
        var original=image();byte[] full=containerNbt(64);
        var containerImage=new DomainJournal.Image(original.id(),original.owner(),original.world(),1,65,-8,2,DomainGeometry.shell(2).stream().map(c->full).toList());
        Path file=directory.resolve("container.snapshot");DomainJournal.save(file,containerImage);
        DomainJournal.atomic(directory.resolve("container.progress"),"5".getBytes());
        for(int restart=0;restart<2;restart++) {
            var recovered=DomainJournal.load(file);
            for(byte[] block:recovered.blocks()){assertArrayEquals(full,block);assertTrue(StructureNbt.equivalent(full,block));}
            assertTrue(Files.exists(file)); // Progress never authorizes deleting the original NBT image.
        }
    }
    @Test void containerVerificationDetectsMissingItemsInsteadOfAcceptingSameMaterial()throws Exception {
        assertFalse(StructureNbt.equivalent(containerNbt(64),containerNbt(63)));
    }
}
