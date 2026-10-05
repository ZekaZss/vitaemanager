package com.bangzachery.vitae.vitaemanager.guis;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class ProfileHolders {

    public static class OnlinePlayersHolder implements InventoryHolder {
        private Inventory inventory;
        private final int page;
        public OnlinePlayersHolder(int page) { this.page = page; }
        public int getPage() { return page; }
        public void setInventory(Inventory inventory) { this.inventory = inventory; }
        @Override public @NotNull Inventory getInventory() { return inventory; }
    }

    public static class PlayerProfileHolder implements InventoryHolder {
        private Inventory inventory;
        private final Player target;
        public PlayerProfileHolder(Player target) { this.target = target; }
        public Player getTarget() { return target; }
        public void setInventory(Inventory inventory) { this.inventory = inventory; }
        @Override public @NotNull Inventory getInventory() { return inventory; }
    }

    public static class CustomInvseeHolder implements InventoryHolder {
        private Inventory inventory;
        private final Player target;
        public CustomInvseeHolder(Player target) { this.target = target; }
        public Player getTarget() { return target; }
        public void setInventory(Inventory inventory) { this.inventory = inventory; }
        @Override public @NotNull Inventory getInventory() { return inventory; }
    }
}