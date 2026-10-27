package com.bangzachery.vitae.vitaemanager.magic.domain;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class DomainInventoryProtectionTest {
    private final UUID worldId = UUID.randomUUID();
    private final World world = stub(World.class, name -> {
        if (name.equals("getUID")) return worldId;
        throw new AssertionError("Unexpected World method: " + name);
    });

    @Test void doubleChestSidesReturnToTheSameCombinedInventory() throws Exception {
        var manager = manager(true);
        var chest = doubleChest(100, 101, false);
        assertSame(chest, chest.getHolder().getLeftSide().getInventory());
        assertSame(chest, chest.getHolder().getRightSide().getInventory());
        assertFalse(protectedInventory(manager, chest));
    }

    @Test void eitherDoubleChestHalfProtectsTheCombinedInventoryInBothOrders() throws Exception {
        var manager = manager(true);
        // (0,64,2) is on the radius-two shell. The other half is not protected.
        for (boolean reversed : new boolean[]{false, true}) {
            assertTrue(protectedInventory(manager, doubleChest(reversed ? 0 : 1, reversed ? 1 : 0, true)));
        }
    }

    @Test void singleChestStillUsesItsPhysicalBlock() throws Exception {
        var manager = manager(true);
        assertTrue(protectedInventory(manager, singleChest(0)));
        assertFalse(protectedInventory(manager, singleChest(100)));
    }

    @Test void cyclicInventoryEdgesAndFreshDoubleChestHoldersStillTerminate() throws Exception {
        var manager = manager(true);
        DoubleChestInventory[] loop = new DoubleChestInventory[1];
        Inventory protectedSide = singleChest(0);
        loop[0] = stub(DoubleChestInventory.class, name -> switch (name) {
            case "getLeftSide" -> loop[0]; // actual object cycle, not an endless sequence of distinct mocks
            case "getRightSide" -> protectedSide;
            case "getHolder" -> new DoubleChest(loop[0]); // CraftInventoryDoubleChest creates fresh holders
            default -> throw new AssertionError(name);
        });
        assertTrue(protectedInventory(manager, loop[0]));
        Inventory outside = singleChest(100);
        DoubleChestInventory[] emptyLoop = new DoubleChestInventory[1];
        emptyLoop[0] = stub(DoubleChestInventory.class, name -> switch (name) {
            case "getLeftSide" -> emptyLoop[0];
            case "getRightSide" -> outside;
            case "getHolder" -> new DoubleChest(emptyLoop[0]);
            default -> throw new AssertionError(name);
        });
        assertFalse(protectedInventory(manager, emptyLoop[0]));
    }

    @Test void hopperTransfersCheckSourceAndDestinationWithoutBlanketCancellation() throws Exception {
        var manager = manager(true);
        Inventory hopper = inventory(null), protectedChest = doubleChest(1, 0, true), outside = doubleChest(100, 101, false);
        assertTransfer(manager, hopper, protectedChest, true);
        assertTransfer(manager, protectedChest, hopper, true);
        assertTransfer(manager, hopper, outside, false);
        assertTransfer(manager, outside, hopper, false);
        assertTransfer(manager, hopper, singleChest(0), true);
        assertTransfer(manager, singleChest(0), hopper, true);
        assertTransfer(manager, hopper, singleChest(100), false);
        assertTransfer(manager, singleChest(100), hopper, false);
    }

    @Test void plainInventoryWrapperCanPointBackThroughFreshCombinedHolders() throws Exception {
        var manager = manager(true);
        DoubleChestInventory[] combined = new DoubleChestInventory[1];
        Inventory wrapper = stub(Inventory.class, name -> {
            if (name.equals("getHolder")) return new DoubleChest(combined[0]);
            throw new AssertionError(name);
        });
        Inventory protectedSide = singleChest(0);
        combined[0] = stub(DoubleChestInventory.class, name -> switch (name) {
            case "getLeftSide" -> wrapper;
            case "getRightSide" -> protectedSide;
            case "getHolder" -> new DoubleChest(combined[0]);
            default -> throw new AssertionError(name);
        });
        assertTrue(protectedInventory(manager, wrapper));
        assertTransfer(manager, inventory(null), wrapper, true);
    }

    @Test void restoredDomainNoLongerProtectsItsContainers() throws Exception {
        var manager = manager(true);
        var handles = DomainManager.class.getDeclaredField("handles"); handles.setAccessible(true);
        var complete = DomainManager.Handle.class.getDeclaredField("complete"); complete.setAccessible(true);
        for (Object handle : (List<?>) handles.get(manager)) complete.setBoolean(handle, true);
        Inventory chest = doubleChest(1, 0, true), hopper = inventory(null);
        assertFalse(protectedInventory(manager, chest));
        assertTransfer(manager, hopper, chest, false);
        assertTransfer(manager, chest, hopper, false);
    }

    @Test void noActiveDomainLeavesSingleDoubleChestAndHopperTransfersAlone() throws Exception {
        var manager = manager(false);
        Inventory hopper = inventory(null), chest = doubleChest(0, 1, true);
        assertFalse(protectedInventory(manager, chest));
        assertFalse(protectedInventory(manager, singleChest(0)));
        assertTransfer(manager, hopper, chest, false);
        assertTransfer(manager, chest, hopper, false);
    }

    @Test void holderlessInventoryIsUnprotectedAndExistingCancellationIsPreserved() throws Exception {
        var manager = manager(true);
        Inventory empty = inventory(null);
        assertFalse(protectedInventory(manager, empty));
        var event = transfer(empty, empty);
        event.setCancelled(true);
        manager.inventory(event);
        assertTrue(event.isCancelled());
    }

    private Inventory singleChest(int x) {
        Inventory[] storage = new Inventory[1];
        Chest holder = chestHolder(x, storage);
        storage[0] = inventory(holder);
        return storage[0];
    }

    private DoubleChestInventory doubleChest(int leftX, int rightX, boolean shellZ) {
        Inventory[] combined = new Inventory[1];
        Chest left = chestHolder(leftX, combined, shellZ ? 2 : 100);
        Chest right = chestHolder(rightX, combined, shellZ ? 2 : 100);
        Inventory leftInventory = inventory(left), rightInventory = inventory(right);
        DoubleChestInventory joined = stub(DoubleChestInventory.class, name -> switch (name) {
            case "getLeftSide" -> leftInventory;
            case "getRightSide" -> rightInventory;
            case "getHolder" -> new DoubleChest((DoubleChestInventory) combined[0]);
            default -> throw new AssertionError(name);
        });
        combined[0] = joined;
        return joined;
    }

    private Chest chestHolder(int x, Inventory[] inventory) { return chestHolder(x, inventory, 2); }
    private Chest chestHolder(int x, Inventory[] inventory, int z) {
        Block block = stub(Block.class, name -> switch (name) {
            case "getWorld" -> world;
            case "getX" -> x;
            case "getY" -> 64;
            case "getZ" -> z;
            default -> throw new AssertionError(name);
        });
        return stub(Chest.class, name -> switch (name) {
            case "getBlock" -> block;
            case "getInventory" -> inventory[0]; // Chest.getInventory() rejoins a double chest
            default -> throw new AssertionError(name);
        });
    }

    private Inventory inventory(InventoryHolder holder) {
        return stub(Inventory.class, name -> {
            if (name.equals("getHolder")) return holder;
            throw new AssertionError(name);
        });
    }

    private DomainManager manager(boolean active) throws Exception {
        var manager = new DomainManager(null, Path.of("unused-domain-test"));
        if (active) {
            // Seed an existing journal-backed handle without starting Bukkit tasks or touching world data.
            var image = new DomainJournal.Image(UUID.randomUUID(), UUID.randomUUID(), worldId, 0, 64, 0, 2, List.of());
            var constructor = DomainManager.Handle.class.getDeclaredConstructor(DomainManager.class, DomainJournal.Image.class);
            constructor.setAccessible(true);
            var handles = DomainManager.class.getDeclaredField("handles"); handles.setAccessible(true);
            @SuppressWarnings("unchecked") var list = (List<DomainManager.Handle>) handles.get(manager);
            list.add(constructor.newInstance(manager, image));
        }
        return manager;
    }

    private boolean protectedInventory(DomainManager manager, Inventory inventory) throws Exception {
        var method = DomainManager.class.getDeclaredMethod("inventoryProtected", Inventory.class);
        method.setAccessible(true);
        return (boolean) method.invoke(manager, inventory);
    }

    private InventoryMoveItemEvent transfer(Inventory source, Inventory destination) {
        return new InventoryMoveItemEvent(source, new ItemStack() {}, destination, true);
    }
    private void assertTransfer(DomainManager manager, Inventory source, Inventory destination, boolean cancelled) {
        var event = transfer(source, destination);
        manager.inventory(event);
        assertEquals(cancelled, event.isCancelled());
    }

    private static <T> T stub(Class<T> type, Function<String, Object> methods) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> switch (method.getName()) {
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> type.getSimpleName() + " test fixture";
            default -> methods.apply(method.getName());
        }));
    }
}
