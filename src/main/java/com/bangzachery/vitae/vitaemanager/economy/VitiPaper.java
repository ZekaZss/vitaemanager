package com.bangzachery.vitae.vitaemanager.economy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class VitiPaper {
    public record Value(
            UUID serial,
            BigDecimal amount,
            boolean legacy) {}

    private final NamespacedKey balance;
    private final NamespacedKey amount;
    private final NamespacedKey exact;
    private final NamespacedKey serial;
    private final NamespacedKey legacy;

    public VitiPaper(Plugin plugin) {
        balance = new NamespacedKey(plugin, "viti_balance");
        amount = new NamespacedKey(plugin, "viti_paper_amount");
        exact = new NamespacedKey(plugin, "viti_paper_exact");
        serial = new NamespacedKey(plugin, "viti_paper_id");
        legacy = new NamespacedKey(plugin, "viti_paper_legacy");
    }

    public BigDecimal oldBalance(Player player) {
        var data = player.getPersistentDataContainer();

        if (data.has(balance)
                && !data.has(
                balance, PersistentDataType.DOUBLE)) {
            throw new IllegalArgumentException(
                    "Tipe PDC saldo lama tidak valid");
        }

        Double value = data.get(
                balance, PersistentDataType.DOUBLE);

        return value == null
                ? BigDecimal.ZERO
                : VitiAmount.stored(value);
    }

    public void mirror(Player player, BigDecimal value) {
        player.getPersistentDataContainer().set(
                balance,
                PersistentDataType.DOUBLE,
                value.doubleValue());
    }

    public boolean isPaper(ItemStack item) {
        return item != null
                && item.getType() == Material.PAPER
                && item.hasItemMeta()
                && item.getItemMeta()
                .getPersistentDataContainer().has(amount);
    }

    public Value read(ItemStack item) {
        if (!isPaper(item) || item.getAmount() < 1) {
            throw new VitiLedger.Failure("viti-note-invalid");
        }

        var data = item.getItemMeta()
                .getPersistentDataContainer();

        if ((data.has(exact)
                && !data.has(exact, PersistentDataType.STRING))
                || (data.has(serial)
                && !data.has(serial, PersistentDataType.STRING))
                || (data.has(legacy)
                && !data.has(legacy, PersistentDataType.BOOLEAN))) {
            throw new VitiLedger.Failure("viti-note-invalid");
        }

        String precise = data.get(
                exact, PersistentDataType.STRING);

        BigDecimal value = VitiAmount.positive(
                VitiAmount.stored(
                        precise == null
                                ? data.get(
                                amount, PersistentDataType.DOUBLE)
                                : precise));

        String id = data.get(
                serial, PersistentDataType.STRING);

        if (id == null) {
            if (data.has(exact) || data.has(legacy)) {
                throw new VitiLedger.Failure(
                        "viti-note-invalid");
            }

            return new Value(
                    null,
                    VitiAmount.positive(value.multiply(
                            BigDecimal.valueOf(item.getAmount()))),
                    true);
        }

        UUID parsed = UUID.fromString(id);

        if (!parsed.toString().equalsIgnoreCase(id)) {
            throw new VitiLedger.Failure("viti-note-invalid");
        }

        return new Value(
                parsed,
                value,
                Boolean.TRUE.equals(data.get(
                        legacy, PersistentDataType.BOOLEAN)));
    }

    public UUID serial(ItemStack item) {
        if (!isPaper(item)) return null;

        String value = item.getItemMeta()
                .getPersistentDataContainer().get(
                        serial, PersistentDataType.STRING);

        try {
            if (value == null) return null;

            UUID id = UUID.fromString(value);

            return id.toString().equalsIgnoreCase(value)
                    ? id : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public ItemStack create(
            UUID id, BigDecimal value, boolean imported) {
        VitiAmount.positive(value);

        ItemStack item = new ItemStack(Material.PAPER);
        var meta = item.getItemMeta();

        meta.displayName(
                Component.text(
                                "Uang Viti Fisik", NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false));

        meta.lore(List.of(
                Component.text(
                                "Nominal: "
                                        + VitiAmount.format(value)
                                        + " Viti",
                                NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text(
                                "Klik kanan di tangan utama untuk mencairkan.",
                                NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)));

        meta.setMaxStackSize(1);

        var data = meta.getPersistentDataContainer();

        data.set(
                amount, PersistentDataType.DOUBLE,
                value.doubleValue());

        data.set(
                exact, PersistentDataType.STRING,
                value.toPlainString());

        data.set(
                serial, PersistentDataType.STRING,
                id.toString());

        data.set(
                legacy, PersistentDataType.BOOLEAN,
                imported);

        item.setItemMeta(meta);
        return item;
    }

    public int emptySlot(Player player) {
        for (int slot = 0; slot < 36; slot++) {
            ItemStack item = player.getInventory().getItem(slot);

            if (item == null || item.getType().isAir()) {
                return slot;
            }
        }

        return -1;
    }

    public boolean contains(
            Player player,
            UUID id,
            BigDecimal expectedAmount) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (!isPaper(item)) continue;

            try {
                Value value = read(item);

                if (id.equals(value.serial())
                        && value.amount()
                        .compareTo(expectedAmount) == 0) {
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        return false;
    }

    public void remove(Player player, UUID id) {
        for (int slot = 0;
             slot < player.getInventory().getSize();
             slot++) {
            if (id.equals(serial(
                    player.getInventory().getItem(slot)))) {
                player.getInventory().setItem(slot, null);
            }
        }
    }
}