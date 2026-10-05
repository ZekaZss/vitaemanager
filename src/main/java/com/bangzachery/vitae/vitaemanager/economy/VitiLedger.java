package com.bangzachery.vitae.vitaemanager.economy;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record VitiLedger(Map<UUID, Account> accounts, Map<UUID, Note> notes,
                         Set<UUID> redeemed, Giveaway giveaway) {
    public VitiLedger {
        accounts = Map.copyOf(accounts);
        notes = Map.copyOf(notes);
        redeemed = Set.copyOf(redeemed);
        if (giveaway != null && !accounts.keySet().containsAll(giveaway.claimed())) {
            throw new IllegalArgumentException("Penerima giveaway tidak memiliki akun");
        }
        for (var entry : notes.entrySet()) {
            if (redeemed.contains(entry.getKey()) || !accounts.containsKey(entry.getValue().owner())) {
                throw new IllegalArgumentException("Note memiliki status atau pemilik tidak valid");
            }
        }
    }

    public VitiLedger(Map<UUID, Account> accounts, Map<UUID, Note> notes, Set<UUID> redeemed) {
        this(accounts, notes, redeemed, null);
    }

    public record Giveaway(UUID id, BigDecimal amount, Set<UUID> claimed, boolean active) {
        public Giveaway {
            if (id == null) throw new IllegalArgumentException("ID giveaway kosong");
            amount = VitiAmount.positive(amount);
            claimed = Set.copyOf(claimed);
        }
    }

    public VitiLedger startGiveaway(UUID id, BigDecimal amount) {
        if (giveaway != null && giveaway.id().equals(id)) {
            throw new IllegalArgumentException("Putaran baru harus memakai ID baru");
        }
        return new VitiLedger(accounts, notes, redeemed, new Giveaway(id, amount, Set.of(), true));
    }

    public VitiLedger stopGiveaway() {
        if (giveaway == null || !giveaway.active()) return this;
        return new VitiLedger(accounts, notes, redeemed,
                new Giveaway(giveaway.id(), giveaway.amount(), giveaway.claimed(), false));
    }

    public VitiLedger claimGiveaway(UUID player) {
        if (giveaway == null || !giveaway.active() || giveaway.claimed().contains(player)) return this;
        VitiLedger credited = add(player, giveaway.amount());
        var claimed = new HashSet<>(giveaway.claimed());
        claimed.add(player);
        return new VitiLedger(credited.accounts(), notes, redeemed,
                new Giveaway(giveaway.id(), giveaway.amount(), claimed, true));
    }

    public record Account(String name, BigDecimal balance, BigDecimal highest) {
        public Account {
            if (name == null || name.isBlank() || name.length() > 64) throw new IllegalArgumentException("Nama tidak valid");
            balance = VitiAmount.checked(balance);
            highest = VitiAmount.checked(highest).max(balance);
        }
        public Account withBalance(BigDecimal value) { return new Account(name, value, highest.max(value)); }
    }

    public record Note(UUID owner, BigDecimal amount, boolean pending) {
        public Note { amount = VitiAmount.positive(amount); if (owner == null) throw new IllegalArgumentException("Owner kosong"); }
    }

    public static final class Failure extends IllegalArgumentException {
        private final String key;
        public Failure(String key) { super(key); this.key = key; }
        public String key() { return key; }
    }

    public VitiLedger seed(UUID id, String name, BigDecimal fallback) {
        Account old = accounts.get(id);
        Account account = old == null ? new Account(name, fallback, fallback)
                : new Account(name, old.balance(), old.highest());
        var copy = new HashMap<>(accounts);
        copy.put(id, account);
        return new VitiLedger(copy, notes, redeemed, giveaway);
    }

    public BigDecimal balance(UUID id) { return account(id).balance(); }

    public VitiLedger set(UUID id, BigDecimal value) {
        var copy = new HashMap<>(accounts);
        copy.put(id, account(id).withBalance(VitiAmount.checked(value)));
        return new VitiLedger(copy, notes, redeemed, giveaway);
    }

    public VitiLedger add(UUID id, BigDecimal amount) {
        return set(id, balance(id).add(VitiAmount.positive(amount)));
    }

    public VitiLedger remove(UUID id, BigDecimal amount) {
        BigDecimal value = balance(id).subtract(VitiAmount.positive(amount));
        if (value.signum() < 0) throw new Failure("viti-insufficient");
        return set(id, value);
    }

    public VitiLedger transfer(UUID from, UUID to, BigDecimal amount) {
        if (from.equals(to)) throw new Failure("viti-invalid");
        return remove(from, amount).add(to, amount);
    }

    public VitiLedger withdraw(UUID owner, UUID serial, BigDecimal amount) {
        if (notes.containsKey(serial) || redeemed.contains(serial)) throw new Failure("viti-note-invalid");
        VitiLedger debited = remove(owner, amount);
        var copy = new HashMap<>(notes);
        copy.put(serial, new Note(owner, amount, true));
        return new VitiLedger(debited.accounts(), copy, redeemed, giveaway);
    }

    public VitiLedger redeem(UUID receiver, UUID serial, BigDecimal amount, boolean legacy) {
        VitiAmount.positive(amount);
        if (redeemed.contains(serial)) throw new Failure("viti-note-invalid");
        Note note = notes.get(serial);
        if ((note == null && !legacy) || (note != null && note.amount().compareTo(amount) != 0)) {
            throw new Failure("viti-note-invalid");
        }
        VitiLedger credited = add(receiver, amount);
        var remaining = new HashMap<>(notes);
        remaining.remove(serial);
        var receipts = new HashSet<>(redeemed);
        receipts.add(serial);
        return new VitiLedger(credited.accounts(), remaining, receipts, giveaway);
    }

    public VitiLedger delivered(Set<UUID> ids) {
        var copy = new HashMap<>(notes);
        for (UUID id : ids) {
            Note note = copy.get(id);
            if (note != null) copy.put(id, new Note(note.owner(), note.amount(), false));
        }
        return new VitiLedger(accounts, copy, redeemed, giveaway);
    }

    public List<Map.Entry<UUID, Account>> top() {
        return accounts.entrySet().stream().sorted((a, b) -> {
            int value = b.getValue().highest().compareTo(a.getValue().highest());
            return value != 0 ? value : a.getKey().compareTo(b.getKey());
        }).limit(10).toList();
    }

    private Account account(UUID id) {
        Account account = accounts.get(id);
        if (account == null) throw new Failure("viti-not-ready");
        return account;
    }
}