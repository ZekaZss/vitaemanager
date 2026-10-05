# Vitae: wajib jongkok untuk mengambil item, offset arah NPC, pemulihan modul NPC.
# Simpan sejajar gradlew.bat. -CheckOnly memeriksa tanpa mengubah proyek.
[CmdletBinding()]
param([string]$ProjectPath = $PSScriptRoot, [switch]$CheckOnly)
$ErrorActionPreference = 'Stop'
$utf8Vitae = New-Object System.Text.UTF8Encoding($false)
$projectVitae = [IO.Path]::GetFullPath($ProjectPath)
$relativeRootVitae = 'src/main/java/com/bangzachery/vitae/vitaemanager'
$folderVitae = Join-Path $projectVitae $relativeRootVitae
if (-not (Test-Path -LiteralPath $folderVitae -PathType Container)) {
    throw 'Folder sumber Vitae tidak ditemukan. Simpan script sejajar gradlew.bat, atau berikan -ProjectPath.'
}
function Get-VitaeHash([byte[]]$Bytes) {
    $hashVitae = [Security.Cryptography.SHA256]::Create()
    try { return ([BitConverter]::ToString($hashVitae.ComputeHash($Bytes))).Replace('-', '').ToLowerInvariant() }
    finally { $hashVitae.Dispose() }
}
$filesVitae = @(
    [PSCustomObject]@{
        Name = 'items/ItemRulesListener.java'
        Hash = 'a6b48950a74c4d8c59f087bc4e5c1e45a2e233d9eab6d8e4f7236f9d969c5516'
        Source = @'
package com.bangzachery.vitae.vitaemanager.items;

import com.bangzachery.vitae.vitaemanager.core.config.ConfigurationService;
import com.bangzachery.vitae.vitaemanager.core.gui.VitaeMenu;
import com.bangzachery.vitae.vitaemanager.core.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

public final class ItemRulesListener implements Listener, AutoCloseable {
    private final JavaPlugin plugin;
    private final ConfigurationService configuration;
    private final MessageService messages;
    private final ItemRulesService rules;
    private final Map<UUID, BukkitTask> pending = new HashMap<>();
    private final Map<UUID, Long> feedback = new HashMap<>();
    private boolean closed;

    public ItemRulesListener(JavaPlugin plugin, ConfigurationService configuration,
                             MessageService messages, ItemRulesService rules) {
        this.plugin = plugin; this.configuration = configuration; this.messages = messages; this.rules = rules;
    }
    public void start() { Bukkit.getOnlinePlayers().forEach(this::schedule); }
    private void schedule(Player player) {
        if (closed || pending.containsKey(player.getUniqueId())) return;
        pending.put(player.getUniqueId(), Bukkit.getScheduler().runTask(plugin, () -> {
            pending.remove(player.getUniqueId());
            if (!closed && player.isOnline()) rules.stacks().normalize(player);
        }));
    }
    private void denied(Player player) {
        long now = System.nanoTime();
        Long last = feedback.get(player.getUniqueId());
        if (last == null || now - last > 1_000_000_000L) {
            feedback.put(player.getUniqueId(), now); messages.send(player, "item-blocked");
        }
    }
    private boolean blocked(ItemStack item) { return rules.blocked(item); }
    private boolean usable(ItemStack item) { return item != null && !item.getType().isAir() && item.getAmount() > 0; }

    @EventHandler(priority = EventPriority.LOWEST)
    public void interact(PlayerInteractEvent event) {
        if (!blocked(event.getItem())) return;
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        denied(event.getPlayer());
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void entityInteract(PlayerInteractEntityEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItem(event.getHand());
        if (blocked(item)) { event.setCancelled(true); denied(event.getPlayer()); }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void entityInteractAt(PlayerInteractAtEntityEvent event) { entityInteract(event); }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void attack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && blocked(player.getInventory().getItemInMainHand())) {
            event.setCancelled(true); denied(player);
        }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void place(BlockPlaceEvent event) {
        if (blocked(event.getItemInHand())) { event.setCancelled(true); denied(event.getPlayer()); }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void breakBlock(BlockBreakEvent event) {
        if (blocked(event.getPlayer().getInventory().getItemInMainHand())) { event.setCancelled(true); denied(event.getPlayer()); }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void shoot(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player && (blocked(event.getBow()) || blocked(event.getConsumable()))) {
            event.setCancelled(true); denied(player);
        }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void consume(PlayerItemConsumeEvent event) {
        if (blocked(event.getItem())) { event.setCancelled(true); denied(event.getPlayer()); }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void drop(PlayerDropItemEvent event) {
        if (blocked(event.getItemDrop().getItemStack())) { event.setCancelled(true); denied(event.getPlayer()); }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void swap(PlayerSwapHandItemsEvent event) {
        if (blocked(event.getMainHandItem()) || blocked(event.getOffHandItem())) { event.setCancelled(true); denied(event.getPlayer()); }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void spawn(CreatureSpawnEvent event) {
        if (rules.current().blockedMobs().contains(event.getEntityType())) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void click(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getView().getTopInventory().getHolder() instanceof VitaeMenu) return;
        ItemStack hotbar = event.getHotbarButton() >= 0 ? player.getInventory().getItem(event.getHotbarButton()) : null;
        boolean reject = blocked(event.getCurrentItem()) || blocked(event.getCursor()) || blocked(hotbar)
                || (event.getClick() == ClickType.SWAP_OFFHAND && blocked(player.getInventory().getItemInOffHand()));
        if (event instanceof CraftItemEvent craft) {
            for (ItemStack ingredient : craft.getInventory().getMatrix()) reject |= blocked(ingredient);
        }
        // Double-click collects from every slot; do not allow it to collect blocked material.
        if (event.getClick() == ClickType.DOUBLE_CLICK && blocked(event.getCursor())) reject = true;
        if (reject) { event.setCancelled(true); denied(player); }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void drag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && !(event.getView().getTopInventory().getHolder() instanceof VitaeMenu)
                && blocked(event.getOldCursor())) { event.setCancelled(true); denied(player); }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void pickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.isSneaking()) { event.setCancelled(true); return; }
        ItemStack ground = event.getItem().getItemStack();
        if (blocked(ground)) { event.setCancelled(true); return; }
        if (!rules.stacks().managed(ground)) { schedule(player); return; }
        // Native pickup may merge using old components. Allocate real storage explicitly instead.
        event.setCancelled(true);
        rules.stacks().normalize(player);
        ItemStack prepared = rules.stacks().prepared(ground);
        List<PickupPlan.Slot> slots = new ArrayList<>();
        for (int index = 0; index < 36; index++) {
            ItemStack existing = player.getInventory().getItem(index);
            slots.add(new PickupPlan.Slot(usable(existing) ? existing.getAmount() : 0,
                    usable(existing) && existing.isSimilar(prepared)));
        }
        PickupPlan plan = PickupPlan.allocate(ground.getAmount(), prepared.getMaxStackSize(), slots);
        int collected = ground.getAmount() - plan.remaining();
        if (collected == 0) return;
        for (int index = 0; index < 36; index++) {
            int add = plan.added().get(index);
            if (add == 0) continue;
            ItemStack existing = player.getInventory().getItem(index);
            ItemStack result = usable(existing) ? existing.clone() : prepared.clone();
            result.setAmount((usable(existing) ? existing.getAmount() : 0) + add);
            player.getInventory().setItem(index, result);
        }
        player.playPickupItemAnimation(event.getItem(), collected);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.2f, 1.0f);
        if (plan.remaining() == 0) event.getItem().remove();
        else { ItemStack remainder = ground.clone(); remainder.setAmount(plan.remaining()); event.getItem().setItemStack(remainder); }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void pickupArrow(PlayerPickupArrowEvent event) {
        if (!event.getPlayer().isSneaking() || blocked(event.getItem().getItemStack())) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void clicked(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) schedule(player);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void dragged(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) schedule(player);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void opened(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) schedule(player);
    }
    @EventHandler public void joined(PlayerJoinEvent event) { schedule(event.getPlayer()); }
    @EventHandler public void closed(InventoryCloseEvent event) { if (event.getPlayer() instanceof Player player) schedule(player); }
    @EventHandler public void held(PlayerItemHeldEvent event) { schedule(event.getPlayer()); }
    @EventHandler public void quit(PlayerQuitEvent event) {
        BukkitTask task = pending.remove(event.getPlayer().getUniqueId());
        if (task != null) task.cancel();
        feedback.remove(event.getPlayer().getUniqueId());
    }
    @Override public void close() { closed = true; pending.values().forEach(BukkitTask::cancel); pending.clear(); feedback.clear(); }
}
'@
    },
    [PSCustomObject]@{
        Name = 'npc/NpcData.java'
        Hash = '10a34d23245c29daadcd13487575c1258a519d2221f9cdb2595c69fb00c0735a'
        Source = @'
package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.economy.VitiAmount;
import com.bangzachery.vitae.vitaemanager.whisper.WhisperArea;
import java.math.BigDecimal;
import java.util.*;

/** Immutable definitions and durable progress. No Bukkit calls in this file. */
public final class NpcData {
    private NpcData() {}
    public enum Kind { HUMAN, MODEL }
    public enum QuestKind { NONE, FARM, MOB }
    public record Facing(float yaw, float pitch) {
        public Facing {
            if (!Float.isFinite(yaw) || Math.abs(yaw) > 360 || !Float.isFinite(pitch) || Math.abs(pitch) > 90)
                throw new IllegalArgumentException("Offset arah: yaw -360 sampai 360, pitch -90 sampai 90.");
        }
        public static Facing zero() { return new Facing(0, 0); }
    }
    public record Point(UUID world, double x, double y, double z, float yaw, float pitch) {
        public Point {
            Objects.requireNonNull(world);
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || Math.abs(x) > 30_000_000 || Math.abs(z) > 30_000_000 || Math.abs(y) > 4096
                    || !Float.isFinite(yaw) || !Float.isFinite(pitch) || Math.abs(pitch) > 90)
                throw new IllegalArgumentException("Lokasi NPC tidak valid.");
        }
    }
    public record Reward(BigDecimal viti, List<String> items) {
        public Reward {
            viti = VitiAmount.checked(viti);
            items = List.copyOf(items);
            if (items.size() > 27 || (viti.signum() > 0 && !items.isEmpty()))
                throw new IllegalArgumentException("Pilih hadiah Viti atau item, bukan keduanya.");
            for (String item : items) if (item.isBlank() || item.length() > 2_000_000)
                throw new IllegalArgumentException("Data item terlalu besar atau kosong.");
        }
        public static Reward none() { return new Reward(BigDecimal.ZERO, List.of()); }
        public boolean empty() { return viti.signum() == 0 && items.isEmpty(); }
    }
    public record Choice(String label, String target) {
        public Choice {
            label = text(label, 80, "Label pilihan");
            target = id(target);
        }
    }
    public record Node(String text, List<Choice> choices) {
        public Node { text = NpcData.text(text, 512, "Dialog"); choices = List.copyOf(choices);
            if (choices.size() > 8) throw new IllegalArgumentException("Maksimum delapan pilihan per halaman."); }
        public List<Choice> available() {
            return choices.isEmpty() ? List.of(new Choice("Keluar...", "END")) : choices;
        }
    }
    public record Crop(int x, int y, int z, String plant, String soil) {
        public Crop { plant = text(plant, 200, "Tanaman"); soil = text(soil, 200, "Tanah"); }
        public String key() { return x + ":" + y + ":" + z; }
    }
    public record Quest(QuestKind kind, WhisperArea area, Point start, int goal,
                        String mob, List<Crop> crops, Reward reward) {
        public Quest {
            Objects.requireNonNull(kind); Objects.requireNonNull(reward);
            crops = List.copyOf(crops);
            mob = text(mob, 64, "Jenis mob");
            if (goal < 1 || goal > 50) throw new IllegalArgumentException("Target quest harus 1 sampai 50.");
            if (kind != QuestKind.NONE) {
                Objects.requireNonNull(area); Objects.requireNonNull(start);
                if (!inside(area, start)) throw new IllegalArgumentException("Posisi awal harus di dalam area quest.");
            }
            if (kind == QuestKind.FARM) {
                if (crops.isEmpty() || crops.size() > 512 || !reward.items().isEmpty())
                    throw new IllegalArgumentException("Ladang perlu 1–512 tanaman dan hadiah berupa Viti.");
                Set<String> keys = new HashSet<>();
                for (Crop crop : crops) if (!keys.add(crop.key())
                        || !area.contains(area.world(), crop.x() + .5, crop.y() + .5, crop.z() + .5))
                    throw new IllegalArgumentException("Tanaman duplikat atau di luar seleksi.");
            }
        }
        public static Quest none() { return new Quest(QuestKind.NONE, null, null, 1, "COW", List.of(), Reward.none()); }
    }
    public record Definition(String id, UUID token, String name, Kind kind, String model,
                             String texture, String signature, Point location, Point stand,
                             double eyeHeight, WhisperArea trigger, String root, Map<String, Node> nodes,
                             Reward gift, Quest quest, String repeat, int speed, boolean enabled, Facing facing) {
        public Definition {
            facing = Objects.requireNonNullElse(facing, Facing.zero());
            id = NpcData.id(id); Objects.requireNonNull(token); name = text(name, 64, "Nama NPC");
            Objects.requireNonNull(kind); Objects.requireNonNull(location); Objects.requireNonNull(stand);
            Objects.requireNonNull(gift); Objects.requireNonNull(quest);
            model = Objects.requireNonNull(model); texture = Objects.requireNonNull(texture);
            signature = Objects.requireNonNull(signature); root = NpcData.id(root);
            nodes = Map.copyOf(nodes); repeat = text(repeat, 512, "Dialog pengulangan");
            if (model.length() > 128 || texture.length() > 16_384 || signature.length() > 16_384)
                throw new IllegalArgumentException("Model atau data skin terlalu besar.");
            if (nodes.isEmpty() || nodes.size() > 128 || !nodes.containsKey(root))
                throw new IllegalArgumentException("Halaman awal tidak ditemukan; maksimum 128 halaman.");
            for (String key : nodes.keySet()) NpcData.id(key);
            if (!Double.isFinite(eyeHeight) || eyeHeight < .2 || eyeHeight > 8 || speed < 1 || speed > 8)
                throw new IllegalArgumentException("Tinggi kepala 0,2–8; kecepatan teks 1–8.");
            if (!location.world().equals(stand.world()) || (trigger != null && !trigger.world().equals(location.world())))
                throw new IllegalArgumentException("NPC, posisi berdiri, dan pemicu harus di dunia yang sama.");
            if (enabled) validate(nodes, root, kind, model, quest);
        }
        /** Retains source compatibility with existing NPC setup/tests. */
        public Definition(String id, UUID token, String name, Kind kind, String model, String texture, String signature,
                          Point location, Point stand, double eyeHeight, WhisperArea trigger, String root, Map<String, Node> nodes,
                          Reward gift, Quest quest, String repeat, int speed, boolean enabled) {
            this(id, token, name, kind, model, texture, signature, location, stand, eyeHeight, trigger, root,
                    nodes, gift, quest, repeat, speed, enabled, Facing.zero());
        }
        public Point facingLocation() {
            float yaw = ((location.yaw() + facing.yaw()) % 360 + 540) % 360 - 180;
            float pitch = Math.max(-90, Math.min(90, location.pitch() + facing.pitch()));
            return new Point(location.world(), location.x(), location.y(), location.z(), yaw, pitch);
        }
        public static Definition create(String id, String name, Point location, Point stand) {
            return new Definition(id, UUID.randomUUID(), name, Kind.HUMAN, "", "", "", location, stand,
                    1.62, null, "start", Map.of("start", new Node("Halo, ada yang bisa kubantu?", List.of())),
                    Reward.none(), Quest.none(), "Aku sudah memberikan hadiah kepadamu. Pergi sana, kamu mencari bansos lagi?", 1, false);
        }
    }
    private static void validate(Map<String, Node> nodes, String root, Kind kind, String model, Quest quest) {
        if (kind == Kind.MODEL && model.isBlank()) throw new IllegalArgumentException("ID model belum diatur.");
        for (Node node : nodes.values()) for (Choice c : node.available()) {
            if (c.target().equals("QUEST")) {
                if (quest.kind() == QuestKind.NONE || quest.reward().empty())
                    throw new IllegalArgumentException("Pilihan QUEST memerlukan quest dan hadiah.");
            } else if (!c.target().equals("END") && !nodes.containsKey(c.target()))
                throw new IllegalArgumentException("Tujuan pilihan tidak ditemukan: " + c.target());
        }
        Set<String> exits = new HashSet<>();
        boolean changed;
        do {
            changed = false;
            for (var entry : nodes.entrySet()) if (!exits.contains(entry.getKey())
                    && entry.getValue().available().stream().anyMatch(c -> c.target().equals("END")
                    || c.target().equals("QUEST") || exits.contains(c.target()))) changed |= exits.add(entry.getKey());
        } while (changed);
        if (exits.size() != nodes.size() || !exits.contains(root))
            throw new IllegalArgumentException("Setiap halaman harus memiliki jalan menuju END atau QUEST.");
    }
    public record Quota(int count, long until) {
        public Quota {
            if (count < 0 || count > 3 || until < 0 || (count < 3 && until != 0) || (count == 3 && until == 0))
                throw new IllegalArgumentException("Data cooldown tidak valid.");
        }
        public Quota effective(long now) { return count == 3 && now >= until ? new Quota(0, 0) : this; }
        public Quota complete(long now) {
            Quota old = effective(now);
            if (old.count == 3) throw new IllegalStateException("Masih cooldown.");
            return old.count == 2 ? new Quota(3, Math.addExact(now, 1_200_000)) : new Quota(old.count + 1, 0);
        }
    }
    public record Award(UUID receipt, UUID owner, String npcName, Reward reward) {
        public Award { Objects.requireNonNull(receipt); Objects.requireNonNull(owner);
            npcName = text(npcName, 64, "Nama NPC"); Objects.requireNonNull(reward); }
    }
    public record State(int version, Map<String, Definition> npcs, Map<UUID, Set<UUID>> completed,
                        Map<UUID, Quota> quotas, Map<UUID, Award> pending) {
        public State {
            if (version != 1 || npcs.size() > 128) throw new IllegalArgumentException("Versi data atau jumlah NPC tidak valid.");
            npcs = Map.copyOf(npcs); quotas = Map.copyOf(quotas); pending = Map.copyOf(pending);
            var progress = new HashMap<UUID, Set<UUID>>();
            completed.forEach((player, tokens) -> progress.put(Objects.requireNonNull(player), Set.copyOf(tokens)));
            completed = Map.copyOf(progress);
            npcs.forEach((key, value) -> { if (!key.equals(value.id())) throw new IllegalArgumentException("ID NPC berbeda."); });
            pending.forEach((key, value) -> { if (!key.equals(value.owner())) throw new IllegalArgumentException("Pemilik hadiah berbeda."); });
        }
        public static State empty() { return new State(1, Map.of(), Map.of(), Map.of(), Map.of()); }
        public Quota quota(UUID player, long now) { return quotas.getOrDefault(player, new Quota(0, 0)).effective(now); }
        public boolean done(UUID player, Definition npc) { return completed.getOrDefault(player, Set.of()).contains(npc.token()); }
        public State put(Definition d) { var copy = new HashMap<>(npcs); copy.put(d.id(), d); return new State(1, copy, completed, quotas, pending); }
        public State remove(String id) { var copy = new HashMap<>(npcs); copy.remove(id); return new State(1, copy, completed, quotas, pending); }
        public State heard(UUID player, Definition npc) {
            var copy = new HashMap<>(completed); var tokens = new HashSet<>(copy.getOrDefault(player, Set.of()));
            tokens.add(npc.token()); copy.put(player, tokens); return new State(1, npcs, copy, quotas, pending);
        }
        public State finish(UUID player, Definition npc, boolean quest, UUID receipt, long now) {
            if (pending.containsKey(player)) throw new IllegalStateException("Hadiah sebelumnya belum dikirim.");
            if (!quest && done(player, npc)) return this;
            State heard = heard(player, npc);
            var q = new HashMap<>(quotas); if (quest) q.put(player, quota(player, now).complete(now));
            var awards = new HashMap<>(pending);
            Reward reward = quest ? npc.quest().reward() : npc.gift();
            if (!reward.empty()) awards.put(player, new Award(receipt, player, npc.name(), reward));
            return new State(1, npcs, heard.completed, q, awards);
        }
        public State acknowledge(UUID player, UUID receipt) {
            Award a = pending.get(player); if (a == null || !a.receipt().equals(receipt)) return this;
            var copy = new HashMap<>(pending); copy.remove(player); return new State(1, npcs, completed, quotas, copy);
        }
        public State reset(UUID player, Definition npc) {
            if (pending.containsKey(player)) throw new IllegalStateException("Tunggu hadiah terkirim sebelum reset.");
            var c = new HashMap<>(completed); var tokens = new HashSet<>(c.getOrDefault(player, Set.of()));
            tokens.remove(npc.token()); c.put(player, tokens);
            var q = new HashMap<>(quotas); q.remove(player); return new State(1, npcs, c, q, pending);
        }
    }
    public static String id(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]{1,48}")) throw new IllegalArgumentException("ID harus 1–48 huruf, angka, _ atau -.");
        return value;
    }
    public static String text(String value, int limit, String field) {
        if (value == null || value.isBlank() || value.length() > limit) throw new IllegalArgumentException(field + " kosong atau terlalu panjang.");
        return value;
    }
    public static boolean inside(WhisperArea a, Point p) {
        return a.world().equals(p.world()) && p.x() >= a.minX() && p.x() < a.maxX() + 1.0
                && p.z() >= a.minZ() && p.z() < a.maxZ() + 1.0 && p.y() >= a.minY() - 1 && p.y() < a.maxY() + 3.0;
    }
    public static boolean overlaps(WhisperArea a, WhisperArea b) {
        return a.world().equals(b.world()) && a.minX() <= b.maxX() && b.minX() <= a.maxX()
                && a.minZ() <= b.maxZ() && b.minZ() <= a.maxZ() && a.minY() - 1 <= b.maxY() + 3 && b.minY() - 1 <= a.maxY() + 3;
    }
}
'@
    },
    [PSCustomObject]@{
        Name = 'npc/NpcStore.java'
        Hash = 'd21958085c2a7742b015033bd9df372b213f87fe3d047945ed4e57043c695b65'
        Source = @'
package com.bangzachery.vitae.vitaemanager.npc;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import java.io.*;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Invalid files are rejected, never replaced with empty state. */
public final class NpcStore {
    public static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Path file;
    public NpcStore(Path file) { this.file = file; }
    public NpcData.State initialize() throws IOException {
        if (!Files.exists(file)) { var state = NpcData.State.empty(); save(state); return state; }
        if (Files.size(file) > 16_000_000) throw new IOException("npc.json melebihi 16 MB.");
        try (var reader = new JsonReader(Files.newBufferedReader(file, StandardCharsets.UTF_8))) {
            reader.setStrictness(Strictness.STRICT);
            JsonElement json = JsonParser.parseReader(reader);
            if (reader.peek() != com.google.gson.stream.JsonToken.END_DOCUMENT) throw new IllegalArgumentException("Data tambahan setelah JSON.");
            validate(json, NpcData.State.class);
            return Objects.requireNonNull(JSON.fromJson(json, NpcData.State.class));
        } catch (RuntimeException e) { throw new IOException("npc.json tidak valid. File lama dipertahankan.", e); }
    }
    public void save(NpcData.State state) throws IOException {
        byte[] bytes = JSON.toJson(state).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 16_000_000) throw new IOException("Data NPC melebihi 16 MB.");
        Path parent = file.toAbsolutePath().getParent(); Files.createDirectories(parent);
        Path temp = Files.createTempFile(parent, "npc-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes); while (buffer.hasRemaining()) channel.write(buffer); channel.force(true);
            }
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
    private static void validate(JsonElement value, Type type) {
        if (value == null || value.isJsonNull()) {
            if (type == NpcData.Point.class || type == com.bangzachery.vitae.vitaemanager.whisper.WhisperArea.class) return;
            throw new IllegalArgumentException("Nilai wajib tidak boleh kosong: " + type);
        }
        if (type instanceof ParameterizedType p) {
            Type[] args = p.getActualTypeArguments();
            if (p.getRawType() == Map.class) {
                if (!value.isJsonObject()) throw new IllegalArgumentException("Map harus berupa object.");
                for (var entry : value.getAsJsonObject().entrySet()) {
                    if (args[0] == UUID.class) UUID.fromString(entry.getKey());
                    validate(entry.getValue(), args[1]);
                }
            } else {
                if (!value.isJsonArray()) throw new IllegalArgumentException("List harus berupa array.");
                for (JsonElement child : value.getAsJsonArray()) validate(child, args[0]);
            }
            return;
        }
        Class<?> c = (Class<?>) type;
        if (c.isRecord()) {
            if (!value.isJsonObject()) throw new IllegalArgumentException("Record harus berupa object.");
            for (RecordComponent field : c.getRecordComponents()) {
                // Older npc.json versions have no orientation offset; their default is zero.
                if (c == NpcData.Definition.class && field.getName().equals("facing") && !value.getAsJsonObject().has("facing")) continue;
                validate(value.getAsJsonObject().get(field.getName()), field.getGenericType());
            }
        } else if (c == boolean.class) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("Harus boolean.");
        } else if (c == int.class || c == long.class || c == float.class || c == double.class || c == java.math.BigDecimal.class) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Harus angka.");
            var number = value.getAsBigDecimal();
            if (c == int.class) number.intValueExact(); if (c == long.class) number.longValueExact();
        } else {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("Harus teks.");
            if (c == UUID.class) UUID.fromString(value.getAsString());
        }
    }
}
'@
    },
    [PSCustomObject]@{
        Name = 'npc/NpcRenderer.java'
        Hash = '27e727bb4fbcefd077b4cd3bd8a63a503d276dcb5822d1872f423246c992bafb'
        Source = @'
package com.bangzachery.vitae.vitaemanager.npc;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Level;

/** Native humanoid packets for Paper 1.21.4; ModelEngine is an optional adapter. */
public final class NpcRenderer implements AutoCloseable {
    @FunctionalInterface public interface Input { void click(Player player, String npc, boolean attack); }
    private record Visible(NpcData.Definition definition, Interaction hitbox, int avatarId, UUID avatarUuid, ArmorStand base,
                           Object modeled, Set<UUID> viewers, Set<UUID> failed) {}
    private final JavaPlugin plugin;
    private final NamespacedKey key;
    private final Input input;
    private final Map<String, Visible> entities = new HashMap<>();
    private final Map<Integer, Visible> avatars = new HashMap<>();
    private final Listener modelListener = new Listener() {};
    private final Set<Chunk> tickets = new HashSet<>();
    public NpcRenderer(JavaPlugin plugin, Input input) {
        this.plugin = plugin; this.input = input; key = new NamespacedKey(plugin, "npc_id");
        hookModel();
    }
    public static Location location(NpcData.Point p) {
        World world = Bukkit.getWorld(p.world());
        if (world == null) throw new IllegalArgumentException("Dunia NPC belum dimuat.");
        return new Location(world, p.x(), p.y(), p.z(), p.yaw(), p.pitch());
    }
    public static NpcData.Point point(Location p) {
        return new NpcData.Point(p.getWorld().getUID(), p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch());
    }
    public String id(Entity entity) { return entity.getPersistentDataContainer().get(key, PersistentDataType.STRING); }
    /** Only accept packets for a humanoid avatar actually shown to this player. */
    public String id(Player player, int entityId) {
        Visible visible = avatars.get(entityId);
        return visible != null && visible.viewers().contains(player.getUniqueId())
                && player.getWorld().getUID().equals(visible.definition().location().world()) ? visible.definition().id() : null;
    }
    @SuppressWarnings("deprecation")
    private static int nextAvatarId() { return Bukkit.getUnsafe().nextEntityId(); }
    public void refresh(Map<String, NpcData.Definition> definitions) {
        for (String id : List.copyOf(entities.keySet())) {
            NpcData.Definition next = definitions.get(id);
            if (next == null || !next.enabled() || !next.equals(entities.get(id).definition())) remove(id);
        }
        for (var d : definitions.values()) if (d.enabled() && !entities.containsKey(d.id()) && Bukkit.getWorld(d.location().world()) != null) {
            Interaction box = null; ArmorStand base = null; Object modeled = null;
            try {
                Location at = location(d.facingLocation());
                Chunk chunk = at.getChunk(); chunk.addPluginChunkTicket(plugin); tickets.add(chunk);
                box = at.getWorld().spawn(at, Interaction.class, e -> {
                    e.setPersistent(false); e.setInteractionWidth(d.kind() == NpcData.Kind.HUMAN ? .8f : 1.5f);
                    e.setInteractionHeight((float) Math.max(1.8, d.eyeHeight() + .2));
                    e.getPersistentDataContainer().set(key, PersistentDataType.STRING, d.id());
                });
                if (d.kind() == NpcData.Kind.MODEL) {
                    if (!Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) throw new IllegalArgumentException("ModelEngine belum aktif.");
                    base = at.getWorld().spawn(at, ArmorStand.class, e -> {
                        e.setVisible(false); e.setMarker(true); e.setGravity(false); e.setInvulnerable(true);
                        e.setPersistent(false); e.getPersistentDataContainer().set(key, PersistentDataType.STRING, d.id());
                    });
                    Class<?> api = Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
                    Class<?> modelType = Class.forName("com.ticxo.modelengine.api.model.ModeledEntity");
                    Class<?> activeType = Class.forName("com.ticxo.modelengine.api.model.ActiveModel");
                    modeled = api.getMethod("createModeledEntity", Entity.class).invoke(null, base);
                    modelType.getMethod("setSaved", boolean.class).invoke(modeled, false);
                    modelType.getMethod("setBaseEntityVisible", boolean.class).invoke(modeled, false);
                    modelType.getMethod("setModelRotationLocked", boolean.class).invoke(modeled, true);
                    Object active = api.getMethod("createActiveModel", String.class).invoke(null, d.model());
                    if (active == null) throw new IllegalArgumentException("ID ModelEngine tidak ditemukan: " + d.model());
                    modelType.getMethod("addModel", activeType, boolean.class).invoke(modeled, active, false);
                    modelRotation(modelType, modeled, "setYBodyRot", at.getYaw());
                    modelRotation(modelType, modeled, "setYHeadRot", at.getYaw());
                    modelRotation(modelType, modeled, "setXHeadRot", at.getPitch());
                }
                // Never disguise the real Interaction entity under the same network ID.
                // Cancelling a use packet can resend its metadata, which is not Player metadata.
                int avatarId = d.kind() == NpcData.Kind.HUMAN ? nextAvatarId() : -1;
                Visible visible = new Visible(d, box, avatarId, UUID.randomUUID(), base, modeled, new HashSet<>(), new HashSet<>());
                entities.put(d.id(), visible);
                if (d.kind() == NpcData.Kind.HUMAN) avatars.put(avatarId, visible);
            } catch (Exception e) {
                if (modeled != null) destroyModel(modeled); if (base != null) base.remove(); if (box != null) box.remove();
                releaseUnusedTickets();
                throw new IllegalStateException("NPC " + d.id() + " gagal ditampilkan. Periksa versi server/model.", e);
            }
        }
        releaseUnusedTickets();
    }
    private void releaseUnusedTickets() {
        Set<Chunk> used = new HashSet<>();
        for (Visible v : entities.values()) used.add(v.hitbox().getLocation().getChunk());
        for (Chunk chunk : Set.copyOf(tickets)) if (!used.contains(chunk)) {
            chunk.removePluginChunkTicket(plugin); tickets.remove(chunk);
        }
    }
    public void tick() {
        for (Visible v : entities.values()) {
            if (v.definition().kind() != NpcData.Kind.HUMAN) continue;
            Set<UUID> tracked = new HashSet<>();
            for (Player player : v.hitbox().getTrackedBy()) {
                tracked.add(player.getUniqueId());
                if (v.viewers().contains(player.getUniqueId()) || v.failed().contains(player.getUniqueId())) continue;
                try { show(player, v); v.viewers().add(player.getUniqueId()); }
                catch (Exception e) {
                    hide(player, v);
                    v.failed().add(player.getUniqueId());
                    plugin.getLogger().log(Level.SEVERE, "Paket NPC manusia gagal pada Paper 1.21.4: " + v.definition().id(), e);
                }
            }
            for (UUID old : Set.copyOf(v.viewers())) if (!tracked.contains(old)) {
                Player player = Bukkit.getPlayer(old); if (player != null) hide(player, v);
                v.viewers().remove(old);
            }
            v.failed().retainAll(tracked);
        }
    }
    public void forget(Player player) {
        for (Visible v : entities.values()) {
            if (v.viewers().remove(player.getUniqueId()) && player.isOnline()) hide(player, v);
            v.failed().remove(player.getUniqueId());
        }
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void show(Player player, Visible v) throws ReflectiveOperationException {
        var d = v.definition(); UUID uuid = v.avatarUuid(); int entityId = v.avatarId();
        Class<?> profileType = Class.forName("com.mojang.authlib.GameProfile");
        Object profile = profileType.getConstructor(UUID.class, String.class).newInstance(uuid, "VN_" + d.id().substring(0, Math.min(13, d.id().length())));
        if (!d.texture().isBlank()) {
            Class<?> property = Class.forName("com.mojang.authlib.properties.Property");
            Object texture = property.getConstructor(String.class, String.class, String.class)
                    .newInstance("textures", d.texture(), d.signature().isBlank() ? null : d.signature());
            Object properties = profileType.getMethod("getProperties").invoke(profile);
            Class.forName("com.google.common.collect.Multimap").getMethod("put", Object.class, Object.class).invoke(properties, "textures", texture);
        }
        String prefix = "net.minecraft.network.protocol.game.";
        Class<? extends Enum> action = (Class<? extends Enum>) Class.forName(prefix + "ClientboundPlayerInfoUpdatePacket$Action");
        EnumSet actions = EnumSet.noneOf(action);
        for (String name : List.of("ADD_PLAYER", "UPDATE_LISTED", "UPDATE_GAME_MODE", "UPDATE_HAT")) actions.add(Enum.valueOf(action, name));
        Class<?> game = Class.forName("net.minecraft.world.level.GameType");
        Class<?> component = Class.forName("net.minecraft.network.chat.Component");
        Class<?> chat = Class.forName("net.minecraft.network.chat.RemoteChatSession$Data");
        Class<?> entryType = Class.forName(prefix + "ClientboundPlayerInfoUpdatePacket$Entry");
        Object entry = entryType.getConstructor(UUID.class, profileType, boolean.class, int.class, game, component, boolean.class, int.class, chat)
                .newInstance(uuid, profile, false, 0, game.getField("SURVIVAL").get(null), null, true, 0, null);
        Class<?> infoType = Class.forName(prefix + "ClientboundPlayerInfoUpdatePacket"); Object info;
        try { info = infoType.getConstructor(EnumSet.class, entryType).newInstance(actions, entry); }
        catch (NoSuchMethodException e) {
            info = infoType.getConstructor(EnumSet.class, Collection.class).newInstance(actions, List.of());
            Field entries = infoType.getDeclaredField("entries"); entries.setAccessible(true); entries.set(info, List.of(entry));
        }
        send(player, info);
        Class<?> entityType = Class.forName("net.minecraft.world.entity.EntityType");
        Class<?> vector = Class.forName("net.minecraft.world.phys.Vec3");
        NpcData.Point pose = d.facingLocation();
        Object spawn = Class.forName(prefix + "ClientboundAddEntityPacket")
                .getConstructor(int.class, UUID.class, double.class, double.class, double.class, float.class, float.class, entityType, int.class, vector, double.class)
                .newInstance(entityId, uuid, pose.x(), pose.y(), pose.z(), pose.pitch(), pose.yaw(),
                        entityType.getField("PLAYER").get(null), 0, vector.getField("ZERO").get(null), (double) pose.yaw());
        send(player, spawn);
        Class<?> accessor = Class.forName("net.minecraft.network.syncher.EntityDataAccessor");
        Field skin = Class.forName("net.minecraft.world.entity.player.Player").getDeclaredField("DATA_PLAYER_MODE_CUSTOMISATION"); skin.setAccessible(true);
        Object value = Class.forName("net.minecraft.network.syncher.SynchedEntityData$DataValue")
                .getMethod("create", accessor, Object.class).invoke(null, skin.get(null), (byte) 127);
        send(player, Class.forName(prefix + "ClientboundSetEntityDataPacket").getConstructor(int.class, List.class).newInstance(entityId, List.of(value)));
    }
    private static void send(Player player, Object packet) throws ReflectiveOperationException {
        Object handle = player.getClass().getMethod("getHandle").invoke(player);
        Object connection = handle.getClass().getField("connection").get(handle);
        connection.getClass().getMethod("send", Class.forName("net.minecraft.network.protocol.Packet")).invoke(connection, packet);
    }
    private static void modelRotation(Class<?> type, Object model, String setter, float angle) throws ReflectiveOperationException {
        Method method;
        try { method = type.getMethod(setter + "Immediately", float.class); }
        catch (NoSuchMethodException e) { method = type.getMethod(setter, float.class); }
        method.invoke(model, angle);
    }
    private void hide(Player p, Visible v) {
        try {
            send(p, Class.forName("net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket")
                    .getConstructor(int[].class).newInstance((Object) new int[]{v.avatarId()}));
            send(p, Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket")
                    .getConstructor(List.class).newInstance(List.of(v.avatarUuid())));
        }
        catch (ReflectiveOperationException e) { plugin.getLogger().fine("Profil NPC sudah tidak tersedia bagi " + p.getName()); }
    }
    @SuppressWarnings("unchecked") private void hookModel() {
        if (!Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) return;
        try {
            Class<?> eventType = Class.forName("com.ticxo.modelengine.api.events.BaseEntityInteractEvent");
            Class<?> baseType = Class.forName("com.ticxo.modelengine.api.entity.BaseEntity");
            Bukkit.getPluginManager().registerEvent((Class<? extends Event>) eventType, modelListener, EventPriority.LOWEST, (listener, event) -> {
                try {
                    Player p = (Player) eventType.getMethod("getPlayer").invoke(event);
                    boolean attack = eventType.getMethod("getAction").invoke(event).toString().equals("ATTACK");
                    if (!attack && eventType.getMethod("getSlot").invoke(event) == EquipmentSlot.OFF_HAND) return;
                    Object base = eventType.getMethod("getBaseEntity").invoke(event);
                    UUID uuid = (UUID) baseType.getMethod("getUUID").invoke(base);
                    Runnable dispatch = () -> { for (Visible v : entities.values()) if (v.base() != null && v.base().getUniqueId().equals(uuid)) {
                        if (event instanceof Cancellable cancellable) cancellable.setCancelled(true);
                        input.click(p, v.definition().id(), attack); break;
                    } };
                    if (event.isAsynchronous()) Bukkit.getScheduler().runTask(plugin, dispatch); else dispatch.run();
                } catch (ReflectiveOperationException e) { throw new org.bukkit.event.EventException(e); }
            }, plugin, false);
        } catch (ReflectiveOperationException | LinkageError e) {
            plugin.getLogger().log(Level.WARNING, "Pengait klik ModelEngine tidak tersedia; NPC manusia dan hitbox Vitae tetap dapat dipakai.", e);
        }
    }
    private void destroyModel(Object model) {
        try {
            Class<?> type = Class.forName("com.ticxo.modelengine.api.model.ModeledEntity");
            Object base = type.getMethod("getBase").invoke(model);
            UUID uuid = (UUID) Class.forName("com.ticxo.modelengine.api.entity.BaseEntity").getMethod("getUUID").invoke(base);
            Class.forName("com.ticxo.modelengine.api.ModelEngineAPI").getMethod("removeModeledEntity", UUID.class).invoke(null, uuid);
            if (!(boolean) type.getMethod("isDestroyed").invoke(model)) type.getMethod("destroy").invoke(model);
        }
        catch (ReflectiveOperationException e) { plugin.getLogger().log(Level.WARNING, "Pembersihan model NPC gagal.", e); }
    }
    private void remove(String id) {
        Visible v = entities.remove(id); if (v == null) return;
        avatars.remove(v.avatarId());
        for (UUID viewer : v.viewers()) { Player p = Bukkit.getPlayer(viewer); if (p != null) hide(p, v); }
        if (v.modeled() != null) destroyModel(v.modeled()); if (v.base() != null) v.base().remove(); v.hitbox().remove();
    }
    @Override public void close() {
        for (String id : List.copyOf(entities.keySet())) remove(id);
        for (Chunk chunk : tickets) chunk.removePluginChunkTicket(plugin); tickets.clear(); HandlerList.unregisterAll(modelListener);
    }
}
'@
    },
    [PSCustomObject]@{
        Name = 'npc/NpcService.java'
        Hash = '38594bc7f74793f89c07d5392cb10e7570862a8d7eb1dab0ac61e2faf1ef7120'
        Source = @'
package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.core.gui.VitaeMenu;
import com.bangzachery.vitae.vitaemanager.economy.*;
import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class NpcService implements Listener, AutoCloseable {
    private static final class Session {
        final Player player; final NpcData.Definition npc; final boolean preview, special, trigger;
        final long deadline = System.currentTimeMillis() + 300_000;
        Location anchor; NpcData.Node node; TextDisplay display, blackout;
        int phase, age, shown, selected; float fromYaw, fromPitch, goalYaw, goalPitch;
        boolean blind;
        Session(Player player, NpcData.Definition npc, boolean preview, boolean special, boolean trigger) {
            this.player = player; this.npc = npc; this.preview = preview; this.special = special; this.trigger = trigger;
            anchor = player.getLocation().clone(); phase = trigger ? 0 : 1;
        }
    }
    private final JavaPlugin plugin;
    private final VitiService viti;
    private final NpcStore store;
    private NpcData.State state;
    private final NpcRenderer renderer;
    private final NpcQuest quests;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, Long> clicks = new HashMap<>(), warned = new HashMap<>();
    private final Map<UUID, BukkitTask> pushes = new HashMap<>();
    private final Set<UUID> internalTeleport = new HashSet<>(), paying = new HashSet<>();
    private final NamespacedKey awardKey;
    private BukkitTask task;
    private boolean closed;
    private long ticks;
    public NpcService(JavaPlugin plugin, VitiService viti) throws IOException {
        this.plugin = plugin; this.viti = viti; awardKey = new NamespacedKey(plugin, "npc_last_award");
        store = new NpcStore(plugin.getDataFolder().toPath().resolve("npc.json")); state = store.initialize();
        for (var d : state.npcs().values()) validateRuntime(d, false);
        for (var a : state.pending().values()) for (String bytes : a.reward().items()) decode(bytes);
        renderer = new NpcRenderer(plugin, this::modelClick);
        quests = new NpcQuest(plugin, () -> state.npcs().values(), this::questComplete, this::teleport);
        Bukkit.getPluginManager().registerEvents(quests, plugin);
    }
    public void start() {
        try {
            renderer.refresh(state.npcs());
            for (var d : state.npcs().values()) if (d.enabled()) NpcQuest.restore(d.quest());
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1, 1);
        } catch (RuntimeException e) { close(); throw e; }
    }
    public NpcData.State current() { return state; }
    public NpcData.Definition get(String id) {
        NpcData.Definition d = state.npcs().get(id);
        if (d == null) throw new IllegalArgumentException("NPC tidak ditemukan: " + id + ". /vnpc list"); return d;
    }
    public boolean inUse(String id) {
        return quests.uses(id) || sessions.values().stream().anyMatch(s -> s.npc.id().equals(id));
    }
    private void commit(NpcData.State next) throws IOException { store.save(next); state = next; }
    public void create(Player p, String id, String name) throws IOException {
        if (state.npcs().containsKey(id)) throw new IllegalArgumentException("ID NPC sudah digunakan.");
        Location stand = p.getLocation(); Vector forward = stand.getDirection().setY(0);
        if (forward.lengthSquared() < .001) forward = new Vector(0, 0, 1); else forward.normalize();
        Location at = stand.clone().add(forward.multiply(2)); at.setYaw(stand.getYaw() + 180); at.setPitch(0);
        commit(state.put(NpcData.Definition.create(id, name, NpcRenderer.point(at), NpcRenderer.point(stand))));
    }
    public void edit(String id, Consumer<JsonObject> change, boolean enable) throws IOException {
        NpcData.Definition old = get(id);
        if (inUse(id)) throw new IllegalStateException("NPC sedang dipakai. Selesaikan dialog/quest sebelum mengubahnya.");
        JsonObject json = NpcStore.JSON.toJsonTree(old).getAsJsonObject(); change.accept(json);
        json.addProperty("enabled", enable); NpcData.Definition next = NpcStore.JSON.fromJson(json, NpcData.Definition.class);
        if (!next.id().equals(old.id()) || !next.token().equals(old.token())) throw new IllegalArgumentException("Identitas NPC tidak boleh berubah.");
        validateRuntime(next);
        NpcData.State previous = state; commit(state.put(next));
        try { renderer.refresh(state.npcs()); if (next.enabled()) NpcQuest.restore(next.quest()); }
        catch (RuntimeException e) { commit(previous); renderer.refresh(previous.npcs()); throw e; }
    }
    private void validateRuntime(NpcData.Definition d) {
        validateRuntime(d, true);
    }
    private void validateRuntime(NpcData.Definition d, boolean requireWorld) {
        for (String item : d.gift().items()) decode(item);
        for (String item : d.quest().reward().items()) decode(item);
        if (d.quest().kind() == NpcData.QuestKind.MOB) NpcQuest.mobType(d.quest().mob());
        for (var crop : d.quest().crops()) {
            var plant = Bukkit.createBlockData(crop.plant()); var soil = Bukkit.createBlockData(crop.soil());
            if (!(plant instanceof org.bukkit.block.data.Ageable) || !Set.of(Material.CARROTS, Material.POTATOES, Material.WHEAT, Material.BEETROOTS).contains(plant.getMaterial())
                    || soil.getMaterial() != Material.FARMLAND) throw new IllegalArgumentException("Snapshot ladang tidak valid.");
        }
        if (requireWorld && d.enabled() && (Bukkit.getWorld(d.location().world()) == null || Bukkit.getWorld(d.stand().world()) == null))
            throw new IllegalArgumentException("Dunia NPC belum dimuat.");
    }
    public void delete(String id) throws IOException {
        get(id); if (inUse(id)) throw new IllegalStateException("NPC sedang dipakai."); commit(state.remove(id)); renderer.refresh(state.npcs());
    }
    public void reset(UUID player, String id) throws IOException {
        if (quests.run(player) != null || sessions.containsKey(player)) throw new IllegalStateException("Pemain sedang dialog/quest.");
        commit(state.reset(player, get(id))); warned.remove(player);
    }
    public void preview(Player p, String id) {
        JsonObject json = NpcStore.JSON.toJsonTree(get(id)).getAsJsonObject(); json.addProperty("enabled", true);
        NpcData.Definition checked = NpcStore.JSON.fromJson(json, NpcData.Definition.class);
        validateRuntime(checked); begin(p, checked, false, true, null);
    }
    public void skin(Player admin, String id, String name) {
        NpcData.Definition before = get(id);
        Player online = Bukkit.getPlayerExact(name);
        com.destroystokyo.paper.profile.PlayerProfile profile = online == null ? Bukkit.createProfile(name) : online.getPlayerProfile();
        Consumer<com.destroystokyo.paper.profile.PlayerProfile> apply = loaded -> {
            if (closed || !admin.isOnline() || !admin.hasPermission("vitae.admin.npc")) return;
            try {
                if (!get(id).equals(before)) throw new IllegalStateException("NPC berubah saat mengambil skin; ulangi perintah.");
                var texture = loaded.getProperties().stream().filter(v -> v.getName().equals("textures")).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Skin tidak ditemukan. Coba nama akun premium atau pemain online."));
                edit(id, json -> { json.addProperty("kind", "HUMAN"); json.addProperty("texture", texture.getValue());
                    json.addProperty("signature", Objects.requireNonNullElse(texture.getSignature(), "")); }, false);
                message(admin, "Skin tersimpan. Jalankan /vnpc " + id + " on setelah setup selesai.");
            } catch (Exception e) { message(admin, e.getMessage()); }
        };
        if (profile.hasTextures()) apply.accept(profile);
        else profile.update().whenComplete((loaded, error) -> {
            if (!closed) Bukkit.getScheduler().runTask(plugin, () -> {
                if (error != null || !(loaded instanceof com.destroystokyo.paper.profile.PlayerProfile paper)) message(admin, "Pengambilan skin gagal; coba lagi nanti.");
                else apply.accept(paper);
            });
        });
    }
    public void quitQuest(Player p) { quests.cancel(p.getUniqueId(), "Quest dibatalkan. Percobaan gagal tidak mengurangi kuota.", true); }
    public void reloadModule() throws IOException {
        if (state.npcs().keySet().stream().anyMatch(this::inUse)) throw new IllegalStateException("Selesaikan dialog/quest aktif sebelum reload NPC.");
        NpcModule.stop(plugin); NpcModule.start(plugin, viti);
    }
    public void status(Player p) {
        NpcQuest.Run r = quests.run(p.getUniqueId()); var quota = state.quota(p.getUniqueId(), System.currentTimeMillis());
        message(p, r == null ? "Tidak ada quest aktif. Selesai: " + quota.count() + "/3."
                : "Quest " + r.npc.name() + ": " + r.count + "/" + r.npc.quest().goal());
        if (quota.count() == 3) message(p, "Cooldown tersisa " + Math.max(1, (quota.until() - System.currentTimeMillis() + 999) / 1000) + " detik.");
    }
    private void tick() {
        if (closed) return; ticks++;
        if (ticks % 10 == 0) renderer.tick(); quests.tick();
        for (Session s : List.copyOf(sessions.values())) {
            if (!s.player.isOnline() || s.player.isDead() || System.currentTimeMillis() >= s.deadline) { end(s, false); continue; }
            if (s.phase == 0) {
                if (++s.age < 12) continue;
                removeBlindness(s); if (s.blackout != null) { s.blackout.remove(); s.blackout = null; }
                if (!teleport(s.player, NpcRenderer.location(s.npc.stand()))) { end(s, true); continue; }
                s.anchor = s.player.getLocation().clone(); s.phase = 1; s.age = 0; aim(s);
            } else if (s.phase == 1) {
                float t = Math.min(1, ++s.age / 12f);
                s.anchor.setYaw(s.fromYaw + shortest(s.goalYaw - s.fromYaw) * t);
                s.anchor.setPitch(s.fromPitch + (s.goalPitch - s.fromPitch) * t);
                if (!teleport(s.player, s.anchor)) { end(s, false); continue; }
                if (s.age >= 12) { s.phase = 2; s.age = 0; display(s); }
            } else if (ticks % 2 == 0) {
                int length = s.node.text().codePointCount(0, s.node.text().length());
                if (s.shown < length) { s.shown = Math.min(length, s.shown + s.npc.speed()); sound(s.player, Sound.BLOCK_NOTE_BLOCK_HAT, .15f, 1.4f); }
                render(s);
            }
        }
        if (ticks % 20 == 0) for (Player p : Bukkit.getOnlinePlayers()) pay(p);
    }
    private static float shortest(float angle) { return (angle % 360 + 540) % 360 - 180; }
    private void aim(Session s) {
        s.fromYaw = s.anchor.getYaw(); s.fromPitch = s.anchor.getPitch();
        Vector delta = NpcRenderer.location(s.npc.location()).add(0, s.npc.eyeHeight(), 0).toVector()
                .subtract(s.anchor.clone().add(0, s.player.getEyeHeight(), 0).toVector());
        Location look = s.anchor.clone().setDirection(delta); s.goalYaw = look.getYaw(); s.goalPitch = look.getPitch();
    }
    private void display(Session s) {
        Location at = s.anchor.clone().add(0, s.player.getEyeHeight(), 0).add(s.anchor.getDirection().multiply(1.5)).add(0, -.4, 0);
        s.display = at.getWorld().spawn(at, TextDisplay.class, e -> {
            e.setPersistent(false); e.setVisibleByDefault(false); e.setGravity(false); e.setInvulnerable(true);
            e.setBillboard(Display.Billboard.CENTER); e.setSeeThrough(true); e.setShadowed(true); e.setLineWidth(230);
            e.setBackgroundColor(Color.fromARGB(210, 10, 10, 15));
            var transform = e.getTransformation(); transform.getScale().set(new Vector3f(.30f)); e.setTransformation(transform);
        });
        s.player.showEntity(plugin, s.display); render(s);
    }
    private void render(Session s) {
        if (s.display == null) return;
        int total = s.node.text().codePointCount(0, s.node.text().length()); int end = s.node.text().offsetByCodePoints(0, s.shown);
        Component text = Component.text(s.npc.name() + "\n", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text(s.node.text().substring(0, end), NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false));
        if (s.shown >= total) {
            List<NpcData.Choice> choices = s.node.available(); int start = Math.max(0, Math.min(s.selected - 1, choices.size() - 3));
            for (int i = start; i < Math.min(choices.size(), start + 3); i++) text = text.append(Component.text("\n" + (i == s.selected ? "▶ " : "  ") + choices.get(i).label(), i == s.selected ? NamedTextColor.YELLOW : NamedTextColor.GRAY));
            s.player.sendActionBar(Component.text("Scroll: pilih • Klik kiri/kanan: pilih • Shift: batal", NamedTextColor.GRAY));
        } else s.player.sendActionBar(Component.text("Dengarkan dialog... • Shift: batal", NamedTextColor.GRAY));
        s.display.text(text);
    }
    private boolean begin(Player p, NpcData.Definition d, boolean trigger, boolean preview, String specialText) {
        if (closed || pushes.containsKey(p.getUniqueId()) || (!preview && !d.enabled()) || (!preview && !p.hasPermission("vitae.npc"))
                || p.isDead() || p.getGameMode() == GameMode.SPECTATOR || p.getVehicle() != null || !p.getPassengers().isEmpty()
                || sessions.containsKey(p.getUniqueId()) || quests.run(p.getUniqueId()) != null
                || p.getOpenInventory().getTopInventory().getHolder() instanceof RewardMenu) return false;
        if (!p.getWorld().getUID().equals(d.location().world())) return false;
        if (!trigger && !preview && p.getLocation().distanceSquared(NpcRenderer.location(d.location())) > 25) return false;
        if (!preview && state.pending().containsKey(p.getUniqueId())) { message(p, "Hadiah sebelumnya sedang dikirim. Tunggu sebentar."); return false; }
        if (!preview && specialText == null) {
            var quota = state.quota(p.getUniqueId(), System.currentTimeMillis());
            if (d.quest().kind() != NpcData.QuestKind.NONE && quota.count() == 3) {
                if (Objects.equals(warned.get(p.getUniqueId()), quota.until())) { push(p, d); return false; }
                warned.put(p.getUniqueId(), quota.until()); specialText = "Kamu sudah menyelesaikan tiga quest. Istirahat dulu; kembali dalam "
                        + Math.max(1, (quota.until() - System.currentTimeMillis() + 999) / 1000) + " detik.";
            } else if (d.quest().kind() == NpcData.QuestKind.NONE && state.done(p.getUniqueId(), d)) specialText = d.repeat();
        }
        Session s = new Session(p, d, preview, specialText != null, trigger);
        s.node = specialText == null ? d.nodes().get(d.root()) : new NpcData.Node(specialText, List.of());
        sessions.put(p.getUniqueId(), s);
        if (trigger) {
            Location blackAt = p.getEyeLocation().add(p.getEyeLocation().getDirection());
            s.blackout = blackAt.getWorld().spawn(blackAt, TextDisplay.class, e -> {
                e.setPersistent(false); e.setVisibleByDefault(false); e.setBillboard(Display.Billboard.CENTER);
                e.setSeeThrough(true); e.setBackgroundColor(Color.fromARGB(255, 0, 0, 0));
                e.text(Component.text("                                        \n                                        \n                                        "));
                var t = e.getTransformation(); t.getScale().set(new Vector3f(100)); e.setTransformation(t);
            });
            p.showEntity(plugin, s.blackout);
            if (!p.hasPotionEffect(PotionEffectType.BLINDNESS)) { p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0, false, false)); s.blind = true; }
        } else aim(s);
        return true;
    }
    private void modelClick(Player p, String id, boolean attack) {
        if (sessions.containsKey(p.getUniqueId())) confirm(p); else click(p, id);
    }
    private void click(Player p, String id) {
        if (ticks - clicks.getOrDefault(p.getUniqueId(), -100L) < 6) return; clicks.put(p.getUniqueId(), ticks);
        begin(p, get(id), false, false, null);
    }
    private void confirm(Player p) {
        Session s = sessions.get(p.getUniqueId());
        if (s == null || s.phase != 2 || s.shown < s.node.text().codePointCount(0, s.node.text().length())
                || ticks - clicks.getOrDefault(p.getUniqueId(), -100L) < 4) return;
        clicks.put(p.getUniqueId(), ticks); NpcData.Choice c = s.node.available().get(s.selected);
        sound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, .4f, 1.3f);
        if (c.target().equals("END")) {
            try {
                if (!s.preview && !s.special) commit(state.finish(p.getUniqueId(), s.npc, false, UUID.randomUUID(), System.currentTimeMillis()));
                end(s, false); pay(p);
            } catch (IOException e) { message(p, "Progres belum tersimpan; coba pilih kembali."); log(e); }
        } else if (c.target().equals("QUEST")) {
            if (s.preview) { message(p, "Preview selesai. Quest dan hadiah tidak dijalankan saat preview."); end(s, false); return; }
            String refusal = p.hasPermission("vitae.quest") ? quests.refusal(p, s.npc) : "Kamu tidak memiliki izin quest.";
            var quota = state.quota(p.getUniqueId(), System.currentTimeMillis()); if (quota.count() == 3) refusal = "Kamu masih cooldown.";
            if (refusal != null) { page(s, new NpcData.Node(refusal, List.of())); return; }
            end(s, false);
            if (quests.start(p, s.npc)) {
                try { commit(state.heard(p.getUniqueId(), s.npc)); }
                catch (IOException e) { quests.cancel(p.getUniqueId(), "Progres gagal disimpan; coba kembali.", true); log(e); }
            }
        } else page(s, s.npc.nodes().get(c.target()));
    }
    private void page(Session s, NpcData.Node node) { s.node = node; s.shown = 0; s.selected = 0; render(s); }
    private void end(Session s, boolean sound) {
        sessions.remove(s.player.getUniqueId()); removeBlindness(s); if (s.blackout != null) s.blackout.remove(); if (s.display != null) s.display.remove();
        s.player.sendActionBar(Component.empty()); if (sound) sound(s.player, Sound.ENTITY_VILLAGER_NO, .45f, 1);
    }
    private void removeBlindness(Session s) {
        if (s.blind) { var effect = s.player.getPotionEffect(PotionEffectType.BLINDNESS);
            if (effect != null && effect.getAmplifier() == 0 && effect.getDuration() <= 30) s.player.removePotionEffect(PotionEffectType.BLINDNESS); s.blind = false; }
    }
    private void questComplete(NpcQuest.Run run) {
        Player p = Bukkit.getPlayer(run.player); if (p == null) { quests.cancel(run.player, "", false); return; }
        try {
            commit(state.finish(run.player, run.npc, true, UUID.randomUUID(), System.currentTimeMillis()));
            quests.finish(run); teleport(p, NpcRenderer.location(run.npc.stand()));
            begin(p, run.npc, false, true, "Kerja bagus! Kamu telah menyelesaikan questku."); pay(p);
        } catch (IOException | RuntimeException e) { log(e); quests.retry(run); message(p, "Penyimpanan hadiah gagal; gelombang terakhir akan diulang."); }
    }
    private boolean teleport(Player p, Location location) {
        internalTeleport.add(p.getUniqueId());
        try { return p.teleport(location); } finally { internalTeleport.remove(p.getUniqueId()); }
    }
    private void push(Player p, NpcData.Definition d) {
        message(p, d.name() + ": Sudah kubilang istirahat dulu! Hus, hus!" );
        Location from = p.getLocation(); Vector dir = from.toVector().subtract(NpcRenderer.location(d.location()).toVector()).setY(0);
        if (dir.lengthSquared() < .001) dir = from.getDirection().setY(0);
        if (dir.lengthSquared() < .001) dir = new Vector(0, 0, 1); dir.normalize(); final Vector away = dir;
        final int[] count = {0}; final BukkitTask[] animation = new BukkitTask[1];
        animation[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (closed || !p.isOnline() || p.isDead() || ++count[0] > 12 || sessions.containsKey(p.getUniqueId()) || quests.run(p.getUniqueId()) != null) { animation[0].cancel(); pushes.remove(p.getUniqueId()); return; }
            double t = count[0] / 12.0; Location to = from.clone().add(away.clone().multiply(3 * t)).add(0, .8 * Math.sin(Math.PI * t), 0);
            if (!to.getBlock().isPassable() || !to.clone().add(0, 1, 0).getBlock().isPassable() || !teleport(p, to)) { animation[0].cancel(); pushes.remove(p.getUniqueId()); return; }
            p.setFallDistance(0);
        }, 1, 1);
        pushes.put(p.getUniqueId(), animation[0]);
    }
    private void pay(Player p) {
        var award = state.pending().get(p.getUniqueId()); if (award == null || !paying.add(p.getUniqueId())) return;
        if (award.reward().viti().signum() > 0) {
            viti.reward(p, award.receipt(), award.reward().viti(), key -> {
                try { if (!closed && key.equals("viti-success")) {
                    if (p.isOnline()) notifyAward(p, award); commit(state.acknowledge(p.getUniqueId(), award.receipt()));
                } } catch (Exception e) { log(e); } finally { paying.remove(p.getUniqueId()); }
            });
        } else {
            ItemStack[] before = p.getInventory().getStorageContents(); var dropped = new ArrayList<Item>();
            String marker = p.getPersistentDataContainer().get(awardKey, PersistentDataType.STRING);
            try {
                if (!award.receipt().toString().equals(marker)) {
                    ItemStack[] items = award.reward().items().stream().map(NpcService::decode).toArray(ItemStack[]::new);
                    for (ItemStack item : p.getInventory().addItem(items).values()) dropped.add(p.getWorld().dropItem(p.getLocation(), item));
                    notifyAward(p, award);
                }
                commit(state.acknowledge(p.getUniqueId(), award.receipt()));
            } catch (RuntimeException e) {
                p.getInventory().setStorageContents(before); for (Item item : dropped) item.remove();
                if (marker == null) p.getPersistentDataContainer().remove(awardKey); else p.getPersistentDataContainer().set(awardKey, PersistentDataType.STRING, marker);
                log(e);
            } catch (IOException e) { log(e); } finally { paying.remove(p.getUniqueId()); }
        }
    }
    private void notifyAward(Player p, NpcData.Award award) {
        if (award.receipt().toString().equals(p.getPersistentDataContainer().get(awardKey, PersistentDataType.STRING))) return;
        p.getPersistentDataContainer().set(awardKey, PersistentDataType.STRING, award.receipt().toString()); p.saveData();
        p.sendMessage(Component.text(award.npcName() + ": " + (award.reward().viti().signum() > 0
                ? "Aku memberikan " + VitiAmount.format(award.reward().viti()) + " Viti kepadamu." : "Hadiahku sudah diberikan. Item yang tidak muat jatuh di dekatmu.")));
        sound(p, Sound.ENTITY_PLAYER_LEVELUP, .4f, 1);
    }
    public static ItemStack decode(String bytes) {
        byte[] data = Base64.getDecoder().decode(bytes);
        if (data.length > 1_500_000) throw new IllegalArgumentException("Item terlalu besar.");
        ItemStack result = ItemStack.deserializeBytes(data); if (result.getType().isAir() || result.getAmount() <= 0) throw new IllegalArgumentException("Item hadiah kosong."); return result;
    }
    public void items(Player p, String id, boolean quest) {
        NpcData.Definition d = get(id);
        if (inUse(id)) throw new IllegalStateException("NPC sedang dipakai.");
        if (quest && (d.quest().kind() == NpcData.QuestKind.NONE || d.quest().kind() == NpcData.QuestKind.FARM)) throw new IllegalArgumentException("Hadiah ladang wajib Viti; konfigurasi quest mob dahulu.");
        p.openInventory(new RewardMenu(p.getUniqueId(), d, quest).inventory);
    }
    private static final class RewardMenu implements VitaeMenu {
        final UUID admin; final NpcData.Definition before; final boolean quest; final Inventory inventory;
        RewardMenu(UUID admin, NpcData.Definition before, boolean quest) {
            this.admin = admin; this.before = before; this.quest = quest;
            inventory = Bukkit.createInventory(this, 27, Component.text("Letakkan item hadiah • tutup: simpan"));
        }
        @Override public Inventory getInventory() { return inventory; }
    }
    @EventHandler public void menuClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof RewardMenu menu) || !(e.getPlayer() instanceof Player p)) return;
        var templates = new ArrayList<String>();
        for (ItemStack item : e.getInventory().getContents()) if (item != null && !item.getType().isAir()) {
            ItemStack copy = item.clone(); templates.add(Base64.getEncoder().encodeToString(copy.serializeAsBytes()));
            for (ItemStack overflow : p.getInventory().addItem(copy).values()) p.getWorld().dropItem(p.getLocation(), overflow);
        }
        e.getInventory().clear();
        if (closed) return;
        try {
            if (!p.getUniqueId().equals(menu.admin) || !p.hasPermission("vitae.admin.npc") || !get(menu.before.id()).equals(menu.before))
                throw new IllegalStateException("NPC atau izin berubah. Setup hadiah tidak disimpan.");
            NpcData.Reward reward = new NpcData.Reward(java.math.BigDecimal.ZERO, templates);
            edit(menu.before.id(), json -> { if (menu.quest) json.getAsJsonObject("quest").add("reward", NpcStore.JSON.toJsonTree(reward)); else json.add("gift", NpcStore.JSON.toJsonTree(reward)); }, false);
            message(p, "Template hadiah disimpan. Item admin dikembalikan. Jalankan /vnpc " + menu.before.id() + " on.");
        } catch (Exception exception) { message(p, exception.getMessage()); }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true) public void move(PlayerMoveEvent e) {
        if (internalTeleport.contains(e.getPlayer().getUniqueId())) return;
        Session s = sessions.get(e.getPlayer().getUniqueId());
        if (s != null) { e.setTo(s.anchor.clone()); return; }
        if (e.getTo() == null || quests.run(e.getPlayer().getUniqueId()) != null) return;
        for (var d : state.npcs().values()) if (d.enabled() && d.trigger() != null && !state.done(e.getPlayer().getUniqueId(), d)
                && (d.trigger().contains(e.getTo().getWorld().getUID(), e.getTo().getX(), e.getTo().getY(), e.getTo().getZ())
                || d.trigger().crossed(e.getFrom().getWorld().getUID(), e.getFrom().getX(), e.getFrom().getY(), e.getFrom().getZ(), e.getTo().getX(), e.getTo().getY(), e.getTo().getZ()))) {
            begin(e.getPlayer(), d, true, false, null); break;
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void externalTeleport(PlayerTeleportEvent e) {
        if (internalTeleport.contains(e.getPlayer().getUniqueId())) return;
        Session s = sessions.get(e.getPlayer().getUniqueId()); if (s != null) end(s, false);
        quests.cancel(e.getPlayer().getUniqueId(), "Teleport membatalkan quest.", false); renderer.forget(e.getPlayer());
    }
    @EventHandler(priority = EventPriority.LOWEST) public void held(PlayerItemHeldEvent e) {
        Session s = sessions.get(e.getPlayer().getUniqueId()); if (s == null) return; e.setCancelled(true);
        if (s.phase != 2 || s.shown < s.node.text().codePointCount(0, s.node.text().length())) return;
        int delta = Math.floorMod(e.getNewSlot() - e.getPreviousSlot() + 4, 9) - 4;
        if (delta == 0) return; s.selected = Math.floorMod(s.selected + (delta > 0 ? 1 : -1), s.node.available().size());
        sound(s.player, Sound.UI_BUTTON_CLICK, .3f, 1.2f); render(s);
    }
    @EventHandler(priority = EventPriority.LOWEST) public void sneak(PlayerToggleSneakEvent e) {
        Session s = sessions.get(e.getPlayer().getUniqueId()); if (s != null && e.isSneaking()) { e.setCancelled(true); end(s, true); }
    }
    @EventHandler(priority = EventPriority.LOWEST) public void swing(PlayerAnimationEvent e) {
        if (e.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        Player p = e.getPlayer(); if (sessions.containsKey(p.getUniqueId())) { e.setCancelled(true); confirm(p); return; }
        var hit = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getEyeLocation().getDirection(), 4.5, .1,
                entity -> entity != p && renderer.id(entity) != null);
        if (hit != null && hit.getHitEntity() != null) click(p, renderer.id(hit.getHitEntity()));
    }
    @EventHandler(priority = EventPriority.LOWEST) public void interact(PlayerInteractEvent e) {
        if (!sessions.containsKey(e.getPlayer().getUniqueId())) return; e.setCancelled(true);
        if (e.getHand() == EquipmentSlot.HAND && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) confirm(e.getPlayer());
    }
    @EventHandler(priority = EventPriority.LOWEST) public void entityInteract(PlayerInteractEntityEvent e) {
        String id = renderer.id(e.getRightClicked());
        boolean dialog = sessions.containsKey(e.getPlayer().getUniqueId());
        if (id == null && !dialog) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (dialog) confirm(e.getPlayer()); else click(e.getPlayer(), id);
    }
    @EventHandler(priority = EventPriority.LOWEST) public void entityInteractAt(PlayerInteractAtEntityEvent e) { entityInteract(e); }
    @EventHandler(priority = EventPriority.LOWEST) public void virtualInteract(PlayerUseUnknownEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        String id = renderer.id(e.getPlayer(), e.getEntityId());
        if (id != null) modelClick(e.getPlayer(), id, e.isAttack());
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void damage(EntityDamageEvent e) {
        if (renderer.id(e.getEntity()) != null || sessions.containsKey(e.getEntity().getUniqueId())) e.setCancelled(true);
        if (e instanceof EntityDamageByEntityEvent hit && hit.getDamager() instanceof Player p) {
            String id = renderer.id(e.getEntity()); if (id != null) { e.setCancelled(true); modelClick(p, id, true); }
            if (sessions.containsKey(p.getUniqueId())) e.setCancelled(true);
        }
    }
    @EventHandler(priority = EventPriority.LOWEST) public void inventory(InventoryClickEvent e) { if (sessions.containsKey(e.getWhoClicked().getUniqueId())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.LOWEST) public void drag(InventoryDragEvent e) { if (sessions.containsKey(e.getWhoClicked().getUniqueId())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.LOWEST) public void drop(PlayerDropItemEvent e) { if (sessions.containsKey(e.getPlayer().getUniqueId())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.LOWEST) public void swap(PlayerSwapHandItemsEvent e) { if (sessions.containsKey(e.getPlayer().getUniqueId())) e.setCancelled(true); }
    @EventHandler public void death(PlayerDeathEvent e) { disconnect(e.getEntity()); }
    @EventHandler public void quit(PlayerQuitEvent e) { disconnect(e.getPlayer()); clicks.remove(e.getPlayer().getUniqueId()); warned.remove(e.getPlayer().getUniqueId()); }
    @EventHandler public void respawn(PlayerRespawnEvent e) { renderer.forget(e.getPlayer()); }
    @EventHandler public void world(PlayerChangedWorldEvent e) { renderer.forget(e.getPlayer()); }
    @EventHandler public void worldLoad(WorldLoadEvent e) { renderer.refresh(state.npcs()); for (var d : state.npcs().values()) if (d.enabled() && !quests.uses(d.id())) NpcQuest.restore(d.quest()); }
    private void disconnect(Player p) { BukkitTask push = pushes.remove(p.getUniqueId()); if (push != null) push.cancel(); Session s = sessions.get(p.getUniqueId()); if (s != null) end(s, false); quests.cancel(p.getUniqueId(), "", false); renderer.forget(p); }
    private static void sound(Player p, Sound sound, float volume, float pitch) { p.playSound(p.getLocation(), sound, volume, pitch); }
    public static void message(Player p, String text) { p.sendMessage(Component.text("[Vitae NPC] " + Objects.requireNonNullElse(text, "Setup gagal."), NamedTextColor.YELLOW)); }
    private void log(Exception e) { plugin.getLogger().log(Level.SEVERE, "Operasi NPC gagal; progres/hadiah belum dihapus.", e); }
    @Override public void close() {
        if (closed) return; closed = true; if (task != null) task.cancel();
        for (Player p : Bukkit.getOnlinePlayers()) if (p.getOpenInventory().getTopInventory().getHolder() instanceof RewardMenu) p.closeInventory();
        for (Session s : List.copyOf(sessions.values())) end(s, false);
        for (BukkitTask push : pushes.values()) push.cancel(); pushes.clear();
        quests.close(); renderer.close(); HandlerList.unregisterAll(this);
    }
}
'@
    },
    [PSCustomObject]@{
        Name = 'npc/NpcCommand.java'
        Hash = '809851a6db2321e73531e27c347cdceb656dafd0ec714dbe1bfb4a13ad0ff3d6'
        Source = @'
package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.economy.VitiAmount;
import com.bangzachery.vitae.vitaemanager.whisper.WhisperArea;
import com.google.gson.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public final class NpcCommand implements CommandExecutor, TabCompleter {
    private record Corners(Location a, Location b) {}
    private final NpcService service;
    private final Map<UUID, Corners> selections = new HashMap<>();
    private static final List<String> TOPICS = List.of("dialog", "hadiah", "ladang", "mob", "pemicu", "tampilan", "kelola");
    private static final List<String> ACTIONS = List.of("help", "move", "stand", "look", "offset", "height", "skin", "model", "page", "option",
            "clearoptions", "root", "repeat", "speed", "pos1", "pos2", "trigger", "quest", "queststart", "reward", "on", "off", "preview", "reset", "delete");
    public NpcCommand(NpcService service) { this.service = service; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("quest")) {
            if (!(sender instanceof Player p)) { sender.sendMessage(Component.text("Perintah ini khusus pemain.")); return true; }
            if (!p.hasPermission("vitae.quest")) { NpcService.message(p, "Kamu tidak memiliki izin quest."); return true; }
            if (args.length == 1 && args[0].equalsIgnoreCase("quit")) service.quitQuest(p);
            else if (args.length == 1 && args[0].equalsIgnoreCase("status")) service.status(p);
            else sender.sendMessage(Component.text("/quest status • /quest quit"));
            return true;
        }
        if (!sender.hasPermission("vitae.admin.npc")) { sender.sendMessage(Component.text("Perlu izin vitae.admin.npc.", NamedTextColor.RED)); return true; }
        try {
            if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
                sender.sendMessage(Component.text("[Vitae NPC] Modul aktif. NPC tersimpan: " + service.current().npcs().size(), NamedTextColor.GREEN)); return true;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                service.reloadModule(); sender.sendMessage(Component.text("[Vitae NPC] Reload diproses. /vnpc status untuk hasilnya.", NamedTextColor.AQUA)); return true;
            }
            if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                require(args.length <= 2, "/vnpc help [topik]");
                help(sender, "<id>", args.length == 2 ? args[1] : ""); return true;
            }
            if (args[0].equalsIgnoreCase("list") && args.length == 1) {
                sender.sendMessage(Component.text("[Vitae NPC] ", NamedTextColor.GOLD)
                        .append(Component.text("NPC: " + (service.current().npcs().isEmpty() ? "belum ada" : String.join(", ", new TreeSet<>(service.current().npcs().keySet()))), NamedTextColor.AQUA))); return true;
            }
            if (!(sender instanceof Player p)) throw new IllegalArgumentException("Setup lokasi dilakukan sebagai pemain di dalam server.");
            if (args[0].equalsIgnoreCase("create")) {
                require(args.length >= 3, "/vnpc create <id> <nama>"); service.create(p, args[1], join(args, 2));
                created(p, args[1], join(args, 2)); return true;
            }
            require(args.length >= 2, "/vnpc <id> <pengaturan> atau /vnpc help");
            String id = args[0], action = args[1].toLowerCase(Locale.ROOT); var d = service.get(id);
            switch (action) {
                case "help" -> {
                    require(args.length <= 3, "/vnpc <id> help [topik]");
                    help(sender, id, args.length == 3 ? args[2] : ""); return true;
                }
                case "delete" -> { require(args.length == 2, "/vnpc <id> delete"); service.delete(id); }
                case "look" -> {
                    require(args.length == 2, "/vnpc <id> look"); Location look = p.getLocation(); var old = d.location();
                    var location = new NpcData.Point(old.world(), old.x(), old.y(), old.z(), look.getYaw(), look.getPitch());
                    service.edit(id, j -> { j.add("location", NpcStore.JSON.toJsonTree(location)); j.add("facing", NpcStore.JSON.toJsonTree(NpcData.Facing.zero())); }, false);
                }
                case "offset" -> {
                    require(args.length == 3 || args.length == 4, "/vnpc <id> offset <yaw> [pitch] — contoh offset 90 0");
                    var facing = new NpcData.Facing(Float.parseFloat(args[2]), args.length == 4 ? Float.parseFloat(args[3]) : 0);
                    service.edit(id, j -> j.add("facing", NpcStore.JSON.toJsonTree(facing)), false);
                }
                case "move", "stand", "queststart" -> {
                    require(args.length == 2, "/vnpc <id> " + action);
                    if (action.equals("queststart")) {
                        require(d.quest().kind() != NpcData.QuestKind.NONE, "Konfigurasi quest dulu.");
                        service.edit(id, j -> j.getAsJsonObject("quest").add("start", NpcStore.JSON.toJsonTree(NpcRenderer.point(p.getLocation()))), false);
                    } else service.edit(id, j -> j.add(action.equals("move") ? "location" : "stand", NpcStore.JSON.toJsonTree(NpcRenderer.point(p.getLocation()))), false);
                }
                case "height", "speed" -> {
                    require(args.length == 3, "/vnpc <id> " + action + " <angka>");
                    double number = Double.parseDouble(args[2]);
                    if (action.equals("speed")) require(number == Math.rint(number), "Speed harus angka bulat 1–8.");
                    service.edit(id, j -> j.addProperty(action.equals("height") ? "eyeHeight" : "speed", number), false);
                }
                case "skin" -> { require(args.length == 3, "/vnpc <id> skin <nama-pemain>"); service.skin(p, id, args[2]); return true; }
                case "model" -> {
                    require(args.length == 3, "/vnpc <id> model <id-modelengine>");
                    service.edit(id, j -> { j.addProperty("kind", "MODEL"); j.addProperty("model", args[2]); }, false);
                }
                case "page" -> {
                    require(args.length >= 4, "/vnpc <id> page <halaman> <teks>"); NpcData.id(args[2]);
                    List<NpcData.Choice> old = d.nodes().containsKey(args[2]) ? d.nodes().get(args[2]).choices() : List.of();
                    var node = new NpcData.Node(join(args, 3), old);
                    service.edit(id, j -> j.getAsJsonObject("nodes").add(args[2], NpcStore.JSON.toJsonTree(node)), false);
                }
                case "option" -> {
                    require(args.length >= 5, "/vnpc <id> option <halaman> <tujuan|END|QUEST> <label>");
                    NpcData.Node old = d.nodes().get(args[2]); require(old != null, "Halaman belum ada; gunakan page dahulu.");
                    String target = Set.of("END", "QUEST").contains(args[3].toUpperCase(Locale.ROOT)) ? args[3].toUpperCase(Locale.ROOT) : args[3];
                    var choices = new ArrayList<>(old.choices()); choices.add(new NpcData.Choice(join(args, 4), target));
                    var node = new NpcData.Node(old.text(), choices);
                    service.edit(id, j -> j.getAsJsonObject("nodes").add(args[2], NpcStore.JSON.toJsonTree(node)), false);
                }
                case "clearoptions" -> {
                    require(args.length == 3, "/vnpc <id> clearoptions <halaman>"); var old = d.nodes().get(args[2]);
                    require(old != null, "Halaman tidak ditemukan.");
                    service.edit(id, j -> j.getAsJsonObject("nodes").add(args[2], NpcStore.JSON.toJsonTree(new NpcData.Node(old.text(), List.of()))), false);
                }
                case "root", "repeat" -> {
                    require(args.length >= 3, "/vnpc <id> " + action + " <teks/id>");
                    service.edit(id, j -> j.addProperty(action, join(args, 2)), false);
                }
                case "pos1", "pos2" -> {
                    require(args.length == 2, "/vnpc <id> " + action); var block = p.getTargetBlockExact(6);
                    Location point = block == null ? p.getLocation().getBlock().getLocation() : block.getLocation();
                    Corners old = selections.getOrDefault(p.getUniqueId(), new Corners(null, null));
                    selections.put(p.getUniqueId(), action.equals("pos1") ? new Corners(point, old.b) : new Corners(old.a, point));
                    NpcService.message(p, "Titik " + (action.equals("pos1") ? "A" : "B") + " tersimpan: " + point.getBlockX() + ", " + point.getBlockY() + ", " + point.getBlockZ()); return true;
                }
                case "trigger" -> {
                    require(args.length == 3 && Set.of("set", "off").contains(args[2]), "/vnpc <id> trigger set|off");
                    var trigger = args[2].equals("set") ? area(p) : null;
                    service.edit(id, j -> j.add("trigger", trigger == null ? JsonNull.INSTANCE : NpcStore.JSON.toJsonTree(trigger)), false);
                }
                case "quest" -> {
                    require(args.length >= 3, "/vnpc <id> quest farm <gelombang> | mob <jumlah> <COW> | off");
                    NpcData.Quest q;
                    if (args[2].equalsIgnoreCase("off") && args.length == 3) q = NpcData.Quest.none();
                    else {
                        require(args.length >= 4, "Masukkan jumlah target quest."); int goal = Integer.parseInt(args[3]); WhisperArea selected = area(p);
                        if (args[2].equalsIgnoreCase("farm") && args.length == 4) q = new NpcData.Quest(NpcData.QuestKind.FARM, selected,
                                NpcRenderer.point(p.getLocation()), goal, "COW", NpcQuest.scan(selected), NpcData.Reward.none());
                        else {
                            require(args[2].equalsIgnoreCase("mob") && args.length == 5, "/vnpc <id> quest mob <jumlah> <jenis-mob>");
                            var mob = NpcQuest.mobType(args[4]); q = new NpcData.Quest(NpcData.QuestKind.MOB, selected,
                                    NpcRenderer.point(p.getLocation()), goal, mob.name(), List.of(), NpcData.Reward.none());
                        }
                    }
                    service.edit(id, j -> j.add("quest", NpcStore.JSON.toJsonTree(q)), false);
                }
                case "reward" -> {
                    require(args.length >= 4 && Set.of("dialog", "quest").contains(args[2]), "/vnpc <id> reward dialog|quest viti <nominal> | items | none");
                    boolean quest = args[2].equals("quest");
                    if (args[3].equalsIgnoreCase("items") && args.length == 4) { service.items(p, id, quest); return true; }
                    NpcData.Reward reward;
                    if (args[3].equalsIgnoreCase("none") && args.length == 4) reward = NpcData.Reward.none();
                    else { require(args[3].equalsIgnoreCase("viti") && args.length == 5, "/vnpc <id> reward dialog|quest viti <nominal>"); reward = new NpcData.Reward(VitiAmount.positive(VitiAmount.parse(args[4])), List.of()); }
                    require(!quest || d.quest().kind() != NpcData.QuestKind.NONE, "Konfigurasi quest dahulu.");
                    service.edit(id, j -> { if (quest) j.getAsJsonObject("quest").add("reward", NpcStore.JSON.toJsonTree(reward)); else j.add("gift", NpcStore.JSON.toJsonTree(reward)); }, false);
                }
                case "on", "off" -> { require(args.length == 2, "/vnpc <id> " + action); service.edit(id, j -> {}, action.equals("on")); }
                case "preview" -> { require(args.length == 2, "/vnpc <id> preview"); service.preview(p, id); return true; }
                case "reset" -> {
                    require(args.length == 3, "/vnpc <id> reset <nama-tersimpan|UUID>"); UUID player;
                    try { player = UUID.fromString(args[2]); }
                    catch (IllegalArgumentException e) { OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(args[2]); require(cached != null, "Pemain belum tersimpan. Gunakan UUID."); player = cached.getUniqueId(); }
                    service.reset(player, id);
                }
                default -> throw new IllegalArgumentException("Pengaturan tidak dikenal: " + action + ". /vnpc help");
            }
            p.sendMessage(Component.text("[Vitae NPC] ", NamedTextColor.GOLD)
                    .append(Component.text("Tersimpan.", NamedTextColor.GREEN)));
            if (!Set.of("on", "off", "reset", "delete").contains(action)) {
                p.sendMessage(Component.text("NPC menjadi draft OFF selama setup. Aktifkan setelah semua siap.", NamedTextColor.GRAY));
                String nextTopic = (action.equals("quest") && args[2].equalsIgnoreCase("mob"))
                        || (action.equals("queststart") && d.quest().kind() == NpcData.QuestKind.MOB) ? "mob" : topic(action);
                tip(p, "/vnpc " + id + " help " + nextTopic, "Petunjuk tahap berikutnya");
                tip(p, "/vnpc " + id + " on", "Aktifkan NPC setelah setup selesai");
            }
        } catch (Exception e) {
            Throwable cause = e; while (cause.getCause() != null) cause = cause.getCause();
            sender.sendMessage(Component.empty());
            sender.sendMessage(Component.text("[Vitae NPC] ", NamedTextColor.GOLD)
                    .append(Component.text(Objects.requireNonNullElse(cause.getMessage(), "Setup tidak valid."), NamedTextColor.RED)));
            String usage = args.length >= 2 && service.current().npcs().containsKey(args[0])
                    ? "/vnpc " + args[0] + " help " + topic(args[1].toLowerCase(Locale.ROOT)) : "/vnpc help";
            tip(sender, usage, "Klik untuk melihat contoh command");
            sender.sendMessage(Component.empty());
        }
        return true;
    }
    private WhisperArea area(Player p) {
        Corners c = selections.get(p.getUniqueId());
        require(c != null && c.a != null && c.b != null, "Tandai pos1 dan pos2 terlebih dahulu.");
        require(c.a.getWorld().equals(c.b.getWorld()), "Kedua titik harus dalam dunia yang sama.");
        return WhisperArea.between(c.a.getWorld().getUID(), c.a.getBlockX(), c.a.getBlockY(), c.a.getBlockZ(), c.b.getBlockX(), c.b.getBlockY(), c.b.getBlockZ());
    }
    private static void require(boolean valid, String help) { if (!valid) throw new IllegalArgumentException(help); }
    private static String join(String[] a, int start) { return String.join(" ", Arrays.copyOfRange(a, start, a.length)); }
    private static void created(CommandSender sender, String id, String name) {
        header(sender, "NPC " + name + " dibuat • " + id);
        note(sender, "Status draft OFF. Lokasi NPC dua blok di depanmu; posisi bicara memakai tempatmu berdiri.");
        note(sender, "Pilih tujuan NPC. Klik command biru untuk menaruhnya di kotak chat, lalu Enter.");
        purposes(sender, id);
        tip(sender, "/vnpc " + id + " help tampilan", "Atur skin/model dan lokasi");
        note(sender, "Mulai dari dialog, tambahkan hadiah/quest bila perlu, lalu preview dan on.");
        footer(sender);
    }
    private static void header(CommandSender sender, String title) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("──── Vitae NPC • " + title + " ────", NamedTextColor.GOLD, TextDecoration.BOLD));
    }
    private static void footer(CommandSender sender) {
        sender.sendMessage(Component.text("────────────────────────────", NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.empty());
    }
    private static void note(CommandSender sender, String text) { sender.sendMessage(Component.text(text, NamedTextColor.GRAY)); }
    private static void tip(CommandSender sender, String command, String explanation) {
        sender.sendMessage(Component.text(command, NamedTextColor.AQUA)
                .clickEvent(ClickEvent.suggestCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text("Klik untuk menaruh command di kotak chat.", NamedTextColor.YELLOW)))
                .append(Component.text(" — " + explanation, NamedTextColor.GRAY).clickEvent(null).hoverEvent(null)));
    }
    private static void purposes(CommandSender sender, String id) {
        String prefix = id.equals("<id>") ? "/vnpc help " : "/vnpc " + id + " help ";
        tip(sender, prefix + "dialog", "Dialog cerita dan pilihan bercabang");
        tip(sender, prefix + "hadiah", "Hadiah gratis sekali per pemain");
        tip(sender, prefix + "ladang", "Quest memanen ladang");
        tip(sender, prefix + "mob", "Quest membunuh mob");
    }
    private static String topic(String action) {
        return switch (action) {
            case "page", "option", "clearoptions", "root", "speed" -> "dialog";
            case "reward", "repeat" -> "hadiah";
            case "pos1", "pos2", "trigger" -> "pemicu";
            case "quest", "queststart" -> "ladang";
            case "skin", "model", "height", "move", "stand", "look", "offset" -> "tampilan";
            default -> "kelola";
        };
    }
    private static void help(CommandSender sender, String id, String requested) {
        String topic = requested.toLowerCase(Locale.ROOT), cmd = "/vnpc " + id + " ";
        require(topic.isEmpty() || TOPICS.contains(topic), "Topik bantuan: " + String.join(", ", TOPICS) + ".");
        header(sender, topic.isEmpty() ? "Bantuan setup" : "Bantuan " + topic + " • " + id);
        switch (topic) {
            case "dialog" -> {
                tip(sender, cmd + "page start Halo, ada yang bisa kubantu?", "Tulis dialog awal");
                tip(sender, cmd + "page cerita Aku menyimpan sebuah rahasia.", "Buat halaman lanjutan");
                tip(sender, cmd + "option start cerita Aku ingin mendengarkan.", "Pilihan menuju halaman cerita");
                tip(sender, cmd + "option start END Keluar...", "Pilihan mengakhiri percakapan");
                tip(sender, cmd + "option cerita END Terima kasih.", "Akhiri halaman lanjutan");
                tip(sender, cmd + "root start", "Pilih halaman pertama");
                tip(sender, cmd + "speed 1", "Kecepatan mengetik 1–8 karakter per pembaruan");
                tip(sender, cmd + "clearoptions start", "Hapus pilihan halaman start sebelum menyusun ulang");
                note(sender, "page memperbarui teks, option menambah pilihan. Halaman tanpa pilihan otomatis punya Keluar...");
            }
            case "hadiah" -> {
                tip(sender, cmd + "page start Terima kasih sudah mendengarkan ceritaku.", "Tulis dialog sebelum hadiah");
                tip(sender, cmd + "reward dialog viti 1000", "Hadiah Viti sekali per pemain");
                tip(sender, cmd + "reward dialog items", "Buka chest; letakkan item contoh lalu tutup");
                tip(sender, cmd + "reward dialog none", "Matikan hadiah dialog");
                tip(sender, cmd + "repeat Hadiahku sudah kuberikan. Hus hus!", "Dialog setelah hadiah sudah diterima");
                note(sender, "Pilih Viti atau item. Item contoh admin dikembalikan setelah GUI ditutup.");
                note(sender, "Hadiah dikirim saat pemain menyelesaikan dialog melalui END, bukan saat membatalkan.");
            }
            case "ladang" -> {
                note(sender, "Siapkan tanaman di atas farmland. Tandai area, lalu berdiri di titik awal player dalam area.");
                tip(sender, cmd + "pos1", "Lihat blok sudut A, lalu jalankan command");
                tip(sender, cmd + "pos2", "Lihat blok sudut B yang mencakup tanaman");
                tip(sender, cmd + "quest farm 5", "Baca tanaman; lima putaran panen");
                tip(sender, cmd + "queststart", "Simpan tempat awal player dalam area");
                tip(sender, cmd + "reward quest viti 1000", "Hadiah ladang wajib Viti");
                tip(sender, cmd + "page start Bantu aku memanen ladang ini.", "Tulis tawaran quest");
                tip(sender, cmd + "option start QUEST Terima quest.", "Pilihan menerima pekerjaan");
                tip(sender, cmd + "option start END Keluar...", "Pilihan menolak");
                note(sender, "Pemain wajib punya iron hoe. Pos1/pos2 adalah blok yang dilihat, maksimal enam blok.");
            }
            case "mob" -> {
                note(sender, "Tandai area A–B yang aman, lalu berdiri di titik awal player dalam area.");
                tip(sender, cmd + "pos1", "Tandai sudut A");
                tip(sender, cmd + "pos2", "Tandai sudut B");
                tip(sender, cmd + "quest mob 5 COW", "Lima mob; ganti COW dengan jenis mob lain");
                tip(sender, cmd + "queststart", "Simpan tempat awal player dalam area");
                tip(sender, cmd + "reward quest viti 1000", "Atur hadiah quest; items juga tersedia");
                tip(sender, cmd + "page start Bantu aku mengalahkan lima mob.", "Tulis tawaran quest");
                tip(sender, cmd + "option start QUEST Terima quest.", "Terima pekerjaan");
                tip(sender, cmd + "option start END Keluar...", "Tolak pekerjaan");
                note(sender, "Pemain wajib punya iron sword dan memakainya di tangan utama saat membunuh mob quest.");
            }
            case "pemicu" -> {
                note(sender, "Pilih dua sudut lorong yang mencakup ketinggian kaki player, dalam dunia NPC.");
                tip(sender, cmd + "pos1", "Tandai sudut A; mengambil blok yang dilihat");
                tip(sender, cmd + "pos2", "Tandai sudut B");
                tip(sender, cmd + "trigger set", "Jadikan seleksi sebagai pemicu dialog wajib");
                tip(sender, cmd + "trigger off", "Hapus pemicu area");
                tip(sender, cmd + "stand", "Berdiri di depan NPC untuk menyimpan posisi bicara");
                note(sender, "Letakkan stand di luar pemicu. Shift membatalkan; percakapan dipicu lagi sampai selesai.");
            }
            case "tampilan" -> {
                tip(sender, cmd + "skin NamaPemain", "Pakai skin akun premium atau pemain online");
                tip(sender, cmd + "model id_model", "Pakai model custom; perlu ModelEngine 4");
                tip(sender, cmd + "height 1.62", "Tinggi kepala untuk arah kamera; rentang 0.2–8");
                tip(sender, cmd + "move", "Simpan lokasi NPC di tempat kamu berdiri");
                tip(sender, cmd + "stand", "Simpan posisi bicara player di tempat kamu berdiri");
                tip(sender, cmd + "look", "Salin arah pandangmu tanpa memindahkan NPC; offset direset");
                tip(sender, cmd + "offset 90 0", "Geser arah 90 derajat dari arah dasar; berlaku untuk skin/model");
                tip(sender, cmd + "offset 0 0", "Reset offset arah");
                note(sender, "Yaw memutar kiri/kanan (-360–360). Pitch mengatur kepala atas/bawah (-90–90); positif melihat ke bawah.");
                note(sender, "Pilih skin manusia atau model. Hindari posisi berdiri di dalam blok.");
            }
            case "kelola" -> {
                tip(sender, "/vnpc list", "Lihat seluruh ID NPC");
                tip(sender, "/vnpc status", "Lihat apakah modul NPC aktif atau penyebab startup gagal");
                tip(sender, "/vnpc reload", "Muat ulang NPC setelah masalah setup diperbaiki");
                tip(sender, cmd + "on", "Aktifkan setelah semua pengaturan valid");
                tip(sender, cmd + "off", "Nonaktifkan sementara");
                tip(sender, cmd + "quest off", "Hapus quest NPC");
                tip(sender, cmd + "reset NamaPemain", "Reset progres NPC dan kuota quest global pemain");
                tip(sender, cmd + "delete", "Hapus NPC ini");
                note(sender, "Player: /quest status untuk progres, /quest quit untuk membatalkan pekerjaan.");
            }
            default -> {
                tip(sender, "/vnpc create <id> <nama>", "Buat NPC baru; contoh /vnpc create mira Mira");
                purposes(sender, id);
                tip(sender, id.equals("<id>") ? "/vnpc help tampilan" : cmd + "help tampilan", "Skin/model dan posisi NPC");
                tip(sender, id.equals("<id>") ? "/vnpc help pemicu" : cmd + "help pemicu", "Area pemicu dialog wajib");
                tip(sender, id.equals("<id>") ? "/vnpc help kelola" : cmd + "help kelola", "Aktif/nonaktif, reset, hapus, dan daftar NPC");
                note(sender, "Bantuan untuk NPC tertentu: /vnpc <id> help <topik>. Ganti <id> dengan ID NPC milikmu.");
            }
        }
        if (!topic.isEmpty() && !topic.equals("kelola")) {
            sender.sendMessage(Component.empty());
            tip(sender, cmd + "preview", "Coba dialog sendiri; hadiah/quest tidak dijalankan");
            tip(sender, cmd + "on", "Aktifkan setelah seluruh setup selesai");
            note(sender, "Perubahan setup membuat NPC menjadi draft OFF. Klik kiri/kanan membuka dialog; scroll memilih; Shift batal.");
        }
        footer(sender);
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command c, String alias, String[] a) {
        List<String> values = new ArrayList<>();
        if (c.getName().equalsIgnoreCase("quest")) values.addAll(List.of("quit", "status"));
        else if (!sender.hasPermission("vitae.admin.npc")) return List.of();
        else if (a.length > 1 && !Set.of("help", "create", "list").contains(a[0].toLowerCase(Locale.ROOT))
                && !service.current().npcs().containsKey(a[0])) return List.of();
        else if (a.length == 1) { values.addAll(List.of("help", "create", "list", "status", "reload")); values.addAll(service.current().npcs().keySet()); }
        else if (a.length == 2 && service.current().npcs().containsKey(a[0])) values.addAll(ACTIONS);
        else if (a.length == 2 && a[0].equalsIgnoreCase("help")) values.addAll(TOPICS);
        else if (a.length == 3) switch (a[1].toLowerCase(Locale.ROOT)) {
            case "help" -> values.addAll(TOPICS);
            case "trigger" -> values.addAll(List.of("set", "off"));
            case "quest" -> values.addAll(List.of("farm", "mob", "off"));
            case "reward" -> values.addAll(List.of("dialog", "quest"));
            case "page", "option", "root", "clearoptions" -> values.addAll(service.get(a[0]).nodes().keySet());
            default -> { }
        }
        else if (a.length == 4 && a[1].equalsIgnoreCase("reward")) values.addAll(List.of("viti", "items", "none"));
        else if (a.length == 4 && a[1].equalsIgnoreCase("option")) { values.addAll(List.of("END", "QUEST")); values.addAll(service.get(a[0]).nodes().keySet()); }
        else if (a.length == 5 && a[1].equalsIgnoreCase("quest") && a[2].equalsIgnoreCase("mob")) values.addAll(List.of("COW", "PIG", "SHEEP", "CHICKEN", "ZOMBIE", "SKELETON"));
        String last = a.length == 0 ? "" : a[a.length - 1].toLowerCase(Locale.ROOT);
        return values.stream().filter(v -> v.toLowerCase(Locale.ROOT).startsWith(last)).sorted().toList();
    }
}
'@
    },
    [PSCustomObject]@{
        Name = 'npc/NpcModule.java'
        Hash = '73a22e6a89092c2ba55ded73179e1e5d920f67689b36d7ba283bb43118f19028'
        Source = @'
package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.economy.VitiService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/** Lifecycle facade: existing modules in Vitaemanager.java need no replacement. */
public final class NpcModule {
    private static final Map<JavaPlugin, NpcService> ACTIVE = new IdentityHashMap<>();
    private static final Map<JavaPlugin, Throwable> FAILED = new IdentityHashMap<>();
    @FunctionalInterface interface Factory { NpcService create() throws IOException; }
    private NpcModule() {}
    public static void start(JavaPlugin plugin, VitiService viti) throws IOException {
        start(plugin, viti, () -> new NpcService(plugin, viti));
    }
    static void start(JavaPlugin plugin, VitiService viti, Factory factory) {
        if (ACTIVE.containsKey(plugin)) return;
        NpcService service = null;
        try {
            service = factory.create();
            NpcCommand executor = new NpcCommand(service);
            for (String name : List.of("vnpc", "quest")) {
                PluginCommand command = plugin.getCommand(name);
                if (command == null) throw new IllegalStateException("Command " + name + " belum terdaftar di plugin.yml.");
                command.setExecutor(executor); command.setTabCompleter(executor);
            }
            plugin.getServer().getPluginManager().registerEvents(service, plugin);
            service.start(); ACTIVE.put(plugin, service); FAILED.remove(plugin);
        } catch (IOException | RuntimeException | LinkageError error) {
            if (service != null) try { service.close(); }
            catch (RuntimeException | LinkageError cleanup) { error.addSuppressed(cleanup); }
            FAILED.put(plugin, error);
            plugin.getLogger().log(Level.SEVERE, "Modul NPC gagal diaktifkan. Modul Vitae lain tetap berjalan; data npc.json dipertahankan. Gunakan /vnpc status.", error);
            recovery(plugin, viti);
        }
    }
    public static void stop(JavaPlugin plugin) {
        FAILED.remove(plugin);
        NpcService service = ACTIVE.remove(plugin);
        if (service != null) try { service.close(); }
        catch (RuntimeException | LinkageError error) { plugin.getLogger().log(Level.SEVERE, "Pembersihan modul NPC gagal.", error); }
    }
    private static void recovery(JavaPlugin plugin, VitiService viti) {
        CommandExecutor executor = (sender, command, label, args) -> {
            if (command.getName().equalsIgnoreCase("quest")) {
                tell(sender, "Modul quest belum aktif. Minta admin memeriksa /vnpc status.", NamedTextColor.RED); return true;
            }
            if (!sender.hasPermission("vitae.admin.npc")) {
                tell(sender, "Perlu izin vitae.admin.npc.", NamedTextColor.RED); return true;
            }
            try {
                if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                    start(plugin, viti); report(plugin, sender); return true;
                }
                if (args.length == 2 && args[1].equalsIgnoreCase("off")) {
                    NpcStore store = new NpcStore(plugin.getDataFolder().toPath().resolve("npc.json"));
                    NpcData.State state = store.initialize(); var definition = state.npcs().get(args[0]);
                    if (definition == null) throw new IllegalArgumentException("NPC tidak ditemukan: " + args[0]);
                    var json = NpcStore.JSON.toJsonTree(definition).getAsJsonObject(); json.addProperty("enabled", false);
                    store.save(state.put(NpcStore.JSON.fromJson(json, NpcData.Definition.class)));
                    tell(sender, "NPC " + args[0] + " dinonaktifkan. Definisi dan progres tetap tersimpan.", NamedTextColor.YELLOW);
                    start(plugin, viti); report(plugin, sender); return true;
                }
                report(plugin, sender);
            } catch (IOException | RuntimeException error) {
                tell(sender, reason(error), NamedTextColor.RED);
                plugin.getLogger().log(Level.SEVERE, "Pemulihan NPC belum berhasil; data lama dipertahankan.", error);
            }
            return true;
        };
        for (String name : List.of("vnpc", "quest")) {
            PluginCommand command = plugin.getCommand(name);
            if (command == null) continue;
            command.setExecutor(executor);
            command.setTabCompleter((sender, cmd, alias, args) -> {
                if (!sender.hasPermission("vitae.admin.npc")) return List.of();
                if (args.length == 1) return List.of("status", "reload", "help").stream()
                        .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
                return args.length == 2 && "off".startsWith(args[1].toLowerCase(Locale.ROOT)) ? List.of("off") : List.of();
            });
        }
    }
    private static void report(JavaPlugin plugin, CommandSender sender) {
        sender.sendMessage(Component.empty());
        if (ACTIVE.containsKey(plugin)) tell(sender, "Modul NPC aktif kembali.", NamedTextColor.GREEN);
        else {
            tell(sender, "Modul NPC belum aktif: " + reason(FAILED.get(plugin)), NamedTextColor.RED);
            tell(sender, "Perbaiki penyebabnya, lalu /vnpc reload. Jika satu NPC/model bermasalah: /vnpc <id> off.", NamedTextColor.AQUA);
            tell(sender, "Error lengkap ada pada log startup. File npc.json tidak dikosongkan.", NamedTextColor.GRAY);
        }
        sender.sendMessage(Component.empty());
    }
    private static String reason(Throwable error) {
        if (error == null) return "Periksa log startup server.";
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while (error.getCause() != null && seen.add(error)) error = error.getCause();
        return error.getClass().getSimpleName() + ": " + Objects.requireNonNullElse(error.getMessage(), "Periksa log startup server.");
    }
    private static void tell(CommandSender sender, String text, NamedTextColor color) {
        sender.sendMessage(Component.text("[Vitae NPC] ", NamedTextColor.GOLD).append(Component.text(text, color)));
    }
}
'@
    }
)
$plansVitae = @()
$lfVitae = [string][char]10
$crVitae = [string][char]13
foreach ($fileVitae in $filesVitae) {
    $targetVitae = Join-Path $folderVitae $fileVitae.Name
    if (-not (Test-Path -LiteralPath $targetVitae -PathType Leaf)) {
        throw ($fileVitae.Name + ' belum terpasang. Pembaruan membutuhkan modul NPC dan aturan item yang sudah ada. Tidak ada file yang diubah.')
    }
    $oldVitae = [IO.File]::ReadAllText($targetVitae, $utf8Vitae)
    $classVitae = [IO.Path]::GetFileNameWithoutExtension($fileVitae.Name)
    $packageVitae = 'com.bangzachery.vitae.vitaemanager.' + $fileVitae.Name.Split('/')[0]
    if ($oldVitae -notmatch ('package[ \t]+' + [regex]::Escape($packageVitae) + '[ \t]*;') -or
        $oldVitae -notmatch ('public[ \t]+final[ \t]+class[ \t]+' + [regex]::Escape($classVitae) + '(?=[ \t{])')) {
        throw ($fileVitae.Name + ' berbeda dari susunan modul ini. Tidak ada file yang diubah.')
    }
    $newVitae = $fileVitae.Source.Replace($crVitae + $lfVitae, $lfVitae).Replace($crVitae, $lfVitae).TrimEnd([char[]]$lfVitae) + $lfVitae
    $bytesVitae = $utf8Vitae.GetBytes($newVitae)
    if ((Get-VitaeHash $bytesVitae) -ne $fileVitae.Hash) {
        throw ('Isi pemasang ' + $fileVitae.Name + ' tidak utuh. Unduh ulang script. Tidak ada file yang diubah.')
    }
    if ((Get-VitaeHash ([IO.File]::ReadAllBytes($targetVitae))) -ne $fileVitae.Hash) {
        $plansVitae += [PSCustomObject]@{ Name = $relativeRootVitae + '/' + $fileVitae.Name; Path = $targetVitae; Bytes = $bytesVitae }
    }
}
foreach ($requiredVitae in @('npc/NpcQuest.java', 'items/StackLimits.java')) {
    if (-not (Test-Path -LiteralPath (Join-Path $folderVitae $requiredVitae) -PathType Leaf)) {
        throw ($requiredVitae + ' belum ada. Modul belum lengkap; tidak ada file yang diubah.')
    }
}

# Preserve the whole main. Migrate only a recognized old direct NPC lifecycle.
$mainPathVitae = Join-Path $folderVitae 'Vitaemanager.java'
if (-not (Test-Path -LiteralPath $mainPathVitae -PathType Leaf)) { throw 'Vitaemanager.java tidak ditemukan. Tidak ada file yang diubah.' }
$oldMainVitae = [IO.File]::ReadAllText($mainPathVitae, $utf8Vitae)
$newMainVitae = $oldMainVitae
$prefixVitae = '(?:com\.bangzachery\.vitae\.vitaemanager\.npc\.)?'
$startPatternVitae = '(?m)^[ \t]*' + $prefixVitae + 'NpcModule\.start\s*\(\s*this\s*,\s*viti\s*\)\s*;[ \t]*\r?$'
$stopPatternVitae = '(?m)^[ \t]*' + $prefixVitae + 'NpcModule\.stop\s*\(\s*this\s*\)\s*;[ \t]*\r?$'
$legacyPatternVitae = '(?m)^[ \t]*(?<field>[A-Za-z_][A-Za-z_0-9]*)[ \t]*=[ \t]*new[ \t]+' + $prefixVitae + 'NpcService\s*\(\s*this\s*,\s*viti\s*\)\s*;[ \t]*\r?$'
$legacyVitae = [regex]::Matches($oldMainVitae, $legacyPatternVitae)
$startsVitae = [regex]::Matches($oldMainVitae, $startPatternVitae)
$stopsVitae = [regex]::Matches($oldMainVitae, $stopPatternVitae)
if ($legacyVitae.Count -gt 0) {
    if ($legacyVitae.Count -ne 1 -or $startsVitae.Count -ne 0 -or $stopsVitae.Count -ne 0) {
        throw 'Startup NPC ganda atau berbeda. Tidak ada file yang diubah. Kirim Vitaemanager.java utuh untuk penyesuaian.'
    }
    $fieldVitae = [regex]::Escape($legacyVitae[0].Groups['field'].Value)
    $blockPatternVitae = '(?m)^(?<indent>[ \t]*)' + $fieldVitae + '[ \t]*=[ \t]*new[ \t]+' + $prefixVitae +
        'NpcService\s*\(\s*this\s*,\s*viti\s*\)\s*;[ \t]*\r?\n[ \t]*' +
        'getServer\s*\(\s*\)\s*\.\s*getPluginManager\s*\(\s*\)\s*\.\s*registerEvents\s*\(\s*' + $fieldVitae +
        '\s*,\s*this\s*\)\s*;[ \t]*\r?\n[ \t]*' + $fieldVitae + '\.start\s*\(\s*\)\s*;[ \t]*(?<tail>\r?)$'
    $closePatternVitae = '(?m)^(?<indent>[ \t]*)if\s*\(\s*' + $fieldVitae + '\s*!=\s*null\s*\)\s*' +
        '(?:\{\s*)?' + $fieldVitae + '\.close\s*\(\s*\)\s*;[ \t]*(?:\})?[ \t]*(?<tail>\r?)$'
    if ([regex]::Matches($oldMainVitae, $blockPatternVitae).Count -ne 1 -or
        [regex]::Matches($oldMainVitae, $closePatternVitae).Count -ne 1) {
        throw 'Integrasi NPC langsung belum dikenali secara aman. Tidak ada file yang diubah. Kirim Vitaemanager.java utuh untuk penyesuaian.'
    }
    $newMainVitae = [regex]::Replace($oldMainVitae, $blockPatternVitae, ('$' + '{indent}com.bangzachery.vitae.vitaemanager.npc.NpcModule.start(this, viti);' + '$' + '{tail}'))
    $newMainVitae = [regex]::Replace($newMainVitae, $closePatternVitae, ('$' + '{indent}com.bangzachery.vitae.vitaemanager.npc.NpcModule.stop(this);' + '$' + '{tail}'))
    $declarationVitae = '(?m)^[ \t]*private[ \t]+' + $prefixVitae + 'NpcService[ \t]+' + $fieldVitae + '[ \t]*;[ \t]*\r?$'
    $remainingMainVitae = [regex]::Replace($newMainVitae, $declarationVitae, '')
    if ([regex]::IsMatch($remainingMainVitae, '\b' + $fieldVitae + '\b')) {
        throw 'Field NPC masih dipakai di bagian lain. Tidak ada file yang diubah. Kirim Vitaemanager.java utuh untuk integrasi.'
    }
} elseif ($startsVitae.Count -ne 1 -or $stopsVitae.Count -ne 1) {
    throw 'Penghubung NpcModule.start/stop belum lengkap atau ganda. Tidak ada file yang diubah. Kirim Vitaemanager.java utuh untuk integrasi.'
}
if ($newMainVitae -ne $oldMainVitae) {
    $mainBytesVitae = $utf8Vitae.GetBytes($newMainVitae)
    $oldMainBytesVitae = [IO.File]::ReadAllBytes($mainPathVitae)
    if ($oldMainBytesVitae.Length -ge 3 -and $oldMainBytesVitae[0] -eq 239 -and $oldMainBytesVitae[1] -eq 187 -and $oldMainBytesVitae[2] -eq 191) {
        $mainBytesVitae = [byte[]](@(239, 187, 191) + $mainBytesVitae)
    }
    $plansVitae += [PSCustomObject]@{ Name = $relativeRootVitae + '/Vitaemanager.java'; Path = $mainPathVitae; Bytes = $mainBytesVitae }
}
if ($plansVitae.Count -eq 0) {
    Write-Host 'Pembaruan Vitae ini sudah terpasang. Tidak ditulis ulang.' -ForegroundColor Green
    exit 0
}
Write-Host ('Pemeriksaan lolos: ' + $plansVitae.Count + ' file siap diperbarui.') -ForegroundColor Green
Write-Host 'Fitur: wajib jongkok; NPC look/offset; status/reload dan isolasi kegagalan modul NPC.' -ForegroundColor Cyan
if ($newMainVitae -ne $oldMainVitae) { Write-Host 'Integrasi NPC lama dipindah ke NpcModule. Bagian modul lain dalam main dipertahankan.' -ForegroundColor Yellow }
if ($CheckOnly) { Write-Host 'CheckOnly selesai. Sumber proyek belum diubah.' -ForegroundColor Yellow; exit 0 }
$backupVitae = Join-Path $projectVitae ('vitae-update-backup-' + [DateTime]::Now.ToString('yyyyMMdd-HHmmss-fff') + '-' + [Guid]::NewGuid().ToString('N').Substring(0, 6))
[IO.Directory]::CreateDirectory($backupVitae) | Out-Null
$temporaryVitae = @()
$attemptedVitae = @()
try {
    foreach ($planVitae in $plansVitae) {
        $savedVitae = Join-Path $backupVitae $planVitae.Name
        [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($savedVitae)) | Out-Null
        Copy-Item -LiteralPath $planVitae.Path -Destination $savedVitae -ErrorAction Stop
    }
    foreach ($planVitae in $plansVitae) {
        $tempVitae = $planVitae.Path + '.vitae-update-' + [Guid]::NewGuid().ToString('N')
        $temporaryVitae += $tempVitae
        [IO.File]::WriteAllBytes($tempVitae, $planVitae.Bytes)
        $planVitae | Add-Member -NotePropertyName Temporary -NotePropertyValue $tempVitae
    }
    foreach ($planVitae in $plansVitae) {
        $attemptedVitae += $planVitae
        Move-Item -LiteralPath $planVitae.Temporary -Destination $planVitae.Path -Force -ErrorAction Stop
    }
} catch {
    $errorVitae = $_.Exception.Message
    foreach ($planVitae in $attemptedVitae) {
        Copy-Item -LiteralPath (Join-Path $backupVitae $planVitae.Name) -Destination $planVitae.Path -Force -ErrorAction Stop
    }
    throw ('Pembaruan dihentikan; file yang ditulis dipulihkan. Penyebab: ' + $errorVitae + '. Backup: ' + $backupVitae)
} finally {
    foreach ($tempVitae in $temporaryVitae) {
        if (Test-Path -LiteralPath $tempVitae -PathType Leaf) { Remove-Item -LiteralPath $tempVitae -Force -ErrorAction SilentlyContinue }
    }
}
Write-Host 'Pembaruan Vitae terpasang. Backup sumber lama:' -ForegroundColor Green
Write-Host $backupVitae
Write-Host 'Lanjutkan: .\gradlew.bat clean build' -ForegroundColor Cyan
Write-Host 'Ganti JAR plugin dengan hasil build baru, lalu restart server.' -ForegroundColor Yellow
Write-Host 'Arah NPC: /vnpc <id> look atau /vnpc <id> offset <yaw> [pitch], lalu /vnpc <id> on' -ForegroundColor Cyan
Write-Host 'Jika NPC belum aktif: /vnpc status dan /vnpc reload. Jika seluruh plugin disabled, ambil error pertama dari log startup.' -ForegroundColor Yellow
