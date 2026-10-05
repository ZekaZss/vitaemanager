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
