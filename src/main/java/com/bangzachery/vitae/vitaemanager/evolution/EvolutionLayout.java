package com.bangzachery.vitae.vitaemanager.evolution;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionAltar.*;

/** Template-only native crafting editor. Preview items never leave its matrix. */
public final class EvolutionLayout implements Listener, AutoCloseable {
    private static final class Editor {
        final UUID owner;
        final EvolutionAltar before;
        final CraftingInventory inventory;
        final Map<Integer, ItemStack> samples = new TreeMap<>();
        boolean rendering;
        Editor(UUID owner, EvolutionAltar before, CraftingInventory inventory) {
            this.owner = owner; this.before = before; this.inventory = inventory;
            for (Ingredient ingredient : before.ingredients()) samples.put(ingredient.slot(),
                    EvolutionService.decode(ingredient.item()).asQuantity(ingredient.count()));
        }
    }
    private final JavaPlugin plugin;
    private final EvolutionService service;
    private final Map<UUID, Editor> editors = new HashMap<>();
    private boolean closed;
    EvolutionLayout(JavaPlugin plugin, EvolutionService service) { this.plugin = plugin; this.service = service; }
    public boolean inUse(String id) { return editors.values().stream().anyMatch(e -> e.before.id().equals(id)); }
    public boolean empty() { return editors.isEmpty(); }
    public void open(Player admin, String id) {
        require(!closed && (admin.hasPermission("vitae.admin") && admin.hasPermission("vitae.admin.evolution")), "Perlu izin vitae.admin.evolution.");
        service.checkSetup(id);
        EvolutionAltar altar = service.altar(id);
        require(!altar.source().isEmpty() && !altar.result().isEmpty(), "Set source dan result dahulu, lalu /evo layout " + id + ".");
        require(!editors.containsKey(admin.getUniqueId()), "Tutup editor sebelumnya dahulu.");
        require(admin.getItemOnCursor() == null || admin.getItemOnCursor().getType().isAir(), "Kosongkan cursor sebelum membuka editor.");
        World world = Bukkit.getWorld(altar.table().world());
        require(world != null && admin.getWorld().equals(world), "Admin harus di dunia altar.");
        require(world.getBlockAt(altar.table().x(), altar.table().y(), altar.table().z()).getType() == Material.CRAFTING_TABLE,
                "Crafting table altar hilang.");
        var view = admin.openWorkbench(new Location(world, altar.table().x(), altar.table().y(), altar.table().z()), true);
        require(view != null && view.getTopInventory() instanceof CraftingInventory, "Editor crafting tidak dapat dibuka.");
        var editor = new Editor(admin.getUniqueId(), altar, (CraftingInventory) view.getTopInventory());
        editors.put(editor.owner, editor); paint(editor);
        service.say(admin, "EDITOR ADMIN: cursor menyalin contoh bahan, tidak mengambil item asli. Klik kiri menyalin jumlah stack; klik kanan satu.");
        service.say(admin, "Cursor kosong + klik slot menghapus contoh. Slot 5 dan hasil kanan terkunci. Tutup GUI untuk menyimpan susunan sebagai draft OFF.");
    }
    private Editor editor(InventoryView view) {
        Editor editor = editors.get(view.getPlayer().getUniqueId());
        return editor != null && editor.inventory == view.getTopInventory() ? editor : null;
    }
    private void paint(Editor editor) {
        editor.rendering = true;
        try {
            editor.inventory.clear(); editor.samples.forEach((slot, sample) -> editor.inventory.setItem(slot, sample.clone()));
            editor.inventory.setItem(5, EvolutionService.decode(editor.before.source()).asOne());
            editor.inventory.setResult(EvolutionService.decode(editor.before.result()).asOne());
        } finally { editor.rendering = false; }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void craft(PrepareItemCraftEvent event) {
        Editor editor = editor(event.getView());
        if (editor != null && !editor.rendering) editor.inventory.setResult(EvolutionService.decode(editor.before.result()).asOne());
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void recipe(com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent event) {
        if (editor(event.getPlayer().getOpenInventory()) != null) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void click(InventoryClickEvent event) {
        Editor editor = editor(event.getView()); if (editor == null) return;
        boolean denied = event.isCancelled(), simple = event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT;
        if (event.getRawSlot() >= 10 && simple && !denied) return;
        event.setCancelled(true);
        if (denied || !simple || !EvolutionGrid.ingredientSlot(event.getRawSlot()) || !(event.getWhoClicked() instanceof Player player)) return;
        try {
            require((player.hasPermission("vitae.admin") && player.hasPermission("vitae.admin.evolution")), "Izin admin berubah; susunan tidak disimpan.");
            ItemStack cursor = event.getCursor();
            if (cursor == null || cursor.getType().isAir()) editor.samples.remove(event.getRawSlot());
            else {
                service.templateAllowed(cursor);
                editor.samples.put(event.getRawSlot(), cursor.asQuantity(event.getClick() == ClickType.RIGHT ? 1 : cursor.getAmount()));
            }
            paint(editor);
        } catch (RuntimeException e) { service.say(player, e.getMessage()); }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void drag(InventoryDragEvent event) {
        Editor editor = editor(event.getView()); if (editor == null) return;
        if (event.getRawSlots().stream().noneMatch(s -> s < 10)) return;
        boolean denied = event.isCancelled(); event.setCancelled(true);
        if (denied || !(event.getWhoClicked() instanceof Player player)) return;
        try {
            require((player.hasPermission("vitae.admin") && player.hasPermission("vitae.admin.evolution")), "Izin admin berubah.");
            require(event.getRawSlots().stream().allMatch(EvolutionGrid::ingredientSlot), "Drag contoh hanya di slot pinggir.");
            Map<Integer, ItemStack> changed = new HashMap<>();
            for (var entry : event.getNewItems().entrySet()) {
                service.templateAllowed(entry.getValue()); changed.put(entry.getKey(), entry.getValue().clone());
            }
            editor.samples.putAll(changed); paint(editor);
        } catch (RuntimeException e) { service.say(player, e.getMessage()); }
    }
    @EventHandler(priority = EventPriority.LOWEST)
    public void closeEditor(InventoryCloseEvent event) {
        Editor editor = editor(event.getView()); if (editor == null) return;
        editors.remove(editor.owner); editor.inventory.clear(); // Before native crafting returns its matrix.
        if (closed || !(event.getPlayer() instanceof Player admin)) return;
        try {
            require(admin.isOnline() && !admin.isDead() && (admin.hasPermission("vitae.admin") && admin.hasPermission("vitae.admin.evolution")), "Editor ditutup; susunan tidak disimpan.");
            require(service.altar(editor.before.id()).equals(editor.before), "Altar berubah; buka ulang editor.");
            List<Ingredient> ingredients = new ArrayList<>();
            for (var entry : editor.samples.entrySet()) {
                Point pedestal = editor.before.ingredients().stream().filter(i -> i.slot() == entry.getKey())
                        .map(Ingredient::point).filter(Objects::nonNull).findFirst().orElse(null);
                ingredients.add(new Ingredient(EvolutionService.encode(entry.getValue().asOne()), entry.getValue().getAmount(), pedestal, entry.getKey()));
            }
            service.edit(editor.before.id(), section -> {
                section.set("enabled", false); section.set("ingredients", null);
                var root = section.createSection("ingredients");
                for (int i = 0; i < ingredients.size(); i++) EvolutionStore.writeIngredient(root.createSection(Integer.toString(i + 1)), ingredients.get(i));
            }, admin);
            service.say(admin, "Susunan dikirim untuk disimpan. Setelah pesan tersimpan: lihat pedestal, /evo pedestal " + editor.before.id() + " <slot>. Terakhir /evo enable " + editor.before.id() + ".");
        } catch (RuntimeException e) { service.say(admin, e.getMessage()); }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void drop(PlayerDropItemEvent event) { if (editors.containsKey(event.getPlayer().getUniqueId())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void swap(PlayerSwapHandItemsEvent event) { if (editors.containsKey(event.getPlayer().getUniqueId())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.LOWEST)
    public void death(org.bukkit.event.entity.PlayerDeathEvent event) { discard(event.getEntity()); }
    @EventHandler(priority = EventPriority.LOWEST)
    public void quit(PlayerQuitEvent event) { discard(event.getPlayer()); }
    private void discard(Player player) {
        Editor editor = editors.remove(player.getUniqueId()); if (editor != null) editor.inventory.clear();
    }
    @Override public void close() {
        closed = true;
        for (Editor editor : List.copyOf(editors.values())) {
            editor.inventory.clear(); Player player = Bukkit.getPlayer(editor.owner);
            if (player != null && player.getOpenInventory().getTopInventory() == editor.inventory) player.closeInventory();
        }
        editors.clear(); HandlerList.unregisterAll(this);
    }
}
