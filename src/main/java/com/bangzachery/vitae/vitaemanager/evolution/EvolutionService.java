package com.bangzachery.vitae.vitaemanager.evolution;

import com.bangzachery.vitae.vitaemanager.core.message.MessageService;
import com.bangzachery.vitae.vitaemanager.items.ItemRulesService;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.Level;

import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionAltar.*;
import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionStore.*;

public final class EvolutionService implements Listener, AutoCloseable {
    private final JavaPlugin plugin;
    private final ItemRulesService rules;
    private final MessageService messages;
    private final EvolutionStore store;
    private final NamespacedKey visualKey, receiptKey, transferKey, refundKey, deliveryKey;
    private final EvolutionBlast blast = new EvolutionBlast();
    private final EvolutionMusic music = new EvolutionMusic();
    private final EvolutionLayout layout;
    private final Map<UUID, Map<Integer, ItemDisplay>> depositVisuals = new HashMap<>();
    private final ExecutorService writer;

    private State current, persisted;

    private final Map<UUID, Menu> menus = new HashMap<>();
    private final Map<UUID, Run> runs = new HashMap<>();
    private final Map<UUID, Run> finalAudio = new HashMap<>();
    private final Map<String, UUID> leases = new HashMap<>();
    private final Set<UUID> recovering = new HashSet<>();
    private final Map<UUID, Map<String, UUID>> soundOwners = new HashMap<>();

    private BukkitTask task;
    private int pendingWrites;
    private long clock;
    private boolean reloading;
    private volatile boolean closed;
    private Predicate<Player> abilityGuard = player -> false;
    public void abilityGuard(Predicate<Player> guard) { abilityGuard = Objects.requireNonNull(guard); }

    public EvolutionService(
            JavaPlugin plugin,
            ItemRulesService rules,
            MessageService messages,
            Set<String> vanillaSounds
    ) throws IOException, InvalidConfigurationException {
        this.plugin = plugin;
        this.rules = rules;
        this.messages = messages;

        store = new EvolutionStore(
                plugin.getDataFolder().toPath().resolve("evolution/evolution.yml"),
                vanillaSounds
        );

        current = store.initialize();
        validateItems(current);
        persisted = current;

        visualKey = new NamespacedKey(plugin, "evolution_visual");
        receiptKey = new NamespacedKey(plugin, "evolution_receipt");
        transferKey = new NamespacedKey(plugin, "evolution_transfer");
        refundKey = new NamespacedKey(plugin, "evolution_refund");
        deliveryKey = new NamespacedKey(plugin, "evolution_delivery");
        writer = Executors.newSingleThreadExecutor(
                action -> new Thread(action, "Vitae-Evolution-Writer"));
        layout = new EvolutionLayout(plugin, this);
    }

    public void start() {
        require(task == null && !closed,
                "Evolution sudah berjalan atau ditutup.");

        Bukkit.getWorlds().forEach(world ->
                world.getEntities().forEach(this::removeStale));

        current.deposits().values().forEach(d -> leases.put(d.altar(), d.owner()));
        Bukkit.getPluginManager().registerEvents(layout, plugin);
        task = Bukkit.getScheduler().runTaskTimer(
                plugin, this::advance, 2, 2);

        if (!pending().isEmpty()) {
            plugin.getLogger().warning(
                    "Evolution: periksa ritual tertunda dengan /evo pending.");
        }
    }

    public Set<String> ids() {
        return current.altars().keySet();
    }

    public EvolutionAltar altar(String id) {
        var altar = current.altars().get(id);
        require(altar != null, "Altar tidak ditemukan: " + id);
        return altar;
    }

    public List<Ticket> pending() {
        return current.tickets().values().stream()
                .filter(ticket -> !ticket.phase().terminal()).toList();
    }

    public List<Deposit> pendingDeposits() { return List.copyOf(current.deposits().values()); }

    public void checkSetup(String id) {
        editable(); altar(id);
        require(!layout.inUse(id), "Editor susunan altar sedang dibuka; tutup dahulu.");
        require(!leases.containsKey(id) && current.deposits().values().stream().noneMatch(d -> d.altar().equals(id)),
                "Altar masih memiliki ritual atau setoran.");
    }
    void templateAllowed(ItemStack item) {
        require(usable(item) && item.getAmount() <= 64 && !rules.blocked(item), "Contoh bahan kosong, terlalu banyak, atau diblokir.");
        template(encode(item.asOne()));
    }
    public void layout(Player admin, String id) { layout.open(admin, id); }

    public void say(CommandSender sender, String text) {
        sender.sendMessage(messages.component("prefix")
                .append(Component.text(text, NamedTextColor.GRAY)));
    }

    public void create(String id, Block block, CommandSender sender) {
        editable();
        require(!ids().contains(id), "ID altar sudah ada.");
        require(block.getType() == Material.CRAFTING_TABLE,
                "Lihat crafting table untuk create.");

        EvolutionAltar draft = EvolutionAltar.draft(id, point(block));
        current.put(draft);

        submit(
                state -> state.put(draft),
                () -> say(sender, "Altar dibuat. Lanjutkan /evo help setup."),
                () -> saveFailed(sender)
        );
    }

    public void edit(
            String id,
            Consumer<ConfigurationSection> change,
            CommandSender sender
    ) {
        checkSetup(id);

        var yaml = store.document(current);
        change.accept(yaml.getConfigurationSection("altars." + id));

        EvolutionAltar next = store.parse(yaml).altars().get(id);
        validateItems(next);
        if (next.enabled()) ready(next);

        submit(
                state -> state.put(next),
                () -> say(sender, "Setup altar tersimpan."),
                () -> saveFailed(sender)
        );
    }

    public void delete(String id, CommandSender sender) {
        checkSetup(id);

        submit(
                state -> state.remove(id),
                () -> say(sender, "Altar dihapus."),
                () -> saveFailed(sender)
        );
    }

    public void reload(CommandSender sender) {
        editable();
        require(layout.empty() && menus.isEmpty() && runs.isEmpty() && recovering.isEmpty(),
                "Tutup GUI dan ritual sebelum reload.");

        reloading = true;
        pendingWrites++;

        writer.execute(() -> {
            try {
                State loaded = store.load();
                post(() -> {
                    try {
                        validateItems(loaded);
                    } catch (RuntimeException exception) {
                        pendingWrites--;
                        reloading = false;
                        failure(exception);
                        saveFailed(sender);
                        return;
                    }

                    writer.execute(() -> {
                        persisted = loaded;
                        post(() -> {
                            current = loaded;
                            pendingWrites--;
                            reloading = false;
                            clearDepositVisuals(); leases.clear();
                            current.deposits().values().forEach(d -> leases.put(d.altar(), d.owner()));
                            say(sender, "Evolution dimuat ulang.");
                        });
                    });
                });
            } catch (Exception exception) {
                failure(exception);
                post(() -> {
                    pendingWrites--;
                    reloading = false;
                    saveFailed(sender);
                });
            }
        });
    }

    public String sample(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        require(usable(item), "Pegang contoh item di tangan utama.");
        require(!rules.blocked(item), "Item diblokir aturan item.");
        return encode(item.asOne());
    }

    private void editable() {
        require(!closed && !reloading && pendingWrites == 0,
                "Masih menyimpan data. Tunggu sebentar lalu ulangi.");
    }

    private void submit(
            UnaryOperator<State> operation,
            Runnable success,
            Runnable failed
    ) {
        pendingWrites++;
        writer.execute(() -> {
            try {
                State next = operation.apply(persisted);
                store.save(next);
                persisted = next;

                post(() -> {
                    current = next;
                    pendingWrites--;
                    success.run();
                });
            } catch (Exception exception) {
                failure(exception);
                post(() -> {
                    pendingWrites--;
                    failed.run();
                });
            }
        });
    }

    private void post(Runnable callback) {
        if (closed) return;
        try {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!closed) callback.run();
            });
        } catch (IllegalPluginAccessException ignored) {
        }
    }

    private void failure(Exception exception) {
        plugin.getLogger().log(
                Level.WARNING,
                "Evolution: operasi gagal; periksa log dan /evo pending.",
                exception
        );
    }

    private void saveFailed(CommandSender sender) {
        say(sender,
                "Gagal menyimpan/memuat data; snapshot terakhir dipertahankan. Periksa console.");
    }

    private void validateItems(State state) {
        state.altars().values().forEach(EvolutionService::validateItems);
        state.deposits().values().forEach(deposit -> {
            EvolutionAltar altar = state.altars().get(deposit.altar()); validateDeposit(altar, deposit.items());
            if (deposit.transfer() != null) { decode(deposit.transfer().cursor()); validateDeposit(altar, deposit.transfer().next()); }
        });
        state.tickets().values().forEach(ticket -> {
            ticket.inputs().forEach(EvolutionService::decode);
            template(ticket.result());
        });
    }

    private static void validateItems(EvolutionAltar altar) {
        if (!altar.source().isEmpty()) template(altar.source());
        if (!altar.result().isEmpty()) template(altar.result());
        altar.ingredients().forEach(ingredient -> template(ingredient.item()));
    }

    private void ready(EvolutionAltar altar) {
        require(altar.complete(),
                "Lengkapi source, result, bahan, pedestal tiap slot dan target.");
        require(block(altar.table()).getType() == Material.CRAFTING_TABLE,
                "Crafting table altar hilang.");
        require(!rules.blocked(decode(altar.source()))
                        && !rules.blocked(decode(altar.result())),
                "Senjata diblokir aturan item.");

        for (Ingredient ingredient : altar.ingredients()) {
            require(block(ingredient.point()).getType().isSolid(),
                    "Pedestal harus berupa block solid.");
            require(!rules.blocked(decode(ingredient.item())),
                    "Bahan diblokir aturan item.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void interact(PlayerInteractEvent event) {
        if (closed || event.getHand() != EquipmentSlot.HAND
                || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getClickedBlock() == null) return;

        EvolutionAltar altar = current.altars().values().stream()
                .filter(a -> a.table().equals(point(event.getClickedBlock())))
                .findFirst().orElse(null);

        if (altar == null) return;
        event.setCancelled(true);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (closed || !event.getPlayer().isOnline()) return;
            try {
                open(event.getPlayer(), altar.id());
            } catch (RuntimeException exception) {
                say(event.getPlayer(), exception.getMessage());
            }
        });
    }

    private void available(Player player, EvolutionAltar altar) {
        require(!layout.inUse(altar.id()), "Admin sedang mengatur susunan altar ini.");
        require(!closed && !reloading && pendingWrites == 0 && !player.isDead(),
                "Evolution sedang menyimpan data; tunggu sebentar.");
        UUID owner = player.getUniqueId();
        require(!menus.containsKey(owner) && !runs.containsKey(owner) && !recovering.contains(owner),
                "Kamu sedang menjalankan ritual.");
        require(!leases.containsKey(altar.id()) || leases.get(altar.id()).equals(owner),
                "Altar sedang digunakan atau menyimpan bahan pemain lain.");
        Deposit deposit = current.deposits().get(owner);
        require(deposit == null || deposit.altar().equals(altar.id()), "Selesaikan setoran di altar lain, atau /evo cancel dahulu.");
        require(deposit == null || !hasReceipt(player, refundKey, deposit.id()), "Setoran telah direfund; admin perlu menutup catatannya.");
        require(deposit == null || deposit.transfer() == null, "Transaksi tertunda; minta admin memeriksa /evo pending.");
        require(menus.size() + runs.size() < 32, "Terlalu banyak ritual berjalan.");
        Ticket ticket = current.tickets().get(owner);
        require(ticket == null || ticket.phase().terminal(), "Ritual sebelumnya perlu diperiksa admin.");
    }

    private void open(Player player, String id) {
        require(!abilityGuard.test(player), "Kamu tidak dapat mengaktifkan ritual selama penguncian sihir.");
        EvolutionAltar altar = altar(id);
        require(player.hasPermission("vitae.evolution"), "Kamu tidak memiliki izin ritual.");
        require(altar.enabled() && altar.targets().contains(player.getUniqueId()), "Altar belum aktif atau belum ditujukan untukmu.");
        require(survival(player) && !usable(player.getItemOnCursor()), "Gunakan survival/adventure dan kosongkan cursor.");
        available(player, altar); ready(altar);
        require(near(player, altar.table(), 8), "Kamu terlalu jauh dari altar.");
        stopFinal(player.getUniqueId());
        InventoryView view = player.openWorkbench(block(altar.table()).getLocation(), true);
        require(view != null && view.getTopInventory() instanceof CraftingInventory, "Crafting table tidak dapat dibuka.");
        Menu menu = new Menu(player.getUniqueId(), altar, (CraftingInventory) view.getTopInventory());
        menus.put(menu.owner, menu); leases.put(id, menu.owner);
        try {
            render(menu);
            say(player, "Ambil bahan ke cursor, lalu taruh/drag ke slot bahan yang cocok. Senjata terakhir di tengah. Hasil kanan hanya preview.");
            say(player, "Setoran tersimpan saat GUI ditutup. /evo cancel untuk refund bahan.");
            Deposit d = current.deposits().get(menu.owner);
            if (d != null && complete(menu.altar, d.items())) begin(player, menu);
        } catch (RuntimeException exception) { removeMenu(menu); player.closeInventory(); throw exception; }
    }
    private Menu menu(InventoryView view) {
        Menu menu = menus.get(view.getPlayer().getUniqueId());
        return menu != null && view.getTopInventory() == menu.inventory ? menu : null;
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void craft(PrepareItemCraftEvent event) {
        Menu menu = menu(event.getView());
        if (menu != null && !menu.rendering) menu.inventory.setResult(decode(menu.altar.result()).asOne());
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void recipeBook(com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent event) {
        // Autofill bisa mengembalikan matrix preview ke storage tanpa InventoryClickEvent.
        if (menu(event.getPlayer().getOpenInventory()) != null) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void click(InventoryClickEvent event) {
        Menu menu = menu(event.getView()); if (menu == null) return;
        boolean top = event.getRawSlot() >= 0 && event.getRawSlot() < 10;
        boolean simple = event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT;
        boolean denied = event.isCancelled();
        // Shift, number-key, offhand dan collect tidak boleh mengambil preview.
        if (!top && simple && !menu.pending && !denied && !(event instanceof InventoryCreativeEvent)) return;
        event.setCancelled(true);
        if (denied || event instanceof InventoryCreativeEvent || menu.pending || !simple || !top
                || !(event.getWhoClicked() instanceof Player player)) return;
        int index = EvolutionGrid.index(menu.altar, event.getRawSlot()); if (index < 0) return;
        ItemStack cursor = event.getCursor();
        if (!usable(cursor)) { describe(player, menu, index); return; }
        if (rules.blocked(cursor)) return;
        try {
            int count = amountFor(menu, index, cursor, event.getClick() == ClickType.RIGHT);
            require(count > 0, "Slot ini sudah selesai atau tidak dipakai.");
            acceptLater(player, menu, cursor.clone(), Map.of(index, count));
        } catch (RuntimeException exception) { say(player, exception.getMessage()); }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void drag(InventoryDragEvent event) {
        Menu menu = menu(event.getView()); if (menu == null) return;
        boolean top = event.getRawSlots().stream().anyMatch(slot -> slot < 10);
        if (!top && !menu.pending) return;
        boolean denied = event.isCancelled(); event.setCancelled(true);
        if (denied || menu.pending || !top || !(event.getWhoClicked() instanceof Player player)) return;
        try {
            require(event.getRawSlots().stream().allMatch(slot -> slot >= 1 && slot < 10), "Drag bahan di slot crafting saja.");
            ItemStack cursor = event.getOldCursor(); require(usable(cursor) && !rules.blocked(cursor), "Bahan tidak tersedia.");
            Map<Integer, Integer> additions = new HashMap<>();
            for (var entry : event.getNewItems().entrySet()) {
                int index = EvolutionGrid.index(menu.altar, entry.getKey()); ItemStack old = menu.inventory.getItem(entry.getKey());
                int requested = entry.getValue().getAmount() - (usable(old) ? old.getAmount() : 0);
                int count = Math.min(requested, amountFor(menu, index, cursor, false));
                require(count > 0, "Drag hanya ke slot bahan yang belum lengkap."); additions.put(index, count);
            }
            require(additions.values().stream().mapToInt(Integer::intValue).sum() <= cursor.getAmount(), "Bahan pada cursor tidak cukup.");
            acceptLater(player, menu, cursor.clone(), additions);
        } catch (RuntimeException exception) { say(player, exception.getMessage()); }
    }
    private void acceptLater(Player player, Menu menu, ItemStack cursor, Map<Integer, Integer> additions) {
        menu.pending = true;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (closed || menus.get(menu.owner) != menu || !player.isOnline()
                    || !cursor.equals(player.getItemOnCursor())) { menu.pending = false; return; }
            try { deposit(player, menu, cursor, additions); }
            catch (RuntimeException exception) { menu.pending = false; say(player, exception.getMessage()); }
        });
    }
    @EventHandler(priority = EventPriority.LOWEST)
    public void closeMenu(InventoryCloseEvent event) {
        Menu menu = menu(event.getView()); if (menu != null) removeMenu(menu);
    }
    private void describe(Player player, Menu menu, int index) {
        if (index == 8) say(player, "Senjata wajib ditaruh terakhir, setelah semua slot bahan hijau.");
        else if (index < menu.altar.ingredients().size()) say(player, "Bahan " + (index + 1) + ": "
                + amounts(items(menu.owner)).getOrDefault(index, 0) + "/" + menu.altar.ingredients().get(index).count());
    }
    private Map<Integer, String> items(UUID owner) {
        Deposit d = current.deposits().get(owner); return d == null ? Map.of() : d.items();
    }
    private static Map<Integer, Integer> amounts(Map<Integer, String> items) {
        Map<Integer, Integer> amounts = new HashMap<>();
        items.forEach((slot, bytes) -> amounts.put(slot, decode(bytes).getAmount())); return amounts;
    }
    private static boolean complete(EvolutionAltar altar, Map<Integer, String> items) {
        Map<Integer, Integer> amounts = amounts(items);
        return EvolutionGrid.materialsComplete(altar, amounts) && amounts.getOrDefault(8, 0) == 1;
    }
    private void validateDeposit(EvolutionAltar altar, Map<Integer, String> items) {
        for (var e : items.entrySet()) {
            int index = e.getKey(); ItemStack actual = decode(e.getValue());
            require(index == 8 || index < altar.ingredients().size(), "Slot setoran tidak sesuai altar.");
            int max = index == 8 ? 1 : altar.ingredients().get(index).count();
            require(actual.getAmount() >= 1 && actual.getAmount() <= max, "Jumlah setoran melebihi kebutuhan.");
            require(matches(actual, index == 8 ? altar.source() : altar.ingredients().get(index).item(), index == 8),
                    "Item setoran tidak sesuai konfigurasi altar.");
        }
        require(!items.containsKey(8) || complete(altar, items), "Senjata disetor sebelum bahan lengkap.");
    }
    private int amountFor(Menu menu, int index, ItemStack cursor, boolean single) {
        require(index >= 0 && (index == 8 || index < menu.altar.ingredients().size()), "Slot ini tidak dipakai.");
        Map<Integer, Integer> amounts = amounts(items(menu.owner));
        if (index == 8) require(EvolutionGrid.materialsComplete(menu.altar, amounts), "Isi seluruh bahan di pinggir sampai hijau, baru senjata di tengah.");
        require(matches(cursor, index == 8 ? menu.altar.source() : menu.altar.ingredients().get(index).item(), index == 8), "Item tidak cocok untuk slot ini.");
        return EvolutionGrid.accepted(index == 8 ? 1 : menu.altar.ingredients().get(index).count(),
                amounts.getOrDefault(index, 0), cursor.getAmount(), single);
    }
    private void deposit(Player player, Menu menu, ItemStack cursor, Map<Integer, Integer> additions) {
        require(!abilityGuard.test(player), "Kamu tidak dapat memakai item selama penguncian sihir.");
        ready(menu.altar);
        require(near(player, menu.altar.table(), 8) && survival(player) && !player.isDead(), "Pemain meninggalkan altar.");
        Deposit old = current.deposits().get(menu.owner); require(old == null || old.transfer() == null, "Masih menyimpan setoran.");
        Map<Integer, String> next = new HashMap<>(items(menu.owner)); int used = 0;
        for (var e : additions.entrySet()) {
            require(e.getValue() > 0 && e.getValue() <= amountFor(menu, e.getKey(), cursor, false), "Setoran berubah.");
            int before = next.containsKey(e.getKey()) ? decode(next.get(e.getKey())).getAmount() : 0;
            next.put(e.getKey(), encode(cursor.asQuantity(before + e.getValue()))); used += e.getValue();
        }
        require(used > 0 && used <= cursor.getAmount(), "Bahan tidak cukup.");
        UUID id = old == null ? UUID.randomUUID() : old.id();
        Transfer transfer = new Transfer(UUID.randomUUID(), encode(cursor), next);
        Deposit prepared = old == null ? new Deposit(id, menu.owner, menu.altar.id(), menu.altar.table(), Map.of(), transfer) : old.pending(transfer);
        int consumed = used;
        submit(state -> state.deposit(prepared), () -> {
            if (!player.isOnline() || player.isDead() || menus.get(menu.owner) != menu
                    || !cursor.equals(player.getItemOnCursor())) { settleDeposit(prepared, false, menu, player); return; }
            try {
                // Cursor tidak disimpan di player.dat. Sisa stack dipindah ke storage sebelum save.
                player.setItemOnCursor(null);
                if (cursor.getAmount() > consumed) give(player, List.of(cursor.asQuantity(cursor.getAmount() - consumed)), transfer.id());
                player.getPersistentDataContainer().set(transferKey, PersistentDataType.STRING, transfer.id().toString()); player.saveData();
            } catch (RuntimeException exception) {
                menu.pending = false; player.closeInventory(); pendingMessage(player); failure(exception); return;
            }
            settleDeposit(prepared, true, menu, player);
        }, () -> { menu.pending = false; releaseIfEmpty(menu); saveFailed(player); });
    }
    private void settleDeposit(Deposit prepared, boolean consumed, Menu menu, Player player) {
        submit(state -> state.settle(prepared.owner(), prepared.id(), prepared.transfer().id(), consumed), () -> {
            menu.pending = false; Deposit d = current.deposits().get(prepared.owner());
            if (d != null) {
                paintDeposit(d);
                if (consumed) for (int index : EvolutionGrid.newlyComplete(menu.altar, amounts(prepared.items()), amounts(d.items()))) {
                    Point point = menu.altar.ingredients().get(index).point(); World world = Bukkit.getWorld(point.world());
                    if (world != null && world.isChunkLoaded(point.x() >> 4, point.z() >> 4)) world.strikeLightningEffect(location(point));
                }
            } else releaseIfEmpty(menu);
            if (menus.get(menu.owner) == menu && player.isOnline()) render(menu);
            if (d != null && complete(menu.altar, d.items()) && player.isOnline() && !player.isDead()
                    && survival(player) && near(player, menu.altar.table(), 8)) begin(player, menu);
        }, () -> { menu.pending = false; if (player.isOnline()) player.closeInventory(); pendingMessage(player); });
    }
    private void releaseIfEmpty(Menu menu) {
        if (!current.deposits().containsKey(menu.owner) && !menus.containsKey(menu.owner) && !runs.containsKey(menu.owner))
            releaseDeposit(menu.owner, menu.altar.id());
    }
    private void render(Menu menu) {
        menu.rendering = true;
        try {
            menu.inventory.clear(); Map<Integer, Integer> amounts = amounts(items(menu.owner));
            for (int i = 0; i < 8; i++) menu.inventory.setItem(EvolutionGrid.rawSlot(i), icon(Material.GRAY_STAINED_GLASS_PANE, "Tidak dipakai", false));
            for (int i = 0; i < menu.altar.ingredients().size(); i++) {
                Ingredient ingredient = menu.altar.ingredients().get(i);
                ItemStack item = amounts.getOrDefault(i, 0) == ingredient.count()
                        ? icon(Material.LIME_STAINED_GLASS_PANE, "Slot " + ingredient.slot() + " selesai", true)
                        : decode(ingredient.item()).asOne();
                menu.inventory.setItem(ingredient.slot(), item);
            }
            menu.inventory.setItem(5, decode(menu.altar.source()).asOne()); menu.inventory.setResult(decode(menu.altar.result()).asOne());
            Deposit d = current.deposits().get(menu.owner); if (d != null) paintDeposit(d);
        } finally { menu.rendering = false; }
    }
    private void paintDeposit(Deposit deposit) {
        EvolutionAltar altar = current.altars().get(deposit.altar());
        if (altar == null || Bukkit.getWorld(altar.table().world()) == null) return;
        var visuals = depositVisuals.computeIfAbsent(deposit.owner(), owner -> new HashMap<>());
        deposit.items().forEach((index, bytes) -> {
            ItemDisplay old = visuals.get(index); if (old != null && old.isValid()) return;
            Point point = index == 8 ? altar.table() : altar.ingredients().get(index).point(); World world = Bukkit.getWorld(point.world());
            if (world != null && world.isChunkLoaded(point.x() >> 4, point.z() >> 4))
                visuals.put(index, display(point, decode(bytes), altar.scale() * (index == 8 ? 1 : .65)));
        });
    }
    private void begin(Player player, Menu menu) {
        require(!abilityGuard.test(player), "Kamu tidak dapat mengaktifkan ritual selama penguncian sihir.");
        Deposit d = current.deposits().get(menu.owner);
        require(d != null && d.transfer() == null && complete(menu.altar, d.items()), "Setoran belum lengkap."); ready(menu.altar);
        Ticket ticket = new Ticket(UUID.randomUUID(), menu.owner, menu.altar.id(), menu.altar.table(), Phase.PREPARED,
                new TreeMap<>(d.items()).values().stream().toList(), menu.altar.result());
        menu.pending = true;
        submit(state -> state.beginDeposit(ticket, d.id()), () -> {
            Map<Integer, ItemDisplay> visuals = depositVisuals.remove(menu.owner); if (visuals == null) visuals = new HashMap<>();
            Run run = new Run(menu.owner, menu.altar, false, visuals, ticket); run.consumed = true; runs.put(run.owner, run);
            removeMenu(menu); leases.put(menu.altar.id(), menu.owner);
            if (player.isOnline()) player.closeInventory();
            try {
                require(player.isOnline() && !player.isDead() && survival(player) && near(player, menu.altar.table(), 8), "Pemilik meninggalkan altar.");
                for (var e : d.items().entrySet()) if (!run.visuals.containsKey(e.getKey()) || !run.visuals.get(e.getKey()).isValid()) {
                    Point point = e.getKey() == 8 ? menu.altar.table() : menu.altar.ingredients().get(e.getKey()).point();
                    run.visuals.put(e.getKey(), display(point, decode(e.getValue()), menu.altar.scale() * (e.getKey() == 8 ? 1 : .65)));
                }
                checkpoint(run, Phase.ACTIVE, () -> {
                    if (live(run)) { run.active = true; say(player, "Ritual dimulai. Saksikan evolusinya!"); }
                }, () -> abort(run, player, null, "Gagal menyimpan ritual."));
            } catch (RuntimeException e) { abort(run, player, null, e.getMessage()); }
        }, () -> { menu.pending = false; saveFailed(player); });
    }
    private void give(Player player, List<ItemStack> items, UUID receipt) {
        for (ItemStack item : items) player.getInventory().addItem(item.clone()).values()
                .forEach(left -> drop(player.getLocation(), left, player.getUniqueId(), receipt));
    }
    private void releaseDeposit(UUID owner, String altar) {
        var visuals = depositVisuals.remove(owner); if (visuals != null) removeVisuals(visuals); leases.remove(altar, owner);
    }
    private void clearDepositVisuals() { depositVisuals.values().forEach(EvolutionService::removeVisuals); depositVisuals.clear(); }

    private void checkpoint(
            Run run, Phase phase, Runnable success, Runnable failed
    ) {
        submit(
                state -> state.phase(run.owner, run.id, phase),
                success, failed
        );
    }

    public void preview(Player player, String id) {
        require(!abilityGuard.test(player), "Kamu tidak dapat mengaktifkan ritual selama penguncian sihir.");
        EvolutionAltar altar = altar(id);
        available(player, altar);
        ready(altar);
        require(!current.deposits().containsKey(player.getUniqueId()), "Batalkan setoran sebelum preview.");
        require(near(player, altar.table(), 10),
                "Datang ke altar sebelum preview.");
        stopFinal(player.getUniqueId());

        Map<Integer, ItemDisplay> visuals = new HashMap<>();
        try {
            for (int i = 0; i < altar.ingredients().size(); i++) {
                var ingredient = altar.ingredients().get(i);
                visuals.put(i, display(
                        ingredient.point(), decode(ingredient.item()),
                        altar.scale() * .65));
            }
            visuals.put(8, display(
                    altar.table(), decode(altar.source()), altar.scale()));

            Run run = new Run(
                    player.getUniqueId(), altar, true, visuals, null);
            run.active = true;
            runs.put(run.owner, run);
            leases.put(id, run.owner);
            say(player,
                    "Preview: tidak mengonsumsi item, membuat hasil, atau memberikan damage.");
        } catch (RuntimeException exception) {
            removeVisuals(visuals);
            throw exception;
        }
    }

    public void control(Player player, String action) {
        Menu menu = menus.get(player.getUniqueId());
        if (action.equals("cancel") && current.deposits().containsKey(player.getUniqueId())) {
            require(menu == null || !menu.pending, "Tunggu setoran selesai disimpan.");
            if (menu != null) player.closeInventory(); recover(player.getUniqueId(), "refund", player); return;
        }
        if (action.equals("cancel") && menu != null) {
            require(!menu.pending, "Tunggu setoran selesai disimpan.");
            player.closeInventory(); say(player, "Persiapan dibatalkan."); return;
        }

        Run run = runs.get(player.getUniqueId());
        require(run != null, "Pemain tidak sedang menjalankan ritual.");

        switch (action) {
            case "cancel" -> abort(
                    run, player, null, "Ritual dibatalkan.");
            case "pause" -> {
                require(run.active && !run.paused && !run.finishing && !run.exploded,
                        "Ritual belum dapat dijeda.");
                run.paused = true;
                stopAudio(run); music.stop(run.id);
                player.clearTitle();
                say(player, "Ritual dijeda.");
            }
            case "resume" -> {
                require(run.active && run.paused,
                        "Ritual tidak sedang dijeda.");
                run.paused = false;
                run.stage = null;
                if (!run.altar.music().sound().isEmpty()) run.elapsed = 0;
                run.musicStarted = false;
                say(player, run.altar.music().sound().isEmpty() ? "Ritual dilanjutkan."
                        : "Animasi diulang dari awal agar musik tetap sinkron.");
            }
            default -> throw new IllegalArgumentException(
                    "Kontrol tidak dikenal.");
        }
    }

    private void advance() {
        if (closed) return;
        clock += 2;

        for (Menu menu : List.copyOf(menus.values())) {
            Player player = Bukkit.getPlayer(menu.owner);
            try {
                require(player != null && !player.isDead()
                                && near(player, menu.altar.table(), 8),
                        "GUI ditutup karena meninggalkan altar.");
            } catch (RuntimeException exception) {
                if (player != null) player.closeInventory();
                removeMenu(menu);
            }
        }

        for (Run run : List.copyOf(runs.values())) {
            Player player = Bukkit.getPlayer(run.owner);
            try {
                if (run.exploded) { advanceReveal(run, player); continue; }
                require(player != null && !player.isDead()
                                && near(player, run.altar.table(), 12),
                        "Pemain meninggalkan altar.");
                require(block(run.altar.table()).getType()
                                == Material.CRAFTING_TABLE,
                        "Crafting table hilang.");
                require(run.altar.ingredients().stream()
                                .allMatch(i -> block(i.point()).getType().isSolid()),
                        "Pedestal hilang.");
                require(run.visuals.values().stream().allMatch(Entity::isValid),
                        "Visual ritual terputus.");

                if (!run.active || run.paused || run.finishing) continue;

                // Checkpoint hasil disiapkan satu detik sebelum ledakan, tanpa memperpanjang timeline normal.
                if (!run.preview && !run.commitRequested && run.elapsed >= run.altar.ticks() - 20) {
                    run.commitRequested = true;
                    checkpoint(run, Phase.COMMITTING, () -> run.commitReady = true,
                            () -> abort(run, player, null, "Hasil gagal disiapkan."));
                }
                if (!run.musicStarted) { music.start(run.id, run.owner, run.altar); run.musicStarted = true; }
                music.trim(run.id);
                if (run.elapsed >= run.altar.ticks()) {
                    if (run.preview || run.commitReady) finish(run, player);
                    continue;
                }

                var frame = EvolutionAnimation.frame(
                        run.altar.preset(),
                        run.elapsed / (double) run.altar.ticks(),
                        run.elapsed,
                        run.origins
                );

                if (frame.stage() != run.stage) {
                    run.stage = frame.stage();
                    cue(run, run.stage);
                }

                animate(run, frame);
                run.elapsed += 2;
            } catch (RuntimeException exception) {
                abort(run, player, null, exception.getMessage());
                failure(exception);
            }
        }

        if (clock % 40 == 0) current.deposits().values().forEach(this::paintDeposit);
        for (Run run : List.copyOf(finalAudio.values())) {
            if (clock >= run.audioUntil) stopFinal(run.owner);
        }
    }

    private void animate(Run run, EvolutionAnimation.Frame frame) {
        Location base = location(run.altar.table());
        Location weapon = offset(base, frame.weapon());
        Particle particle = particle(run.altar.preset());

        move(run.visuals.get(8), weapon, frame.rotation(),
                frame.tilt(), run.altar.scale());

        for (int i = 0; i < frame.ingredients().size(); i++) {
            Location position = offset(base, frame.ingredients().get(i));
            move(run.visuals.get(i), position,
                    -frame.rotation(), 0, run.altar.scale() * .65);

            if (run.elapsed >= run.altar.ticks() * .15) {
                position.getWorld().spawnParticle(
                        particle, position, 1, .03, .03, .03, .01);
            }
        }

        weapon.getWorld().spawnParticle(
                particle, weapon, Math.max(1, run.altar.density() / 8),
                .12, .15, .12, .01);

        if (run.altar.effects().lightning() && run.elapsed > 0
                && run.elapsed % run.altar.effects().interval() < 2) {
            double angle = run.elapsed * .71;
            Location strike = altarCenter(run.altar.table()).add(Math.cos(angle) * run.altar.effects().radius(),
                    0, Math.sin(angle) * run.altar.effects().radius());
            strike.getWorld().strikeLightningEffect(strike);
        }
        if (run.altar.preset() == Preset.INFERNO) {
            for (int i = 0; i < 3; i++) {
                double angle = frame.rotation() + i * Math.PI * 2 / 3;
                base.getWorld().spawnParticle(
                        Particle.FLAME,
                        base.clone().add(
                                Math.cos(angle) * .7,
                                frame.weapon().y() * i / 3,
                                Math.sin(angle) * .7),
                        1, 0, .05, 0, 0
                );
            }
        } else if (run.altar.preset() == Preset.STORMFORGE
                && run.elapsed % 12 == 0) {
            weapon.getWorld().spawnParticle(
                    Particle.ELECTRIC_SPARK, weapon,
                    run.altar.density() / 2, .8, .15, .8, .1);
        } else if (run.altar.preset() == Preset.VOID) {
            base.getWorld().spawnParticle(
                    Particle.PORTAL, base,
                    Math.max(1, run.altar.density() / 8),
                    .6, .05, .6, .05);
        } else if (run.altar.preset() == Preset.ASTRAL) {
            for (int i = 0; i < run.altar.density(); i++) {
                double a = i * Math.PI * 2 / run.altar.density() + frame.rotation();
                double r = 1.4 * (1 - Math.max(0, (run.elapsed / (double) run.altar.ticks() - .7) / .3));
                weapon.getWorld().spawnParticle(Particle.END_ROD,
                        weapon.clone().add(Math.cos(a) * r, Math.sin(a) * r * .7, Math.sin(a) * r * .6), 1, 0, 0, 0, 0);
            }
        } else if (run.altar.preset() == Preset.FROST) {
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3 + frame.rotation();
                for (int arm = 1; arm <= 3; arm++) base.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        base.clone().add(Math.cos(a) * arm * .6, .03, Math.sin(a) * arm * .6), 1, 0, 0, 0, 0);
            }
        } else if (run.altar.preset() == Preset.PHOENIX) {
            for (int wing : new int[]{-1, 1}) for (int i = 1; i <= 8; i++) {
                double flap = Math.sin(run.elapsed * .12) * .5;
                weapon.getWorld().spawnParticle(Particle.FLAME,
                        weapon.clone().add(wing * i * .22, Math.sin(i * Math.PI / 8) * (1 + flap), -.15 * i), 1, .02, .02, .02, 0);
            }
        }
    }

    private void finish(Run run, Player player) {
        if (run.altar.aftermath().enabled()) { startReveal(run, player); return; }
        if (run.preview) {
            completeVisual(run, player);
            return;
        }

        require(!rules.blocked(decode(run.ticket.result())),
                "Senjata hasil diblokir aturan item.");
        run.finishing = true;

        require(run.commitReady, "Hasil belum siap.");
        if (!live(run)) return;
        try {
                require(player.isOnline() && !player.isDead()
                                && near(player, run.altar.table(), 12),
                        "Pemilik tidak tersedia.");

                // Lepas run sebelum damage agar kematian tidak me-refund input.
                removeRun(run);
                deliver(player, run.ticket, location(run.altar.table()));

                checkpoint(
                        run,
                        Phase.DELIVERED,
                        () -> say(player,
                                "Evolusi selesai. Senjata berada di atas altar."),
                        () -> pendingMessage(player)
                );

                completeVisual(run, player);

                if ((run.altar.lethal() || run.altar.damage() > 0) && run.altar.damageRadius() > 0) {
                    Location center = altarCenter(run.altar.table());
                    for (Player nearby : List.copyOf(center.getWorld().getPlayers()))
                        if (nearby.getLocation().distanceSquared(center) <= run.altar.damageRadius() * run.altar.damageRadius())
                            blast.hit(nearby, run.altar.lethal(), run.altar.damage());
                }
            } catch (RuntimeException exception) {
                if (live(run)) {
                    abort(run, player, null, exception.getMessage());
                } else {
                    pendingMessage(player);
                    failure(exception);
                }
            }
    }

    private void startReveal(Run run, Player player) {
        if (run.finishing) return;
        run.finishing = true;
        if (run.preview) { explodeReveal(run); return; }
        require(run.commitReady, "Hasil belum siap.");
        require(!rules.blocked(decode(run.ticket.result())), "Senjata hasil diblokir aturan item.");
        // This checkpoint makes death/disconnect during the blast preserve the result, never refund the old weapon.
        run.uncertain = true;
        checkpoint(run, Phase.REVEALING, () -> {
            run.uncertain = false;
            if (live(run)) try { explodeReveal(run); }
            catch (RuntimeException error) { abort(run, Bukkit.getPlayer(run.owner), null, "Adegan hasil terputus."); failure(error); }
        }, () -> { run.uncertain = false; abort(run, Bukkit.getPlayer(run.owner), null, "Adegan hasil gagal disimpan."); });
    }
    private void explodeReveal(Run run) {
        run.exploded = true; run.finishing = false; run.revealAge = 0;
        music.stop(run.id); stopAudio(run);
        Player owner = Bukkit.getPlayer(run.owner); if (owner != null) owner.clearTitle();
        for (int key : List.copyOf(run.visuals.keySet())) if (key != 8) removeVisual(run.visuals, key);
        run.visuals.get(8).setItemStack(new ItemStack(Material.AIR));
        explosionParticles(run); cueSound(run, run.altar.cue(Stage.FINISH));
        if (!run.preview && (run.altar.lethal() || run.altar.damage() > 0) && run.altar.damageRadius() > 0) {
            Location center = altarCenter(run.altar.table());
            for (Player nearby : List.copyOf(center.getWorld().getPlayers()))
                if (nearby.getLocation().distanceSquared(center) <= run.altar.damageRadius() * run.altar.damageRadius())
                    blast.hit(nearby, run.altar.lethal(), run.altar.damage());
        }
    }
    private void advanceReveal(Run run, Player player) {
        if (run.finishing) return;
        run.revealAge += 2;
        ItemDisplay weapon = run.visuals.get(8);
        require(weapon != null && weapon.isValid(), "Visual hasil terputus.");
        if (run.revealAge >= run.altar.aftermath().quietTicks()) {
            if (!run.revealed) {
                weapon.setItemStack(decode(run.altar.result())); run.revealed = true;
                Cue cue = run.altar.cue(Stage.FINISH);
                if (player != null && player.isOnline() && !player.isDead()) player.showTitle(Title.title(
                        Component.text(cue.title(), NamedTextColor.GOLD), Component.text(cue.subtitle(), NamedTextColor.GRAY),
                        Title.Times.times(Duration.ofMillis(250), Duration.ofMillis((run.altar.aftermath().ticks() - run.altar.aftermath().quietTicks()) * 50L), Duration.ofMillis(250))));
            }
            Location at = location(run.altar.table()).add(0, 1.1 + Math.sin(run.revealAge * .06) * .12, 0);
            move(weapon, at, run.revealAge * .035, 0, run.altar.scale());
            if (run.revealAge % 6 == 0) at.getWorld().spawnParticle(particle(run.altar.preset()), at, 3, .2, .2, .2, .01);
        }
        if (run.revealAge < run.altar.aftermath().ticks()) return;
        run.finishing = true;
        if (run.preview) { removeRun(run); if (player != null) say(player, "Preview selesai."); return; }
        if (player == null || !player.isOnline() || player.isDead()) { abort(run, player, null, "Hasil menunggu pemulihan admin setelah pemain kembali."); return; }
        require(!rules.blocked(decode(run.ticket.result())), "Senjata hasil diblokir aturan item.");
        removeRun(run);
        try {
            deliver(player, run.ticket, location(run.altar.table()));
            checkpoint(run, Phase.DELIVERED, () -> say(player, "Evolusi selesai. Senjata berada di atas altar."), () -> pendingMessage(player));
        } catch (RuntimeException error) { pendingMessage(player); failure(error); }
    }
    private void explosionParticles(Run run) {
        Location center = location(run.altar.table()).add(0, 2.4, 0);
        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
        center.getWorld().spawnParticle(particle(run.altar.preset()), center, run.altar.density() * 2, .7, .7, .7, .15);
        if (run.altar.effects().shockwave()) for (int i = 0; i < run.altar.density() * 2; i++) {
            double angle = Math.PI * 2 * i / (run.altar.density() * 2);
            center.getWorld().spawnParticle(Particle.GUST, altarCenter(run.altar.table()).add(Math.cos(angle) * 2.5, .15, Math.sin(angle) * 2.5), 1, 0, 0, 0, 0);
        }
    }
    private void completeVisual(Run run, Player player) {
        removeRun(run);

        Location center = location(run.altar.table()).add(0, 2.4, 0);
        center.getWorld().spawnParticle(
                Particle.EXPLOSION_EMITTER, center, 1);
        center.getWorld().spawnParticle(
                particle(run.altar.preset()), center,
                run.altar.density() * 2, .7, .7, .7, .15);

        if (run.altar.effects().shockwave()) {
            for (int i = 0; i < run.altar.density() * 2; i++) {
                double angle = Math.PI * 2 * i / (run.altar.density() * 2);
                center.getWorld().spawnParticle(Particle.GUST,
                        altarCenter(run.altar.table()).add(Math.cos(angle) * 2.5, .15, Math.sin(angle) * 2.5), 1, 0, 0, 0, 0);
            }
        }
        cue(run, Stage.FINISH);
        run.audioUntil = clock + 2400;
        finalAudio.put(run.owner, run);

        if (run.preview) say(player, "Preview selesai.");
    }

    private void abort(
            Run run, Player player, PlayerDeathEvent death, String reason
    ) {
        if (!live(run)) return;
        removeRun(run);

        if (player != null) {
            say(player, reason == null ? "Ritual dibatalkan." : reason);
        }
        if (run.preview) return;
        if (run.exploded) {
            if (player != null) pendingMessage(player);
            return;
        }
        if (run.uncertain) {
            if (player != null) pendingMessage(player);
            return;
        }

        boolean returned = false;
        try {
            if (run.consumed) {
                require(player != null, "Pemain offline.");
                refund(player, run.ticket, death);
                returned = true;
            }

            Phase terminal = returned ? Phase.RETURNED : Phase.DISCARDED;

            submit(state -> {
                Ticket old = state.tickets().get(run.owner);
                return old == null || !old.id().equals(run.id)
                        || old.phase().terminal()
                        ? state : state.phase(run.owner, run.id, terminal);
            }, () -> {
                if (player != null) {
                    say(player, "Pembatalan tersimpan; "
                            + (run.consumed
                            ? "input dikembalikan." : "item belum dikonsumsi."));
                }
            }, () -> {
                if (player != null) pendingMessage(player);
            });
        } catch (RuntimeException exception) {
            if (player != null) pendingMessage(player);
            failure(exception);
        }
    }

    private void refund(
            Player player, Ticket ticket, PlayerDeathEvent death
    ) {
        require(!hasReceipt(player, deliveryKey, ticket.id()), "Hasil sudah diberikan; refund ditolak.");
        if (hasReceipt(player, refundKey, ticket.id())) return;
        List<ItemStack> inputs = ticket.inputs().stream()
                .map(EvolutionService::decode).toList();

        if (death != null && !death.getKeepInventory()) {
            inputs.forEach(item -> death.getDrops().add(item.clone()));
            player.getPersistentDataContainer().set(refundKey, PersistentDataType.STRING, ticket.id().toString()); return;
        }

        require(player.isOnline(),
                "Pemain harus online untuk menerima refund.");

        for (ItemStack item : inputs) {
            player.getInventory().addItem(item.clone()).values()
                    .forEach(left -> drop(
                            player.getLocation(), left,
                            ticket.owner(), ticket.id()));
        }

        player.getPersistentDataContainer().set(refundKey, PersistentDataType.STRING, ticket.id().toString());
        if (death == null) player.saveData();
    }

    public void recover(UUID owner, String action, CommandSender sender) {
        editable();
        if (current.deposits().containsKey(owner)) { recoverDeposit(owner, action, sender); return; }
        require(!closed && !reloading && !runs.containsKey(owner)
                        && !menus.containsKey(owner) && recovering.add(owner),
                "Pemain sedang diproses.");

        Ticket ticket = current.tickets().get(owner);
        if (ticket == null || ticket.phase().terminal()) {
            recovering.remove(owner);
            throw new IllegalArgumentException("Tidak ada ritual tertunda.");
        }

        if (action.equals("discard")) {
            submit(
                    state -> state.phase(owner, ticket.id(), Phase.DISCARDED),
                    () -> {
                        recovering.remove(owner);
                        say(sender, "Ticket ditutup tanpa memberi item.");
                    },
                    () -> {
                        recovering.remove(owner);
                        saveFailed(sender);
                    }
            );
            return;
        }

        Player player = Bukkit.getPlayer(owner);
        if (player == null || player.isDead()
                || !List.of("refund", "result").contains(action)) {
            recovering.remove(owner);
            throw new IllegalArgumentException(
                    "Pilihan refund/result memerlukan pemain online dan hidup.");
        }

        if (ticket.phase() == Phase.REVEALING && action.equals("refund")) {
            recovering.remove(owner); throw new IllegalArgumentException("Ledakan sudah dipastikan. Pilih result atau discard setelah memeriksa item.");
        }
        Phase preparing = action.equals("refund") ? Phase.REFUNDING
                : ticket.phase() == Phase.REVEALING ? Phase.REVEALING : Phase.COMMITTING;

        submit(state -> state.phase(owner, ticket.id(), preparing), () -> {
            try {
                require(player.isOnline() && !player.isDead(),
                        "Pemain tidak tersedia; periksa ticket lagi.");

                if (action.equals("refund")) {
                    refund(player, ticket, null);
                } else {
                    require(!rules.blocked(decode(ticket.result())),
                            "Hasil diblokir aturan item.");
                    deliver(player, ticket, player.getLocation());
                }

                submit(
                        state -> state.phase(
                                owner, ticket.id(),
                                action.equals("refund")
                                        ? Phase.RETURNED : Phase.DELIVERED),
                        () -> {
                            recovering.remove(owner);
                            say(sender, "Pemulihan selesai.");
                            say(player, "Admin memulihkan item ritualmu.");
                        },
                        () -> {
                            recovering.remove(owner);
                            saveFailed(sender);
                        }
                );
            } catch (RuntimeException exception) {
                recovering.remove(owner);
                say(sender, exception.getMessage());
            }
        }, () -> {
            recovering.remove(owner);
            saveFailed(sender);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void blastDamage(EntityDamageEvent event) { blast.adjust(event); }
    private static boolean hasReceipt(Player player, NamespacedKey key, UUID id) {
        return id.toString().equals(player.getPersistentDataContainer().get(key, PersistentDataType.STRING));
    }
    private void deliver(Player player, Ticket ticket, Location position) {
        require(!hasReceipt(player, refundKey, ticket.id()), "Input sudah direfund; hasil ditolak.");
        if (hasReceipt(player, deliveryKey, ticket.id())) return;
        boolean groundExists = Bukkit.getWorlds().stream().flatMap(w -> w.getEntitiesByClass(Item.class).stream())
                .anyMatch(item -> ticket.id().toString().equals(item.getPersistentDataContainer().get(receiptKey, PersistentDataType.STRING))
                        && item.getPersistentDataContainer().has(deliveryKey, PersistentDataType.BYTE)
                        && ticket.owner().equals(item.getOwner()));
        if (!groundExists) drop(position, decode(ticket.result()), ticket.owner(), ticket.id())
                .getPersistentDataContainer().set(deliveryKey, PersistentDataType.BYTE, (byte) 1);
        player.getPersistentDataContainer().set(deliveryKey, PersistentDataType.STRING, ticket.id().toString()); player.saveData();
    }
    private void recoverDeposit(UUID owner, String action, CommandSender sender) {
        require(!menus.containsKey(owner) && !runs.containsKey(owner) && recovering.add(owner), "Pemain sedang diproses.");
        Deposit d = current.deposits().get(owner);
        if (action.equals("discard")) {
            submit(state -> state.removeDeposit(owner, d.id()), () -> {
                recovering.remove(owner); releaseDeposit(owner, d.altar()); say(sender, "Setoran ditutup tanpa memberi item.");
            }, () -> { recovering.remove(owner); saveFailed(sender); }); return;
        }
        Player player = Bukkit.getPlayer(owner);
        if (!action.equals("refund") || player == null || player.isDead()) {
            recovering.remove(owner); throw new IllegalArgumentException("Setoran belum berevolusi: pilih refund dengan pemain online; atau discard.");
        }
        if (d.transfer() != null) {
            boolean consumed = hasReceipt(player, transferKey, d.transfer().id());
            submit(state -> state.settle(owner, d.id(), d.transfer().id(), consumed), () -> {
                recovering.remove(owner);
                if (current.deposits().containsKey(owner)) recoverDeposit(owner, action, sender);
                else { releaseDeposit(owner, d.altar()); say(sender, "Transaksi yang belum dipotong dibatalkan."); }
            }, () -> { recovering.remove(owner); saveFailed(sender); }); return;
        }
        try {
            require(player.isOnline(), "Pemain offline.");
            if (!hasReceipt(player, refundKey, d.id())) {
                give(player, d.items().values().stream().map(EvolutionService::decode).toList(), d.id());
                player.getPersistentDataContainer().set(refundKey, PersistentDataType.STRING, d.id().toString()); player.saveData();
            }
            submit(state -> state.removeDeposit(owner, d.id()), () -> {
                recovering.remove(owner); releaseDeposit(owner, d.altar()); say(sender, "Setoran dikembalikan; altar tersedia lagi.");
            }, () -> { recovering.remove(owner); saveFailed(sender); });
        } catch (RuntimeException exception) { recovering.remove(owner); pendingMessage(player); failure(exception); }
    }

    private void pendingMessage(Player player) {
        say(player,
                "Item ritual perlu diperiksa admin: /evo pending. Jangan mengulangi payout tanpa pemeriksaan.");
    }

    private void cue(Run run, Stage stage) {
        stopAudio(run);
        Cue cue = run.altar.cue(stage);
        Player owner = Bukkit.getPlayer(run.owner);

        if (owner != null
                && (!cue.title().isEmpty() || !cue.subtitle().isEmpty())) {
            int duration = EvolutionAnimation.remaining(
                    stage, run.altar.ticks(), run.elapsed);
            owner.showTitle(Title.title(
                    Component.text(cue.title(), NamedTextColor.GOLD),
                    Component.text(cue.subtitle(), NamedTextColor.GRAY),
                    Title.Times.times(
                            Duration.ofMillis(250),
                            Duration.ofMillis((duration - 10L) * 50),
                            Duration.ofMillis(250))
            ));
        }

        cueSound(run, cue);
    }
    private void cueSound(Run run, Cue cue) {
        if (cue.sound().isEmpty() || cue.volume() == 0) return;
        Player owner = Bukkit.getPlayer(run.owner);
        Location center = location(run.altar.table());
        List<Player> audience = cue.privateSound()
                ? owner == null ? List.of() : List.of(owner)
                : center.getWorld().getPlayers().stream()
                .filter(p -> near(p, run.altar.table(), run.altar.soundRadius()))
                .toList();

        for (Player listener : audience) {
            listener.playSound(
                    listener.getLocation(), cue.sound(), SoundCategory.VOICE,
                    cue.volume(), cue.pitch());

            run.audio.put(listener.getUniqueId(), cue.sound());
            soundOwners.computeIfAbsent(
                            listener.getUniqueId(), id -> new HashMap<>())
                    .put(cue.sound(), run.id);
        }
    }

    private void stopAudio(Run run) {
        run.audio.forEach((id, key) -> {
            Map<String, UUID> ownership = soundOwners.get(id);

            if (ownership != null && ownership.remove(key, run.id)) {
                Player player = Bukkit.getPlayer(id);
                if (player != null) {
                    player.stopSound(key, SoundCategory.VOICE);
                }
                if (ownership.isEmpty()) soundOwners.remove(id);
            }
        });
        run.audio.clear();
    }

    private void stopFinal(UUID owner) {
        Run old = finalAudio.remove(owner);
        if (old != null) stopAudio(old);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void quit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Menu menu = menus.get(player.getUniqueId());
        if (menu != null) removeMenu(menu);
        Map<Integer, ItemDisplay> dormant = depositVisuals.remove(player.getUniqueId());
        if (dormant != null) removeVisuals(dormant); music.quit(player.getUniqueId());

        Run run = runs.get(player.getUniqueId());
        if (run != null) {
            abort(run, player, null,
                    "Ritual dibatalkan karena disconnect.");
        }

        stopFinal(player.getUniqueId());
        soundOwners.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void death(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Menu menu = menus.get(player.getUniqueId());
        if (menu != null) removeMenu(menu);

        Run run = runs.get(player.getUniqueId());
        if (run != null) {
            abort(run, player, event, "Ritual dibatalkan karena mati.");
        }

        stopFinal(player.getUniqueId());
    }

    @EventHandler
    public void join(PlayerJoinEvent event) {
        Ticket ticket = current.tickets().get(event.getPlayer().getUniqueId());
        if (ticket != null && !ticket.phase().terminal()) pendingMessage(event.getPlayer());
        Deposit d = current.deposits().get(event.getPlayer().getUniqueId());
        if (d != null) say(event.getPlayer(), "Setoran evo di altar " + d.altar() + " tersimpan. Buka altar untuk melanjutkan, atau /evo cancel.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void breakBlock(BlockBreakEvent event) {
        if (protectedBlock(event.getBlock())) {
            event.setCancelled(true);
            say(event.getPlayer(), "Block ritual sedang digunakan.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void explode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::protectedBlock);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void explode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::protectedBlock);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void piston(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(block ->
                protectedBlock(block)
                        || protectedBlock(block.getRelative(event.getDirection())))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void piston(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(block ->
                protectedBlock(block)
                        || protectedBlock(block.getRelative(event.getDirection())))) {
            event.setCancelled(true);
        }
    }

    private boolean protectedBlock(Block block) {
        Point point = point(block);
        return leases.keySet().stream()
                .map(current.altars()::get)
                .filter(Objects::nonNull)
                .anyMatch(altar -> altar.table().equals(point)
                        || altar.ingredients().stream()
                        .anyMatch(ingredient -> Objects.equals(ingredient.point(), point)));
    }

    @EventHandler
    public void chunk(ChunkLoadEvent event) {
        Arrays.stream(event.getChunk().getEntities()).forEach(this::removeStale);
    }

    private void removeStale(Entity entity) {
        if (!(entity instanceof ItemDisplay)
                || !entity.getPersistentDataContainer().has(
                visualKey, PersistentDataType.BYTE)) return;

        boolean owned = runs.values().stream().anyMatch(run -> run.visuals.containsValue(entity))
                || depositVisuals.values().stream().anyMatch(visuals -> visuals.containsValue(entity));

        if (!owned) entity.remove();
    }

    private ItemDisplay display(Point point, ItemStack item, double scale) {
        return location(point).getWorld().spawn(
                location(point), ItemDisplay.class, entity -> {
                    entity.setPersistent(false);
                    entity.setInvulnerable(true);
                    entity.setGravity(false);
                    entity.setItemStack(item.asOne());
                    entity.setItemDisplayTransform(
                            ItemDisplay.ItemDisplayTransform.FIXED);
                    entity.setTeleportDuration(2);
                    entity.setInterpolationDuration(2);
                    entity.getPersistentDataContainer().set(
                            visualKey, PersistentDataType.BYTE, (byte) 1);
                    entity.setTransformation(transform(0, 0, scale));
                });
    }

    private static void move(
            ItemDisplay entity, Location position,
            double rotation, double tilt, double scale
    ) {
        require(entity != null && entity.isValid() && entity.teleport(position),
                "Visual gagal dipindahkan.");
        entity.setTransformation(transform(rotation, tilt, scale));
    }

    private static Transformation transform(
            double rotation, double tilt, double scale
    ) {
        return new Transformation(
                new Vector3f(),
                new Quaternionf().rotationY((float) rotation),
                new Vector3f((float) scale),
                new Quaternionf().rotationZ((float) tilt)
        );
    }

    private Item drop(
            Location position, ItemStack stack, UUID owner, UUID receipt
    ) {
        Item item = position.getWorld().dropItem(position, stack.clone());
        item.setOwner(owner);
        item.setPickupDelay(20);
        item.setUnlimitedLifetime(true);
        item.setInvulnerable(true);
        item.setVelocity(new Vector(0, .08, 0));
        item.getPersistentDataContainer().set(
                receiptKey, PersistentDataType.STRING, receipt.toString());
        return item;
    }

    private boolean matches(
            ItemStack actual, String encoded, boolean ignoreDamage
    ) {
        if (!usable(actual)) return false;

        ItemStack first = rules.stacks().prepared(actual.asOne());
        ItemStack second = rules.stacks().prepared(decode(encoded).asOne());

        if (ignoreDamage) {
            first.editMeta(meta -> {
                if (meta instanceof Damageable damage) damage.setDamage(0);
            });
            second.editMeta(meta -> {
                if (meta instanceof Damageable damage) damage.setDamage(0);
            });
        }
        return first.isSimilar(second);
    }

    static String encode(ItemStack item) {
        String result = Base64.getEncoder()
                .encodeToString(item.serializeAsBytes());
        encoded(result, false);
        return result;
    }

    static ItemStack decode(String encoded) {
        ItemStack item = ItemStack.deserializeBytes(
                Base64.getDecoder().decode(encoded));
        require(usable(item), "Data item kosong/tidak valid.");
        return item;
    }

    private static void template(String encoded) {
        require(decode(encoded).getAmount() == 1,
                "Contoh item harus berjumlah satu.");
    }

    private static boolean usable(ItemStack item) {
        return item != null && !item.getType().isAir() && item.getAmount() > 0;
    }

    private static boolean survival(Player player) {
        return player.getGameMode() == GameMode.SURVIVAL
                || player.getGameMode() == GameMode.ADVENTURE;
    }

    public static Point point(Block block) {
        return new Point(
                block.getWorld().getUID(),
                block.getX(), block.getY(), block.getZ());
    }

    private static Location location(Point point) {
        World world = Bukkit.getWorld(point.world());
        require(world != null, "World altar belum tersedia.");
        return new Location(
                world, point.x() + .5, point.y() + 1.2, point.z() + .5);
    }

    private static Location altarCenter(Point point) { return location(point).subtract(0, .7, 0); }

    private static Block block(Point point) {
        return location(point).getWorld()
                .getBlockAt(point.x(), point.y(), point.z());
    }

    private static boolean near(
            Player player, Point point, double radius
    ) {
        return player.getWorld().getUID().equals(point.world())
                && player.getLocation().distanceSquared(location(point))
                <= radius * radius;
    }

    private static Location offset(
            Location base, EvolutionAnimation.Vec vector
    ) {
        return base.clone().add(vector.x(), vector.y(), vector.z());
    }

    private static Particle particle(Preset preset) {
        return switch (preset) {
            case INFERNO, PHOENIX -> Particle.FLAME;
            case STORMFORGE -> Particle.ELECTRIC_SPARK;
            case VOID -> Particle.PORTAL;
            case ASTRAL -> Particle.END_ROD;
            case FROST -> Particle.SNOWFLAKE;
        };
    }

    private static ItemStack icon(
            Material material, String name, boolean green
    ) {
        var item = new ItemStack(material);
        item.editMeta(meta -> meta.displayName(Component.text(
                name, green ? NamedTextColor.GREEN : NamedTextColor.GRAY)));
        return item;
    }

    private boolean live(Run run) {
        return runs.get(run.owner) == run;
    }

    private static void removeVisual(
            Map<Integer, ItemDisplay> visuals, int key
    ) {
        ItemDisplay old = visuals.remove(key);
        if (old != null) old.remove();
    }

    private static void removeVisuals(Map<Integer, ItemDisplay> visuals) {
        visuals.values().forEach(Entity::remove);
        visuals.clear();
    }

    private void removeMenu(Menu menu) {
        // Preview dikosongkan sebelum native close mengembalikan matrix sebagai item nyata.
        menu.rendering = true; menu.inventory.clear(); menus.remove(menu.owner, menu);
        if (!current.deposits().containsKey(menu.owner) && !menu.pending && !runs.containsKey(menu.owner))
            leases.remove(menu.altar.id(), menu.owner);
    }

    private void removeRun(Run run) {
        if (runs.remove(run.owner, run)) {
            leases.remove(run.altar.id(), run.owner);
        }
        removeVisuals(run.visuals);
        stopAudio(run); music.stop(run.id);

        Player player = Bukkit.getPlayer(run.owner);
        if (player != null) player.clearTitle();
    }

    @Override
    public void close() {
        if (closed) return;
        layout.close();
        if (task != null) task.cancel();

        for (Menu menu : List.copyOf(menus.values())) {
            Player player = Bukkit.getPlayer(menu.owner);
            if (player != null
                    && player.getOpenInventory().getTopInventory()
                    == menu.inventory) {
                player.closeInventory();
            }
            removeMenu(menu);
        }

        for (Run run : List.copyOf(runs.values())) {
            abort(run, Bukkit.getPlayer(run.owner), null,
                    "Ritual dihentikan karena plugin ditutup.");
        }

        List.copyOf(finalAudio.keySet()).forEach(this::stopFinal);
        clearDepositVisuals(); music.close();
        closed = true;
        HandlerList.unregisterAll(this);
        writer.shutdown();

        try {
            if (!writer.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning(
                        "Writer evolution belum selesai setelah 10 detik.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class Menu {
        final UUID owner;
        final EvolutionAltar altar;
        final CraftingInventory inventory;
        boolean pending, rendering;
        Menu(UUID owner, EvolutionAltar altar, CraftingInventory inventory) {
            this.owner = owner; this.altar = altar; this.inventory = inventory;
        }
    }

    private static final class Run {
        final UUID owner, id;
        final EvolutionAltar altar;
        final boolean preview;
        final Ticket ticket;
        final Map<Integer, ItemDisplay> visuals;
        final Map<UUID, String> audio = new HashMap<>();
        final List<EvolutionAnimation.Vec> origins;

        boolean consumed, active, paused, finishing, uncertain, musicStarted, commitRequested, commitReady, exploded, revealed;
        int elapsed, revealAge;
        Stage stage;
        long audioUntil;

        Run(
                UUID owner,
                EvolutionAltar altar,
                boolean preview,
                Map<Integer, ItemDisplay> visuals,
                Ticket ticket
        ) {
            this.owner = owner;
            this.altar = altar;
            this.preview = preview;
            this.visuals = visuals;
            this.ticket = ticket;
            id = preview ? UUID.randomUUID() : ticket.id();

            origins = altar.ingredients().stream()
                    .map(ingredient -> new EvolutionAnimation.Vec(
                            ingredient.point().x() - altar.table().x(),
                            ingredient.point().y() - altar.table().y(),
                            ingredient.point().z() - altar.table().z()))
                    .toList();
        }
    }
}
