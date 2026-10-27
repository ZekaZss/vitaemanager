package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.economy.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitTask;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.logging.Level;
import static com.bangzachery.vitae.vitaemanager.npc.NpcShopData.*;

/** NPC-local shop: no public remote-open command, no second economy or item currency. */
public final class NpcShop implements Listener, AutoCloseable {
    private final JavaPlugin plugin; private final VitiService viti; private final NpcService npcs; private final NpcShopStore store;
    private final NamespacedKey markerKey;
    private final Map<UUID, Receipt> pending = new HashMap<>();
    private final Set<UUID> settling = new HashSet<>(); private BukkitTask retry; private boolean closed;
    private final Set<UUID> recoveryNeedsEvidence = new HashSet<>();
    enum View { CATALOG, DETAIL, EDITOR, SAVE }
    private static final class Menu implements com.bangzachery.vitae.vitaemanager.core.gui.VitaeMenu {
        final UUID viewer, token; final String npc; final View view; final long revision; final Map<Integer, Product> products;
        Inventory inventory; int selected, quantity = 1; boolean selling = true;
        Menu(Player player, NpcData.Definition definition, View view, Catalog catalog) {
            viewer = player.getUniqueId(); token = definition.token(); npc = definition.id(); this.view = view; revision = catalog.revision(); products = new HashMap<>(catalog.products());
        }
        @Override public Inventory getInventory() { return inventory; }
    }
    public NpcShop(JavaPlugin plugin, VitiService viti, NpcService npcs) throws IOException {
        this.plugin = plugin; this.viti = viti; this.npcs = npcs; markerKey = new NamespacedKey(plugin, "npc_shop_receipt");
        store = new NpcShopStore(plugin.getDataFolder().toPath().resolve("npc/shops"));
        for (Receipt receipt : store.pending()) if (!receipt.terminal()) {
            if (pending.put(receipt.player(), receipt) != null) throw new IOException("Beberapa journal toko aktif untuk UUID sama; perlu pemeriksaan admin.");
            viti.holdShopAccount(receipt.player(), true);
            recoveryNeedsEvidence.add(receipt.player());
        }
    }
    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        retry = Bukkit.getScheduler().runTaskTimer(plugin, () -> { for (Player player : Bukkit.getOnlinePlayers()) resume(player); }, 1, 20);
    }
    public boolean configured(NpcData.Definition npc) { return !store.catalog(npc.token()).products().isEmpty(); }
    public boolean hasPending(UUID token) { return pending.values().stream().anyMatch(receipt -> receipt.npc().equals(token)); }
    public boolean interact(Player player, NpcData.Definition npc, boolean attack) {
        if (!attack && player.isSneaking() && player.hasPermission("vitae.admin.npc")) { open(player, npc, View.EDITOR); return true; }
        if (!configured(npc)) return false;
        if (!npc.enabled()) { say(player, "Toko sedang dinonaktifkan."); return true; }
        open(player, npc, View.CATALOG); return true;
    }
    private void open(Player player, NpcData.Definition npc, View view) {
        if (npcs.isAbilityBlocked(player)) { say(player, "Kamu tidak dapat membuka toko selama pengaruh sihir ini."); return; }
        if (pending.containsKey(player.getUniqueId())) { say(player, "Transaksi sebelumnya belum selesai; minta admin memeriksa journal toko."); return; }
        show(player, new Menu(player, npc, view, store.catalog(npc.token())));
    }
    private void show(Player player, Menu menu) {
        menu.inventory = Bukkit.createInventory(menu, menu.view == View.DETAIL || menu.view == View.SAVE ? 27 : 54, Component.text("Toko " + menu.npc + " • " + menu.view));
        if (menu.view == View.CATALOG || menu.view == View.EDITOR) {
            for (var entry : menu.products.entrySet()) {
                Product product = entry.getValue(); ItemStack icon = decode(product.sample()); icon.setAmount(1);
                List<String> lore = new ArrayList<>(); lore.add("Produk " + (entry.getKey() + 1));
                if (product.sell() != null) lore.add("Jual " + product.sell().base() + " = " + VitiAmount.format(product.sell().amount()) + " Viti");
                if (product.buy() != null) lore.add("Beli " + product.buy().base() + " = " + VitiAmount.format(product.buy().amount()) + " Viti");
                lore.add("Mode: " + product.mode() + " • Cocok: " + (product.materialOnly() ? "material" : "metadata lengkap"));
                if (menu.view == View.EDITOR) lore.add("Klik pilih; Shift+klik kanan hapus"); else lore.add("Klik untuk detail transaksi");
                ItemMeta meta = icon.getItemMeta(); meta.lore(lore.stream().map(Component::text).toList()); icon.setItemMeta(meta); menu.inventory.setItem(entry.getKey(), icon);
            }
            if (menu.view == View.EDITOR) {
                put(menu, 45, Material.BOOK, "Pilih slot: " + (menu.selected + 1), "Klik item inventory bawah untuk menyalin contoh.");
                put(menu, 46, Material.COMPARATOR, "Ubah mode produk terpilih"); put(menu, 47, Material.HOPPER, "Ubah kecocokan metadata/material");
                put(menu, 49, Material.LIME_CONCRETE, "SIMPAN", "Buka konfirmasi simpan."); put(menu, 51, Material.RED_CONCRETE, "BUANG PERUBAHAN"); put(menu, 53, Material.ARROW, "KEMBALI", "Kembali ke katalog tanpa menyimpan.");
            }
        } else if (menu.view == View.SAVE) {
            put(menu, 11, Material.RED_CONCRETE, "BUANG PERUBAHAN"); put(menu, 13, Material.ARROW, "KEMBALI KE EDITOR"); put(menu, 15, Material.LIME_CONCRETE, "KONFIRMASI SIMPAN");
        } else {
            Product product = menu.products.get(menu.selected);
            put(menu, 4, decode(product.sample()).getType(), "Produk " + (menu.selected + 1), "Jumlah: " + menu.quantity, "Mode: " + (menu.selling ? "JUAL ke toko" : "BELI dari toko"),
                    product.available(menu.selling) ? "Total: " + VitiAmount.format(product.price(menu.selling).total(menu.quantity)) + " Viti" : "Mode/harga belum tersedia");
            put(menu, 9, Material.REDSTONE, "-1"); put(menu, 10, Material.REDSTONE_BLOCK, "-64"); put(menu, 11, Material.GLOWSTONE_DUST, "+1"); put(menu, 12, Material.GLOWSTONE, "+64");
            put(menu, 13, Material.CHEST, "MAKSIMAL"); put(menu, 14, Material.COMPARATOR, "GANTI JUAL/BELI"); put(menu, 18, Material.ARROW, "KEMBALI"); put(menu, 24, Material.LIME_CONCRETE, "KONFIRMASI TRANSAKSI");
        }
        player.openInventory(menu.inventory);
    }
    private static void put(Menu menu, int slot, Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material); ItemMeta meta = stack.getItemMeta(); meta.displayName(Component.text(name, NamedTextColor.YELLOW)); meta.lore(Arrays.stream(lore).map(Component::text).toList()); stack.setItemMeta(meta); menu.inventory.setItem(slot, stack);
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void click(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (pending.containsKey(player.getUniqueId())) { event.setCancelled(true); return; }
        if (!(event.getView().getTopInventory().getHolder() instanceof Menu menu)) return;
        event.setCancelled(true);
        if (!menu.viewer.equals(player.getUniqueId()) || event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT && event.getClick() != ClickType.SHIFT_RIGHT) return;
        int slot = event.getRawSlot();
        // Act next tick and validate exact view, identity, permission and catalog revision again.
        ItemStack sample = event.getCurrentItem() == null ? null : event.getCurrentItem().clone();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (closed || !player.isOnline() || player.getOpenInventory().getTopInventory() != menu.inventory || pending.containsKey(player.getUniqueId())) return;
            try {
                NpcData.Definition npc = npcs.get(menu.npc);
                if (npcs.isAbilityBlocked(player)) throw new IllegalStateException("Kamu tidak dapat bertransaksi selama pengaruh sihir ini.");
                if (!npc.token().equals(menu.token)) throw new IllegalStateException("Identitas NPC berubah; buka ulang toko.");
                if ((menu.view == View.EDITOR || menu.view == View.SAVE) && !player.hasPermission("vitae.admin.npc")) throw new IllegalStateException("Perlu izin vitae.admin.npc.");
                action(player, npc, menu, slot, event.getClick(), sample);
            } catch (IOException | RuntimeException error) { say(player, error.getMessage()); plugin.getLogger().log(Level.WARNING, "Operasi toko belum selesai.", error); }
        });
    }
    private Menu next(Player player, NpcData.Definition npc, Menu old, View view) {
        Menu result = new Menu(player, npc, view, new Catalog(old.revision, old.products)); result.selected = old.selected; result.quantity = old.quantity; result.selling = old.selling; return result;
    }
    private void action(Player player, NpcData.Definition npc, Menu menu, int slot, ClickType click, ItemStack sample) throws IOException {
        if (menu.view == View.CATALOG) {
            if (slot < 0 || slot >= 45 || !menu.products.containsKey(slot)) return;
            if (!npc.enabled()) throw new IllegalStateException("Toko sedang dinonaktifkan.");
            Menu detail = next(player, npc, menu, View.DETAIL); detail.selected = slot; detail.selling = menu.products.get(slot).available(true); show(player, detail); return;
        }
        if (menu.view == View.EDITOR) {
            if (slot >= 0 && slot < 45) {
                menu.selected = slot; if (click == ClickType.SHIFT_RIGHT) menu.products.remove(slot);
            } else if (slot >= menu.inventory.getSize() && sample != null && !sample.getType().isAir()) {
                sample.setAmount(1); Product old = menu.products.get(menu.selected);
                menu.products.put(menu.selected, new Product(encode(sample), old != null && old.materialOnly(), old == null ? Mode.BOTH : old.mode(), old == null ? null : old.sell(), old == null ? null : old.buy()));
            } else if (slot == 46 || slot == 47) {
                Product old = menu.products.get(menu.selected); if (old == null) throw new IllegalArgumentException("Pilih produk terlebih dahulu.");
                menu.products.put(menu.selected, new Product(old.sample(), slot == 47 ? !old.materialOnly() : old.materialOnly(), slot == 46 ? Mode.values()[(old.mode().ordinal() + 1) % 3] : old.mode(), old.sell(), old.buy()));
            } else if (slot == 49) { show(player, next(player, npc, menu, View.SAVE)); return; }
            else if (slot == 51 || slot == 53) { open(player, npc, View.CATALOG); return; }
            show(player, menu); return;
        }
        if (menu.view == View.SAVE) {
            if (slot == 11) open(player, npc, View.CATALOG);
            else if (slot == 13) show(player, next(player, npc, menu, View.EDITOR));
            else if (slot == 15) { store.save(menu.token, menu.revision, menu.products); say(player, "Katalog disimpan. Atur harga: /vnpc " + menu.npc + " shop price <slot> sell|buy <jumlah-dasar> <Viti>"); open(player, npc, View.CATALOG); }
            return;
        }
        Product product = menu.products.get(menu.selected);
        switch (slot) {
            case 9 -> menu.quantity = Math.max(1, menu.quantity - 1); case 10 -> menu.quantity = Math.max(1, menu.quantity - 64);
            case 11 -> menu.quantity = Math.min(2304, menu.quantity + 1); case 12 -> menu.quantity = Math.min(2304, menu.quantity + 64);
            case 13 -> { menu.quantity = maximum(player, product, menu.selling); if (menu.quantity < 1) throw new IllegalArgumentException("Tidak ada jumlah yang dapat ditransaksikan."); }
            case 14 -> menu.selling = !menu.selling;
            case 18 -> { open(player, npc, View.CATALOG); return; }
            case 24 -> { exchange(player, npc, menu); return; }
            default -> { return; }
        }
        show(player, menu);
    }
    private int maximum(Player player, Product product, boolean selling) {
        if (!product.available(selling)) return 0;
        if (selling) return Math.min(2304, count(player.getInventory().getStorageContents(), product));
        int capacity = capacity(player.getInventory().getStorageContents(), decode(product.sample()));
        BigDecimal unit = product.price(false).total(1);
        return viti.balance(player).divideToIntegralValue(unit).min(BigDecimal.valueOf(Math.min(2304, capacity))).intValueExact();
    }
    private void exchange(Player player, NpcData.Definition npc, Menu menu) throws IOException {
        if (npcs.isAbilityBlocked(player)) throw new IllegalStateException("Kamu tidak dapat bertransaksi selama pengaruh sihir ini.");
        if (!npc.enabled()) throw new IllegalStateException("Toko sedang dinonaktifkan.");
        Catalog live = store.catalog(npc.token());
        if (live.revision() != menu.revision || !Objects.equals(live.products().get(menu.selected), menu.products.get(menu.selected))) throw new IllegalStateException("Harga/katalog berubah. Buka ulang toko dan periksa total terbaru.");
        Product product = live.products().get(menu.selected); BigDecimal total = product.price(menu.selling).total(menu.quantity);
        if (!viti.shopAvailable()) throw new IllegalStateException("Ekonomi sedang menyimpan transaksi lain; coba kembali.");
        if (!menu.selling && viti.balance(player).compareTo(total) < 0) throw new IllegalArgumentException("Saldo Viti tidak cukup.");
        ItemStack[] before = cloneItems(player.getInventory().getStorageContents()), after = changed(before, product, menu.quantity, menu.selling);
        // Verify format/native persistence before any item change. SaveData by itself is not evidence.
        String baseline = "preflight:" + UUID.randomUUID(); List<String> beforeBytes = snapshot(before), afterBytes = snapshot(after);
        player.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING, baseline); player.saveData();
        if (!NpcShopEvidence.verified(playerdata(player), markerKey.toString(), baseline, beforeBytes)) throw new IOException("Penyimpanan inventory native belum dapat diverifikasi; transaksi ditolak sebelum item berubah.");
        UUID id = UUID.randomUUID(); String marker = id + ":" + digest(afterBytes);
        Receipt receipt = new Receipt(id, player.getUniqueId(), npc.token(), npc.id(), menu.selected, menu.quantity, menu.selling, total, System.currentTimeMillis(), beforeBytes, afterBytes, marker, baseline, Phase.PREPARED, "");
        store.receipt(receipt); pending.put(player.getUniqueId(), receipt); viti.holdShopAccount(player.getUniqueId(), true); player.closeInventory();
        try {
            player.getInventory().setStorageContents(after); player.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING, marker); player.saveData();
            if (!NpcShopEvidence.verified(playerdata(player), markerKey.toString(), marker, afterBytes)) { quarantine(receipt, "Inventory setelah perubahan belum cocok dengan playerdata. Jangan tebak hasil transaksi."); say(player, "Transaksi dikarantina untuk pemeriksaan admin; journal dan snapshot dipertahankan."); return; }
            receipt = receipt.phase(Phase.INVENTORY_SAVED, "Inventory dan marker tersimpan pada playerdata dan cocok dengan snapshot."); store.receipt(receipt); pending.put(player.getUniqueId(), receipt); settle(player, receipt);
        } catch (IOException | RuntimeException error) { quarantine(receipt, "Perubahan inventory terputus: " + error.getMessage()); throw error; }
    }
    private void resume(Player player) {
        Receipt receipt = pending.get(player.getUniqueId()); if (receipt == null || receipt.phase() == Phase.QUARANTINED || settling.contains(player.getUniqueId())) return;
        try {
            if (!viti.current().redeemed().contains(receipt.id()) && (receipt.phase() == Phase.PREPARED || recoveryNeedsEvidence.contains(player.getUniqueId()))) {
                if (!NpcShopEvidence.verified(playerdata(player), markerKey.toString(), receipt.marker(), receipt.after())) {
                    if (!viti.current().redeemed().contains(receipt.id()) && snapshot(player.getInventory().getStorageContents()).equals(receipt.before()) && NpcShopEvidence.verified(playerdata(player), markerKey.toString(), receipt.beforeMarker(), receipt.before())) {
                        store.abort(receipt); pending.remove(player.getUniqueId()); viti.holdShopAccount(player.getUniqueId(), false); say(player, "Transaksi terputus sebelum item tersimpan; dibatalkan berdasarkan bukti inventory asli. Saldo tidak diubah."); return;
                    }
                    quarantine(receipt, "Pemulihan: marker/inventory tersimpan tidak membuktikan hasil; perlu audit manual."); return;
                }
                receipt = receipt.phase(Phase.INVENTORY_SAVED, "Pemulihan memverifikasi snapshot dan marker native."); store.receipt(receipt); pending.put(player.getUniqueId(), receipt);
            }
            recoveryNeedsEvidence.remove(player.getUniqueId());
            settle(player, receipt);
        } catch (IOException | RuntimeException e) { plugin.getLogger().log(Level.SEVERE, "Pemulihan transaksi toko ditunda; journal dipertahankan: " + receipt.id(), e); }
    }
    private void settle(Player player, Receipt receipt) {
        if (!settling.add(player.getUniqueId())) return;
        viti.shopExchange(player, receipt.id(), receipt.total(), receipt.selling(), result -> {
            settling.remove(player.getUniqueId()); if (closed) return;
            if (result.equals("viti-success")) {
                try { store.complete(receipt); pending.remove(player.getUniqueId()); viti.holdShopAccount(player.getUniqueId(), false); say(player, "Transaksi selesai: " + receipt.quantity() + " item, " + VitiAmount.format(receipt.total()) + " Viti."); }
                catch (IOException error) { plugin.getLogger().log(Level.SEVERE, "Receipt ledger aman, tetapi audit toko belum selesai: " + receipt.id(), error); }
            } else if (!result.equals("viti-busy") && !result.equals("viti-failed")) {
                try { quarantine(receipt, "Ledger menolak penyelesaian: " + result + "; perlu audit, bukan pengulangan inventory."); } catch (IOException error) { plugin.getLogger().log(Level.SEVERE, "Karantina belum tersimpan", error); }
            }
        });
    }
    private void quarantine(Receipt receipt, String reason) throws IOException {
        Receipt quarantined = receipt.phase(Phase.QUARANTINED, reason); store.receipt(quarantined); pending.put(receipt.player(), quarantined);
        plugin.getLogger().severe("Transaksi toko dikarantina " + receipt.id() + " player=" + receipt.player() + ": " + reason);
    }
    private Path playerdata(Player player) { return Bukkit.getWorlds().getFirst().getWorldFolder().toPath().resolve("playerdata").resolve(player.getUniqueId() + ".dat"); }
    private static String digest(List<String> values) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(String.join("\n", values).getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private static ItemStack[] cloneItems(ItemStack[] items) { return Arrays.stream(items).map(item -> item == null ? null : item.clone()).toArray(ItemStack[]::new); }
    private static List<String> snapshot(ItemStack[] items) { return Arrays.stream(items).map(item -> item == null || item.getType().isAir() ? "" : encode(item)).toList(); }
    private static String encode(ItemStack item) { return Base64.getEncoder().encodeToString(item.serializeAsBytes()); }
    private static ItemStack decode(String item) { return NpcService.decode(item); }
    private static boolean matches(ItemStack item, Product product) { if (item == null || item.getType().isAir()) return false; ItemStack sample = decode(product.sample()); return product.materialOnly() ? item.getType() == sample.getType() : item.isSimilar(sample); }
    private static int count(ItemStack[] inventory, Product product) { return Arrays.stream(inventory).filter(item -> matches(item, product)).mapToInt(ItemStack::getAmount).sum(); }
    private static int capacity(ItemStack[] inventory, ItemStack sample) { int result = 0; for (ItemStack item : inventory) if (item == null || item.getType().isAir()) result += sample.getMaxStackSize(); else if (item.isSimilar(sample)) result += Math.max(0, sample.getMaxStackSize() - item.getAmount()); return result; }
    private static ItemStack[] changed(ItemStack[] inventory, Product product, int quantity, boolean selling) {
        ItemStack[] result = cloneItems(inventory); int remaining = quantity; ItemStack sample = decode(product.sample());
        if (selling && count(result, product) < quantity) throw new IllegalArgumentException("Item yang cocok tidak cukup.");
        if (!selling && capacity(result, sample) < quantity) throw new IllegalArgumentException("Inventory tidak cukup untuk seluruh jumlah; tidak ada item dijatuhkan.");
        for (int i = 0; i < result.length && remaining > 0; i++) {
            ItemStack item = result[i];
            if (selling && matches(item, product)) { int amount = Math.min(remaining, item.getAmount()); item.setAmount(item.getAmount() - amount); if (item.getAmount() == 0) result[i] = null; remaining -= amount; }
            else if (!selling && (item == null || item.getType().isAir() || item.isSimilar(sample))) {
                int current = item == null || item.getType().isAir() ? 0 : item.getAmount(); int amount = Math.min(remaining, sample.getMaxStackSize() - current);
                if (amount > 0) { ItemStack copy = sample.clone(); copy.setAmount(current + amount); result[i] = copy; remaining -= amount; }
            }
        }
        if (remaining != 0) throw new IllegalStateException("Perubahan inventory tidak lengkap."); return result;
    }
    private static void say(Player player, String text) { NpcService.message(player, Objects.requireNonNullElse(text, "Operasi toko gagal.")); }

    @EventHandler(priority = EventPriority.LOWEST) public void join(PlayerJoinEvent event) { resume(event.getPlayer()); }
    @EventHandler(priority = EventPriority.HIGHEST) public void drag(InventoryDragEvent event) { if (event.getView().getTopInventory().getHolder() instanceof Menu || pending.containsKey(event.getWhoClicked().getUniqueId())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void drop(PlayerDropItemEvent event) { if (pending.containsKey(event.getPlayer().getUniqueId())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void swap(PlayerSwapHandItemsEvent event) { if (pending.containsKey(event.getPlayer().getUniqueId())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void interact(PlayerInteractEvent event) { if (pending.containsKey(event.getPlayer().getUniqueId())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void interactEntity(PlayerInteractEntityEvent event) { if (pending.containsKey(event.getPlayer().getUniqueId())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void consume(PlayerItemConsumeEvent event) { if (pending.containsKey(event.getPlayer().getUniqueId())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void durability(PlayerItemDamageEvent event) { if (pending.containsKey(event.getPlayer().getUniqueId())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void pickup(EntityPickupItemEvent event) { if (pending.containsKey(event.getEntity().getUniqueId())) event.setCancelled(true); }
    @Override public void close() { closed = true; if (retry != null) retry.cancel(); for (Player player : Bukkit.getOnlinePlayers()) if (player.getOpenInventory().getTopInventory().getHolder() instanceof Menu) player.closeInventory(); HandlerList.unregisterAll(this); }

    public void command(Player admin, NpcData.Definition npc, String[] args) throws IOException {
        if (args.length < 3) throw new IllegalArgumentException("/vnpc <id> shop edit|price|template|status|retry|copy");
        Catalog catalog = store.catalog(npc.token());
        switch (args[2].toLowerCase(Locale.ROOT)) {
            case "edit" -> { if (args.length != 3) throw new IllegalArgumentException("/vnpc <id> shop edit"); open(admin, npc, View.EDITOR); }
            case "price" -> {
                if (args.length != 7 || !Set.of("sell", "buy").contains(args[4].toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("/vnpc <id> shop price <slot 1–45> sell|buy <jumlah-dasar> <Viti>");
                int slot = Integer.parseInt(args[3]) - 1; Product product = catalog.products().get(slot); if (product == null) throw new IllegalArgumentException("Produk belum tersedia; salin contoh lewat editor.");
                Price price = new Price(Integer.parseInt(args[5]), VitiAmount.parse(args[6])); var products = new HashMap<>(catalog.products()); products.put(slot, product.withPrice(args[4].equalsIgnoreCase("sell"), price)); store.save(npc.token(), catalog.revision(), products); say(admin, "Harga disimpan tanpa pembulatan.");
            }
            case "template" -> {
                if (args.length != 4) throw new IllegalArgumentException("/vnpc <id> shop template <1|2|3|4>");
                if (!catalog.products().isEmpty()) throw new IllegalArgumentException("Template hanya dapat diterapkan pada katalog kosong.");
                store.save(npc.token(), catalog.revision(), template(Integer.parseInt(args[3]))); say(admin, "Template diterapkan. Aktifkan NPC dengan /vnpc " + npc.id() + " on.");
            }
            case "copy" -> { if (args.length != 4) throw new IllegalArgumentException("/vnpc <id> shop copy <id-baru>"); npcs.copyShop(admin, npc.id(), args[3]); }
            case "status" -> {
                say(admin, "Katalog revisi " + catalog.revision() + ": " + catalog.products().size() + " produk.");
                for (Receipt receipt : store.pending()) if (receipt.npc().equals(npc.token()) && !receipt.terminal()) say(admin, receipt.id() + " player=" + receipt.player() + " " + receipt.phase() + " " + receipt.reason());
            }
            case "retry" -> {
                if (args.length != 4) throw new IllegalArgumentException("/vnpc <id> shop retry <receipt-uuid>");
                UUID id = UUID.fromString(args[3]); Receipt receipt = pending.values().stream().filter(r -> r.id().equals(id) && r.npc().equals(npc.token())).findFirst().orElseThrow(() -> new IllegalArgumentException("Receipt aktif tidak ditemukan."));
                if (viti.current().redeemed().contains(receipt.id())) { store.complete(receipt); pending.remove(receipt.player()); recoveryNeedsEvidence.remove(receipt.player()); viti.holdShopAccount(receipt.player(), false); say(admin, "Receipt sudah tersimpan di ledger; audit diselesaikan tanpa mengubah inventory/saldo lagi."); return; }
                Player player = Bukkit.getPlayer(receipt.player()); if (player == null) throw new IllegalArgumentException("Pemilik receipt harus online untuk verifikasi playerdata.");
                if (!NpcShopEvidence.verified(playerdata(player), markerKey.toString(), receipt.marker(), receipt.after())) {
                    if (!viti.current().redeemed().contains(receipt.id()) && snapshot(player.getInventory().getStorageContents()).equals(receipt.before()) && NpcShopEvidence.verified(playerdata(player), markerKey.toString(), receipt.beforeMarker(), receipt.before())) { store.abort(receipt); pending.remove(receipt.player()); viti.holdShopAccount(receipt.player(), false); say(admin, "Receipt dibatalkan berdasarkan bukti native inventory sebelum transaksi. Saldo tidak diubah."); return; }
                    throw new IllegalStateException("Bukti inventory tidak cocok; receipt tetap dikarantina. Periksa snapshot/journal dan playerdata, jangan hapus receipt.");
                }
                Receipt next = receipt.phase(Phase.INVENTORY_SAVED, "Retry admin setelah bukti native diverifikasi."); store.receipt(next); pending.put(next.player(), next); settle(player, next);
            }
            default -> throw new IllegalArgumentException("/vnpc <id> shop edit|price|template|status|retry|copy");
        }
    }
    public void copy(NpcData.Definition source, NpcData.Definition destination) throws IOException { Catalog catalog = store.catalog(source.token()); store.save(destination.token(), 0, catalog.products()); }
    public void delete(NpcData.Definition npc) throws IOException { store.remove(npc.token()); }
    private static Map<Integer, Product> template(int number) {
        Map<Integer, Product> products = new LinkedHashMap<>();
        switch (number) {
            case 1 -> {
                add(products, new ItemStack(Material.CARROT), true, 64, 85); add(products, new ItemStack(Material.WHEAT), true, 64, 85); add(products, new ItemStack(Material.ROTTEN_FLESH), true, 32, 75);
                add(products, new ItemStack(Material.HAY_BLOCK), true, 16, 60); add(products, new ItemStack(Material.BREAD), true, 20, 40);
                for (Material material : List.of(Material.COD, Material.SALMON, Material.TROPICAL_FISH, Material.PUFFERFISH)) add(products, new ItemStack(material), true, 1, 10);
            }
            case 2 -> { add(products, new ItemStack(Material.GOLDEN_APPLE), false, 16, 100); add(products, new ItemStack(Material.ENDER_PEARL), false, 16, 85); add(products, new ItemStack(Material.GOLDEN_CARROT), false, 64, 50); add(products, new ItemStack(Material.EXPERIENCE_BOTTLE), false, 64, 35); }
            case 3 -> {
                book(products, Enchantment.MENDING, 1, 5000); book(products, Enchantment.SWIFT_SNEAK, 3, 5000);
                book(products, Enchantment.UNBREAKING, 3, 35); book(products, Enchantment.FIRE_ASPECT, 2, 35); book(products, Enchantment.LOOTING, 3, 35); book(products, Enchantment.FORTUNE, 3, 35); book(products, Enchantment.EFFICIENCY, 5, 35); book(products, Enchantment.FEATHER_FALLING, 4, 35); book(products, Enchantment.INFINITY, 1, 35); book(products, Enchantment.KNOCKBACK, 2, 35); book(products, Enchantment.POWER, 5, 35); book(products, Enchantment.PROTECTION, 4, 35); book(products, Enchantment.SHARPNESS, 5, 35); book(products, Enchantment.SILK_TOUCH, 1, 35); book(products, Enchantment.SWEEPING_EDGE, 3, 35);
            }
            case 4 -> { ItemStack item = new ItemStack(Material.SPLASH_POTION); PotionMeta meta = (PotionMeta) item.getItemMeta(); meta.setBasePotionType(PotionType.LUCK); item.setItemMeta(meta); add(products, item, false, 1, 1600); }
            default -> throw new IllegalArgumentException("Template harus 1, 2, 3, atau 4.");
        }
        return products;
    }
    private static void add(Map<Integer, Product> products, ItemStack sample, boolean selling, int base, int amount) { Price price = new Price(base, BigDecimal.valueOf(amount)); products.put(products.size(), new Product(encode(sample), false, selling ? Mode.SELL : Mode.BUY, selling ? price : null, selling ? null : price)); }
    private static void book(Map<Integer, Product> products, Enchantment enchantment, int level, int amount) { ItemStack item = new ItemStack(Material.ENCHANTED_BOOK); EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta(); meta.addStoredEnchant(enchantment, level, true); item.setItemMeta(meta); add(products, item, false, 1, amount); }
}
