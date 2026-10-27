package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.core.gui.VitaeMenu;
import com.bangzachery.vitae.vitaemanager.economy.*;
import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import com.google.gson.JsonObject;
import com.google.gson.JsonNull;
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
import java.util.function.BooleanSupplier;
import java.util.logging.Level;

public final class NpcService implements Listener, AutoCloseable {
    static final class Session {
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
    private final NpcGates gates;
    private final NpcShop shops;
    private java.util.function.Predicate<Player> abilityBlocked = player -> false;
    public void abilityGuard(java.util.function.Predicate<Player> guard) { abilityBlocked = Objects.requireNonNull(guard); }
    public boolean isAbilityBlocked(Player player) { return abilityBlocked.test(player); }
    private record FarmSetup(UUID admin, NpcFarm.Handle work) {}
    private final Map<String, FarmSetup> farmSetups = new HashMap<>();
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, Long> clicks = new HashMap<>(), warned = new HashMap<>();
    private final Map<UUID, BukkitTask> pushes = new HashMap<>();
    private final Set<UUID> internalTeleport = new HashSet<>(), paying = new HashSet<>();
    private final Set<UUID> queuedTriggers = new HashSet<>();
    private final Map<UUID, Long> triggerRetry = new HashMap<>();
    private final NamespacedKey awardKey;
    private BukkitTask task;
    private boolean closed;
    private long ticks;
    public NpcService(JavaPlugin plugin, VitiService viti) throws IOException {
        this.plugin = plugin; this.viti = viti; awardKey = new NamespacedKey(plugin, "npc_last_award");
        store = new NpcStore(plugin.getDataFolder().toPath().resolve("npc/npc.json")); state = store.initialize();
        for (var d : state.npcs().values()) validateRuntime(d, false);
        for (var a : state.pending().values()) for (String bytes : a.reward().items()) decode(bytes);
        renderer = new NpcRenderer(plugin, this::modelClick);
        quests = new NpcQuest(plugin, () -> state.npcs().values(), this::questComplete, this::teleport);
        gates = new NpcGates(plugin, () -> state.npcs(), (id, gate) -> commit(state.put(get(id).withGate(gate))), this::log);
        shops = new NpcShop(plugin, viti, this);
        Bukkit.getPluginManager().registerEvents(quests, plugin);
    }
    public void start() {
        try {
            gates.start();
            shops.start();
            renderer.refresh(state.npcs());
            for (var d : state.npcs().values()) if (d.enabled()) quests.restore(d);
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1, 1);
        } catch (IOException e) { close(); throw new IllegalStateException("Status gerbang gagal dipulihkan; data lama dipertahankan.", e); }
        catch (RuntimeException e) { close(); throw e; }
    }
    public NpcData.State current() { return state; }
    public void cancelStoryDialog(Player player) {
        if (player == null) return;
        Session session = sessions.get(player.getUniqueId());
        if (session != null) end(session, false);
    }
    public boolean storyReward(Player player, String id, String completionPage) {
        NpcData.Definition definition = get(id);
        String text = completionPage.isEmpty() ? "Selamat! Kamu telah menyelesaikan tantanganku. Ambillah hadiahmu."
                : definition.nodes().get(completionPage).text();
        if (!begin(player, definition, false, false, text)) return false;
        Session session = sessions.get(player.getUniqueId());
        session.node = new NpcData.Node(text, List.of(new NpcData.Choice("Ambil hadiah", "DUNGEON_REWARD")));
        return true;
    }
    public NpcData.Definition get(String id) {
        NpcData.Definition d = state.npcs().get(id);
        if (d == null) throw new IllegalArgumentException("NPC tidak ditemukan: " + id + ". /vnpc list"); return d;
    }
    public boolean inUse(String id) {
        var npc = state.npcs().get(id);
        return npc != null && shops.hasPending(npc.token()) || farmSetups.containsKey(id) || gates.busy(id) || quests.uses(id) || sessions.values().stream().anyMatch(s -> s.npc.id().equals(id));
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
        if(closed)throw new IllegalStateException("Modul NPC sudah dimuat ulang. Ambil alat seleksi baru.");
        NpcData.Definition old = get(id);
        if (inUse(id)) throw new IllegalStateException("NPC sedang dialog, quest, pemindaian, atau pemulihan ladang. Tunggu selesai sebelum mengubahnya.");
        JsonObject json = NpcStore.JSON.toJsonTree(old).getAsJsonObject(); change.accept(json);
        json.addProperty("enabled", enable); NpcData.Definition next = NpcStore.JSON.fromJson(json, NpcData.Definition.class);
        if (!next.id().equals(old.id()) || !next.token().equals(old.token())) throw new IllegalArgumentException("Identitas NPC tidak boleh berubah.");
        validateRuntime(next);
        NpcData.State previous = state; commit(state.put(next));
        try { gates.refresh(); renderer.refresh(state.npcs()); if (next.enabled()) quests.restore(next); }
        catch (RuntimeException e) { commit(previous); gates.refresh(); renderer.refresh(previous.npcs()); throw e; }
    }
    private void validateRuntime(NpcData.Definition d) {
        validateRuntime(d, true);
    }
    private void validateRuntime(NpcData.Definition d, boolean requireWorld) {
        for (String item : d.gift().items()) decode(item);
        for (String item : d.quest().reward().items()) decode(item);
        if (d.quest().kind() == NpcData.QuestKind.MOB) NpcQuest.mobType(d.quest().mob());
        Set<String> plants = new HashSet<>(), soils = new HashSet<>();
        for (var crop : d.quest().crops()) {
            if (plants.add(crop.plant())) {
                var plant = Bukkit.createBlockData(crop.plant());
                if (!(plant instanceof org.bukkit.block.data.Ageable)
                        || !Set.of(Material.CARROTS, Material.POTATOES, Material.WHEAT, Material.BEETROOTS).contains(plant.getMaterial()))
                    throw new IllegalArgumentException("Snapshot ladang tidak valid.");
            }
            if (soils.add(crop.soil()) && Bukkit.createBlockData(crop.soil()).getMaterial() != Material.FARMLAND)
                throw new IllegalArgumentException("Snapshot ladang tidak valid.");
        }
        if (d.gate() != null) for (var cell : d.gate().blocks()) {
            Material material = Bukkit.createBlockData(cell.data()).getMaterial();
            if (!material.isSolid() || material.hasGravity() || material.isInteractable()
                    || Set.of(Material.BEDROCK, Material.BARRIER, Material.END_PORTAL_FRAME, Material.REINFORCED_DEEPSLATE).contains(material))
                throw new IllegalArgumentException("Snapshot gerbang harus berupa dinding solid biasa.");
        }
        if (requireWorld && d.enabled() && (Bukkit.getWorld(d.location().world()) == null || Bukkit.getWorld(d.stand().world()) == null))
            throw new IllegalArgumentException("Dunia NPC belum dimuat.");
    }
    public void delete(String id) throws IOException {
        var old = get(id); if (inUse(id)) throw new IllegalStateException("NPC sedang dipakai atau mempunyai transaksi toko yang belum diselesaikan."); commit(state.remove(id)); gates.refresh(); renderer.refresh(state.npcs()); shops.delete(old);
    }
    public void shop(Player admin, String id, String[] args) throws IOException { shops.command(admin, get(id), args); }
    public void copyShop(Player admin, String sourceId, String newId) throws IOException {
        var source = get(sourceId);
        if (!shops.configured(source)) throw new IllegalArgumentException("NPC asal belum memiliki katalog toko.");
        if (state.npcs().containsKey(newId)) throw new IllegalArgumentException("ID NPC tujuan sudah digunakan.");
        var fresh = NpcData.Definition.create(newId, source.name(), NpcRenderer.point(admin.getLocation()), NpcRenderer.point(admin.getLocation()));
        var json = NpcStore.JSON.toJsonTree(fresh).getAsJsonObject();
        for (String property : List.of("kind", "model", "texture", "signature", "eyeHeight", "facing", "appearance")) json.add(property, NpcStore.JSON.toJsonTree(source).getAsJsonObject().get(property));
        var copy = NpcStore.JSON.fromJson(json, NpcData.Definition.class);
        // Persist an isolated token/catalog first. An interrupted creation can only leave an orphan,
        // never share the source catalog or copy quest progress/rewards/gate identity.
        shops.copy(source, copy); commit(state.put(copy)); renderer.refresh(state.npcs());
        message(admin, "Toko disalin sebagai " + newId + " (OFF), tanpa progres/hadiah quest. Aktifkan dengan /vnpc " + newId + " on.");
    }
    public void setupFarm(Player admin, String id, com.bangzachery.vitae.vitaemanager.whisper.WhisperArea area, int goal) {
        NpcData.Definition before = get(id);
        if (inUse(id) || quests.farmBusy(area)) throw new IllegalStateException("NPC/ladang sedang dipakai atau diproses. Tunggu sampai selesai.");
        if (goal < 1 || goal > 50) throw new IllegalArgumentException("Jumlah putaran harus 1–50.");
        NpcData.Point start = NpcRenderer.point(admin.getLocation());
        if (!NpcData.inside(area, start)) throw new IllegalArgumentException("Berdiri di titik awal pemain di dalam area ladang sebelum menyimpan quest.");
        BooleanSupplier active = () -> !closed && admin.isOnline() && admin.hasPermission("vitae.admin.npc") && state.npcs().get(id) == before;
        NpcFarm.Handle work = quests.scan(area, active, (read, total) -> {
            if (admin.isOnline()) admin.sendActionBar(Component.text("Membaca ladang " + id + ": " + read + "/" + total + " blok", NamedTextColor.AQUA));
        }, crops -> {
            farmSetups.remove(id);
            if (!active.getAsBoolean()) return;
            try {
                NpcData.Reward reward = before.quest().kind() == NpcData.QuestKind.FARM ? before.quest().reward() : NpcData.Reward.none();
                NpcData.Quest quest = new NpcData.Quest(NpcData.QuestKind.FARM, area, start, goal, "COW", crops, reward);
                edit(id, json -> json.add("quest", NpcStore.JSON.toJsonTree(quest)), false);
                admin.sendActionBar(Component.empty());
                message(admin, "Ladang tersimpan: " + crops.size() + " tanaman, " + goal + " putaran. NPC sekarang OFF.");
                message(admin, "Contoh hadiah: /vnpc " + id + " reward quest viti 10000. Setelah setup selesai: /vnpc " + id + " on.");
            } catch (IOException | RuntimeException e) { message(admin, e.getMessage()); log(e); }
        }, reason -> {
            farmSetups.remove(id);
            if (admin.isOnline()) { admin.sendActionBar(Component.empty()); message(admin, reason + " Setup lama dipertahankan."); }
        });
        farmSetups.put(id, new FarmSetup(admin.getUniqueId(), work));
        message(admin, "Pemindaian ladang dimulai. Tunggu pesan 'Ladang tersimpan' sebelum menjalankan command hadiah/on. /vnpc " + id + " quest cancel untuk batal.");
    }
    public void cancelFarmSetup(String id) {
        get(id); FarmSetup setup = farmSetups.remove(id);
        if (setup == null) throw new IllegalArgumentException("NPC ini tidak sedang membaca ladang.");
        setup.work().cancel();
        Player admin = Bukkit.getPlayer(setup.admin());
        if (admin != null) { admin.sendActionBar(Component.empty()); message(admin, "Pemindaian dibatalkan. Setup ladang sebelumnya tetap tersimpan."); }
    }
    public void reset(UUID player, String id) throws IOException {
        if (quests.run(player) != null || sessions.containsKey(player)) throw new IllegalStateException("Pemain sedang dialog/quest.");
        commit(state.reset(player, get(id))); warned.remove(player); triggerRetry.remove(player);
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
    public void gateSelect(Player admin, String id, boolean first) { get(id); gates.arm(admin, id, first); }
    public void gateCancel(Player admin) { gates.cancelSelection(admin); }
    public void gateSet(Player admin, String id) throws IOException {setupGate(admin,id,gates.selectedArea(admin,id));}
    public void setupGate(Player admin,String id,com.bangzachery.vitae.vitaemanager.whisper.WhisperArea area){
        NpcData.Definition before=get(id);
        if(before.gate()!=null)throw new IllegalArgumentException("Gerbang sudah ada. Gunakan gate off sebelum mengganti seleksi.");
        if(inUse(id))throw new IllegalStateException("NPC sedang dipakai.");
        gates.scan(admin,id,area,()->state.npcs().get(id)==before,gate->{
            try{edit(id,json->{json.add("gate",NpcStore.JSON.toJsonTree(gate));json.add("trigger",JsonNull.INSTANCE);},false);
                message(admin,"Gerbang tersimpan: "+gate.blocks().size()+" blok. NPC OFF sampai /vnpc "+id+" on.");
            }catch(IOException|RuntimeException e){message(admin,e.getMessage());log(e);}
        });
    }
    public void gateReset(String id) throws IOException {
        if (inUse(id)) throw new IllegalStateException("NPC sedang dialog, quest, atau animasi. Tunggu selesai sebelum reset gerbang.");
        NpcData.Gate gate = get(id).gate();
        if (gate == null) throw new IllegalArgumentException("Gerbang belum diatur.");
        gates.reset(id, () -> edit(id, json -> json.add("gate", NpcStore.JSON.toJsonTree(gate.withStatus(NpcData.GateStatus.CLOSED))), false));
    }
    public void reloadModule() throws IOException {
        if (state.npcs().keySet().stream().anyMatch(this::inUse)) throw new IllegalStateException("Selesaikan dialog/quest aktif sebelum reload NPC.");
        var guard = abilityBlocked;
        NpcModule.stop(plugin); NpcModule.start(plugin, viti);
        var replacement = NpcModule.active(plugin); if (replacement != null) replacement.abilityGuard(guard);
    }
    public void status(Player p) {
        NpcQuest.Run r = quests.run(p.getUniqueId()); var quota = state.quota(p.getUniqueId(), System.currentTimeMillis());
        message(p, r == null ? "Tidak ada quest aktif. Selesai: " + quota.count() + "/3."
                : "Quest " + NpcText.plain(r.npc.name()) + ": " + r.count + "/" + r.npc.quest().goal());
        if (quota.count() == 3) message(p, "Cooldown tersisa " + Math.max(1, (quota.until() - System.currentTimeMillis() + 999) / 1000) + " detik.");
    }
    private void tick() {
        if (closed) return; ticks++;
        if (ticks % 10 == 0) renderer.tick(); quests.tick(); gates.tick();
        for (Session s : List.copyOf(sessions.values())) {
            try {
            if (!s.player.isOnline() || s.player.isDead() || System.currentTimeMillis() >= s.deadline) { end(s, false); continue; }
            if (ticks % 2 == 0 && s.npc.appearance().track()) renderer.lookAt(s.npc.id(), s.player);
            if (s.phase == 0) {
                if (++s.age < 12) continue;
                Location target = NpcRenderer.location(s.npc.stand());
                if (!target.getBlock().isPassable() || !target.clone().add(0, 1, 0).getBlock().isPassable()
                        || !teleport(s.player, target)) {
                    triggerRetry.put(s.player.getUniqueId(), ticks + 40);
                    message(s.player, "Teleport dialog gagal. Admin: cek /vnpc " + s.npc.id() + " stand dan izin teleport plugin proteksi.");
                    end(s, true); continue;
                }
                removeBlindness(s); if (s.blackout != null) { s.blackout.remove(); s.blackout = null; }
                s.anchor = s.player.getLocation().clone(); s.phase = 1; s.age = 0; aim(s);
            } else if (s.phase == 1) {
                float t = Math.min(1, ++s.age / 12f);
                s.anchor.setYaw(s.fromYaw + shortest(s.goalYaw - s.fromYaw) * t);
                s.anchor.setPitch(s.fromPitch + (s.goalPitch - s.fromPitch) * t);
                if (!teleport(s.player, s.anchor)) { end(s, false); continue; }
                if (s.age >= 12) { s.phase = 2; s.age = 0; display(s); }
            } else if (ticks % 2 == 0) {
                int length = s.node.text().codePointCount(0, s.node.text().length());
                if (s.shown < length) { s.shown = Math.min(length, s.shown + s.npc.speed()); sound(s.player, "minecraft:block.note_block.hat", .15f, 1.4f); }
                render(s);
            }
            } catch (RuntimeException e) {
                triggerRetry.put(s.player.getUniqueId(), ticks + 40);
                end(s, false); message(s.player, "Dialog dihentikan karena error. Kamu sudah dilepas; minta admin memeriksa log."); log(e);
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
        Component text = NpcText.dialogName(s.npc.name()).decorate(TextDecoration.BOLD).append(Component.newline())
                .append(Component.text(s.node.text().substring(0, end), NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false));
        if (s.shown >= total) {
            List<NpcData.Choice> choices = s.node.available(); int start = Math.max(0, Math.min(s.selected - 1, choices.size() - 3));
            for (int i = start; i < Math.min(choices.size(), start + 3); i++) text = text.append(Component.text("\n" + (i == s.selected ? "▶ " : "  ") + choices.get(i).label(), i == s.selected ? NamedTextColor.YELLOW : NamedTextColor.GRAY));
            s.player.sendActionBar(Component.text("Scroll: pilih • Klik kiri/kanan: pilih • Shift: batal", NamedTextColor.GRAY));
        } else s.player.sendActionBar(Component.text("Dengarkan dialog... • Shift: batal", NamedTextColor.GRAY));
        s.display.text(text);
    }
    private boolean begin(Player p, NpcData.Definition d, boolean trigger, boolean preview, String specialText) {
        if (isAbilityBlocked(p)) return false;
        if (closed || pushes.containsKey(p.getUniqueId()) || (!preview && !d.enabled()) || (!preview && !p.hasPermission("vitae.npc"))
                || p.isDead() || p.getGameMode() == GameMode.SPECTATOR || p.getVehicle() != null || !p.getPassengers().isEmpty()
                || sessions.containsKey(p.getUniqueId()) || quests.run(p.getUniqueId()) != null
                || p.getOpenInventory().getTopInventory().getHolder() instanceof RewardMenu) return false;
        if (!p.getWorld().getUID().equals(d.location().world())) return false;
        if (!trigger && !preview && p.getLocation().distanceSquared(NpcRenderer.location(d.location())) > 25) return false;
        if (!preview && state.pending().containsKey(p.getUniqueId())) { message(p, "Hadiah sebelumnya sedang dikirim. Tunggu sebentar."); return false; }
        com.bangzachery.vitae.vitaemanager.story.StoryNpcDialogEvent storyDialog = null;
        if (!preview && specialText == null) {
            storyDialog = new com.bangzachery.vitae.vitaemanager.story.StoryNpcDialogEvent(p, d.id());
            Bukkit.getPluginManager().callEvent(storyDialog);
        }
        if (!preview && specialText == null) {
            var quota = state.quota(p.getUniqueId(), System.currentTimeMillis());
            if (d.quest().kind() != NpcData.QuestKind.NONE && quota.count() == 3) {
                if (Objects.equals(warned.get(p.getUniqueId()), quota.until())) { push(p, d); return false; }
                warned.put(p.getUniqueId(), quota.until()); specialText = "Kamu sudah menyelesaikan tiga quest. Istirahat dulu; kembali dalam "
                        + Math.max(1, (quota.until() - System.currentTimeMillis() + 999) / 1000) + " detik.";
            } else if (d.gate() != null && d.gate().status() != NpcData.GateStatus.CLOSED) {
                specialText = switch (d.gate().status()) {
                    case OPEN -> d.repeat();
                    case OPENING -> "Gerbang sedang dibuka. Tunggu sebentar.";
                    case CLOSING -> "Gerbang sedang ditutup. Tunggu sampai selesai.";
                    default -> null;
                };
            } else if (d.gate() == null && d.quest().kind() == NpcData.QuestKind.NONE && state.done(p.getUniqueId(), d)
                    && d.memory().isEmpty() && (storyDialog == null || !storyDialog.managed())) specialText = d.repeat();
        }
        Session s = new Session(p, d, preview, specialText != null || (storyDialog != null && (storyDialog.managed() || storyDialog.node() != null)), trigger);
        s.node = specialText == null ? d.nodes().get(state.recalledRoot(p.getUniqueId(), d)) : new NpcData.Node(specialText, List.of());
        if (storyDialog != null && storyDialog.node() != null) s.node = storyDialog.node();
        sessions.put(p.getUniqueId(), s);
        if (d.appearance().track()) renderer.lookAt(d.id(), p);
        try { if (trigger) {
            blackout(s);
        } else aim(s); }
        catch (RuntimeException e) { end(s, false); throw e; }
        return true;
    }
    void blackout(Session s) {
            Location blackAt = s.player.getEyeLocation().add(s.player.getEyeLocation().getDirection());
            s.blackout = blackAt.getWorld().spawn(blackAt, TextDisplay.class, e -> {
                e.setPersistent(false); e.setVisibleByDefault(false); e.setBillboard(Display.Billboard.CENTER);
                e.setSeeThrough(true); e.setBackgroundColor(Color.fromARGB(255, 0, 0, 0));
                e.text(Component.text("                                        \n                                        \n                                        "));
                var t = e.getTransformation(); t.getScale().set(new Vector3f(100)); e.setTransformation(t);
            });
            s.player.showEntity(plugin, s.blackout);
            if (!s.player.hasPotionEffect(PotionEffectType.BLINDNESS)) { s.player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0, false, false)); s.blind = true; }
    }
    private void modelClick(Player p, String id, boolean attack) {
        if (isAbilityBlocked(p)) { message(p, "Kamu tidak dapat berinteraksi selama pengaruh sihir ini."); return; }
        if (sessions.containsKey(p.getUniqueId())) { confirm(p); return; }
        if (gates.selecting(p.getUniqueId())) return;
        if (ticks - clicks.getOrDefault(p.getUniqueId(), -100L) < 6) return;
        if (shops.interact(p, get(id), attack)) { clicks.put(p.getUniqueId(), ticks); return; }
        click(p, id);
    }
    private void click(Player p, String id) {
        if (isAbilityBlocked(p)) { message(p, "Kamu tidak dapat berinteraksi selama pengaruh sihir ini."); return; }
        if (gates.selecting(p.getUniqueId())) return;
        if (ticks - clicks.getOrDefault(p.getUniqueId(), -100L) < 6) return; clicks.put(p.getUniqueId(), ticks);
        begin(p, get(id), false, false, null);
    }
    private void confirm(Player p) {
        if (isAbilityBlocked(p)) return;
        Session s = sessions.get(p.getUniqueId());
        if (s == null || s.phase != 2 || s.shown < s.node.text().codePointCount(0, s.node.text().length())
                || ticks - clicks.getOrDefault(p.getUniqueId(), -100L) < 4) return;
        clicks.put(p.getUniqueId(), ticks); NpcData.Choice c = s.node.available().get(s.selected);
        sound(p, "minecraft:entity.experience_orb.pickup", .4f, 1.3f);
        if (com.bangzachery.vitae.vitaemanager.story.StoryNpcActionEvent.target(c.target())) {
            if (s.preview) { message(p, "Preview selesai. Aksi story tidak dijalankan saat preview."); end(s, false); return; }
            String npcId = s.npc.id(); end(s, false);
            Bukkit.getPluginManager().callEvent(new com.bangzachery.vitae.vitaemanager.story.StoryNpcActionEvent(p, npcId, c.target()));
        } else if (c.target().equals("END")) {
            try {
                if (!s.preview && !s.special) commit(state.remember(p.getUniqueId(), s.npc, c).finish(p.getUniqueId(), s.npc, false, UUID.randomUUID(), System.currentTimeMillis()));
                end(s, false); pay(p);
            } catch (IOException e) { message(p, "Progres belum tersimpan; coba pilih kembali."); log(e); }
        } else if (c.target().equals("OPEN")) {
            if (s.preview) { message(p, "Preview selesai. Gerbang dan hadiah tidak dijalankan saat preview."); end(s, false); return; }
            try {
                boolean first = gates.open(s.npc.id(), opening -> commit(state.remember(p.getUniqueId(), s.npc, c).finish(p.getUniqueId(), s.npc, false, UUID.randomUUID(), System.currentTimeMillis())
                        .put(get(s.npc.id()).withGate(opening))));
                if (!first) commit(state.remember(p.getUniqueId(), s.npc, c).finish(p.getUniqueId(), s.npc, false, UUID.randomUUID(), System.currentTimeMillis()));
                end(s, false); pay(p);
                message(p, first ? "Gerbang mulai bergetar. Tunggu sampai terbuka!" : "Gerbang sudah dibuka atau sedang dibuka pemain lain.");
            } catch (IOException | RuntimeException e) {
                page(s, new NpcData.Node("Gerbang belum bisa dibuka. Minta admin memeriksa setup; kamu boleh mencoba lagi.", List.of())); log(e);
            }
        } else if (c.target().equals("QUEST")) {
            if (s.preview) { message(p, "Preview selesai. Quest dan hadiah tidak dijalankan saat preview."); end(s, false); return; }
            String refusal = p.hasPermission("vitae.quest") ? quests.refusal(p, s.npc) : "Kamu tidak memiliki izin quest.";
            var quota = state.quota(p.getUniqueId(), System.currentTimeMillis()); if (quota.count() == 3) refusal = "Kamu masih cooldown.";
            if (refusal != null) { page(s, new NpcData.Node(refusal, List.of())); return; }
            end(s, false);
            if (quests.start(p, s.npc)) {
                try { commit(state.remember(p.getUniqueId(), s.npc, c).heard(p.getUniqueId(), s.npc)); }
                catch (IOException e) { quests.cancel(p.getUniqueId(), "Progres gagal disimpan; coba kembali.", true); log(e); }
            }
        } else {
            try {
                if (!s.preview && !s.special && !c.memoryKey().isEmpty()) commit(state.remember(p.getUniqueId(), s.npc, c));
                page(s, s.npc.nodes().get(c.target()));
            } catch (IOException e) { message(p, "Pilihan belum tersimpan; coba kembali."); log(e); }
        }
    }
    private void page(Session s, NpcData.Node node) { s.node = node; s.shown = 0; s.selected = 0; render(s); }
    private void end(Session s, boolean sound) {
        sessions.remove(s.player.getUniqueId()); renderer.endLook(s.npc.id(), s.player); removeBlindness(s); if (s.blackout != null) s.blackout.remove(); if (s.display != null) s.display.remove();
        s.player.sendActionBar(Component.empty()); if (sound) sound(s.player, "minecraft:entity.villager.no", .45f, 1);
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
        try { return p.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN); } finally { internalTeleport.remove(p.getUniqueId()); }
    }
    private void push(Player p, NpcData.Definition d) {
        message(p, NpcText.plain(d.name()) + ": Sudah kubilang istirahat dulu! Hus, hus!" );
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
            ItemStack[] before = Arrays.stream(p.getInventory().getStorageContents()).map(i -> i == null ? null : i.clone()).toArray(ItemStack[]::new);
            var dropped = new ArrayList<Item>();
            String marker = p.getPersistentDataContainer().get(awardKey, PersistentDataType.STRING);
            try {
                if (!award.receipt().toString().equals(marker)) {
                    ItemStack[] items = award.reward().items().stream().map(NpcService::decode).toArray(ItemStack[]::new);
                    for (ItemStack item : p.getInventory().addItem(items).values()) dropped.add(dropAward(p, item, award.receipt()));
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
    private Item dropAward(Player player, ItemStack stack, UUID receipt) {
        Item item = player.getWorld().dropItem(player.getLocation(), stack.clone());
        item.setOwner(player.getUniqueId()); item.setPickupDelay(20);
        item.setUnlimitedLifetime(true); item.setInvulnerable(true);
        item.getPersistentDataContainer().set(awardKey, PersistentDataType.STRING, receipt.toString()); return item;
    }
    private void notifyAward(Player p, NpcData.Award award) {
        if (award.receipt().toString().equals(p.getPersistentDataContainer().get(awardKey, PersistentDataType.STRING))) return;
        p.getPersistentDataContainer().set(awardKey, PersistentDataType.STRING, award.receipt().toString()); p.saveData();
        p.sendMessage(NpcText.name(award.npcName()).append(Component.text(": " + (award.reward().viti().signum() > 0
                ? "Aku memberikan " + VitiAmount.format(award.reward().viti()) + " Viti kepadamu." : "Hadiahku sudah diberikan. Item yang tidak muat jatuh di dekatmu."), NamedTextColor.WHITE)));
        sound(p, "minecraft:entity.player.levelup", .4f, 1);
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
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void move(PlayerMoveEvent e) {
        if (internalTeleport.contains(e.getPlayer().getUniqueId())) return;
        Session s = sessions.get(e.getPlayer().getUniqueId());
        if (s != null && !(e instanceof PlayerTeleportEvent)) e.setTo(s.anchor.clone());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void trigger(PlayerMoveEvent e) {
        Player player = e.getPlayer(); UUID owner = player.getUniqueId();
        if (e.isCancelled() || e instanceof PlayerTeleportEvent || internalTeleport.contains(owner) || e.getTo() == null || !e.hasChangedPosition()
                || sessions.containsKey(owner) || queuedTriggers.contains(owner) || quests.run(owner) != null
                || ticks < triggerRetry.getOrDefault(owner, 0L)) return;
        for (var d : state.npcs().values()) if (d.enabled() && d.trigger() != null && !state.done(e.getPlayer().getUniqueId(), d)
                && (d.trigger().contains(e.getTo().getWorld().getUID(), e.getTo().getX(), e.getTo().getY(), e.getTo().getZ())
                || d.trigger().crossed(e.getFrom().getWorld().getUID(), e.getFrom().getX(), e.getFrom().getY(), e.getFrom().getZ(), e.getTo().getX(), e.getTo().getY(), e.getTo().getZ()))) {
            queuedTriggers.add(owner);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!queuedTriggers.remove(owner) || closed || !player.isOnline() || player.isDead()) return;
                NpcData.Definition live = state.npcs().get(d.id());
                if (live == null || !live.enabled() || !live.token().equals(d.token()) || live.trigger() == null || state.done(owner, live)) return;
                try { begin(player, live, true, false, null); }
                catch (RuntimeException error) {
                    Session session = sessions.get(owner); if (session != null) end(session, false);
                    triggerRetry.put(owner, ticks + 40); message(player, "Trigger dialog gagal; pemain sudah dilepas. Minta admin memeriksa log."); log(error);
                }
            });
            break;
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void externalTeleport(PlayerTeleportEvent e) {
        if (internalTeleport.contains(e.getPlayer().getUniqueId())) return;
        queuedTriggers.remove(e.getPlayer().getUniqueId());
        Session s = sessions.get(e.getPlayer().getUniqueId()); if (s != null) end(s, false);
        quests.cancel(e.getPlayer().getUniqueId(), "Teleport membatalkan quest.", false); renderer.forget(e.getPlayer());
    }
    @EventHandler(priority = EventPriority.LOWEST) public void held(PlayerItemHeldEvent e) {
        Session s = sessions.get(e.getPlayer().getUniqueId()); if (s == null) return; e.setCancelled(true);
        if (s.phase != 2 || s.shown < s.node.text().codePointCount(0, s.node.text().length())) return;
        int delta = Math.floorMod(e.getNewSlot() - e.getPreviousSlot() + 4, 9) - 4;
        if (delta == 0) return; s.selected = Math.floorMod(s.selected + (delta > 0 ? 1 : -1), s.node.available().size());
        sound(s.player, "minecraft:ui.button.click", .3f, 1.2f); render(s);
    }
    @EventHandler(priority = EventPriority.LOWEST) public void sneak(PlayerToggleSneakEvent e) {
        Session s = sessions.get(e.getPlayer().getUniqueId()); if (s != null && e.isSneaking()) { e.setCancelled(true); end(s, true); }
    }
    @EventHandler(priority = EventPriority.LOWEST) public void swing(PlayerAnimationEvent e) {
        if (e.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        if (gates.selecting(e.getPlayer().getUniqueId()) || com.bangzachery.vitae.vitaemanager.selection.SelectionService.selecting(e.getPlayer())) return;
        Player p = e.getPlayer(); if (sessions.containsKey(p.getUniqueId())) { e.setCancelled(true); confirm(p); return; }
        var hit = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getEyeLocation().getDirection(), 4.5, .1,
                entity -> entity != p && renderer.id(entity) != null);
        if (hit != null && hit.getHitEntity() != null) modelClick(p, renderer.id(hit.getHitEntity()), true);
    }
    @EventHandler(priority = EventPriority.LOWEST) public void interact(PlayerInteractEvent e) {
        if (!sessions.containsKey(e.getPlayer().getUniqueId())) return; e.setCancelled(true);
        if (e.getHand() == EquipmentSlot.HAND && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) confirm(e.getPlayer());
    }
    @EventHandler(priority = EventPriority.LOWEST) public void entityInteract(PlayerInteractEntityEvent e) {
        if(com.bangzachery.vitae.vitaemanager.selection.SelectionService.selecting(e.getPlayer()))return;
        String id = renderer.id(e.getRightClicked());
        boolean dialog = sessions.containsKey(e.getPlayer().getUniqueId());
        if (id == null && !dialog) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (dialog) confirm(e.getPlayer()); else modelClick(e.getPlayer(), id, false);
    }
    @EventHandler(priority = EventPriority.LOWEST) public void entityInteractAt(PlayerInteractAtEntityEvent e) { entityInteract(e); }
    @EventHandler(priority = EventPriority.LOWEST) public void virtualInteract(PlayerUseUnknownEntityEvent e) {
        if(com.bangzachery.vitae.vitaemanager.selection.SelectionService.selecting(e.getPlayer()))return;
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
    @EventHandler public void quit(PlayerQuitEvent e) { disconnect(e.getPlayer()); clicks.remove(e.getPlayer().getUniqueId()); warned.remove(e.getPlayer().getUniqueId()); triggerRetry.remove(e.getPlayer().getUniqueId()); }
    @EventHandler public void respawn(PlayerRespawnEvent e) { renderer.forget(e.getPlayer()); }
    @EventHandler public void world(PlayerChangedWorldEvent e) { renderer.forget(e.getPlayer()); }
    @EventHandler public void worldLoad(WorldLoadEvent e) { renderer.refresh(state.npcs()); for (var d : state.npcs().values()) if (d.enabled() && !quests.uses(d.id())) quests.restore(d); }
    private void disconnect(Player p) { queuedTriggers.remove(p.getUniqueId()); BukkitTask push = pushes.remove(p.getUniqueId()); if (push != null) push.cancel(); Session s = sessions.get(p.getUniqueId()); if (s != null) end(s, false); quests.cancel(p.getUniqueId(), "", false); renderer.forget(p); }
    private static void sound(Player p, String sound, float volume, float pitch) { p.playSound(p.getLocation(), sound, volume, pitch); }
    public static void message(Player p, String text) { p.sendMessage(Component.text("[Vitae NPC] " + Objects.requireNonNullElse(text, "Setup gagal."), NamedTextColor.YELLOW)); }
    private void log(Exception e) { plugin.getLogger().log(Level.SEVERE, "Operasi NPC gagal; progres/hadiah belum dihapus.", e); }
    @Override public void close() {
        if (closed) return; closed = true; if (task != null) task.cancel();
        for (Player p : Bukkit.getOnlinePlayers()) if (p.getOpenInventory().getTopInventory().getHolder() instanceof RewardMenu) p.closeInventory();
        for (Session s : List.copyOf(sessions.values())) end(s, false);
        for (BukkitTask push : pushes.values()) push.cancel(); pushes.clear();
        queuedTriggers.clear(); triggerRetry.clear(); shops.close(); gates.close(); quests.close(); renderer.close(); HandlerList.unregisterAll(this);
        farmSetups.clear();
    }
}
