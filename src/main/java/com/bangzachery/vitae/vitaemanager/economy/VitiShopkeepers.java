package com.bangzachery.vitae.vitaemanager.economy;

import com.bangzachery.vitae.vitaemanager.core.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

/** Optional integration through the public Shopkeepers API; no extra runtime plugin is required. */
public final class VitiShopkeepers implements Listener, AutoCloseable {
    private final JavaPlugin plugin;
    private final VitiService service;
    private final VitiPaper paper;
    private final MessageService messages;
    private final Map<Event, Set<UUID>> payments = new WeakHashMap<>();
    private Plugin shops;
    private Class<?> adminType, tradingType;
    private Method adminOffers, adminSetOffers, tradingOffers, tradingSetOffers;
    private Method offerItem1, offerItem2, offerResult, offerCreate, copyItem;
    private Method openShop, editedShop, tradeShop, tradePlayer, tradeRecipe;
    private Method offered1, offered2, recipe1, recipe2, completedTrade, saveShop;
    private BukkitTask startup;
    private boolean backedUp;

    public VitiShopkeepers(JavaPlugin plugin, VitiService service, VitiPaper paper, MessageService messages) {
        this.plugin = plugin; this.service = service; this.paper = paper; this.messages = messages;
    }

    public void start() {
        shops = Bukkit.getPluginManager().getPlugin("Shopkeepers");
        if (shops == null || !shops.isEnabled()) return;
        try {
            ClassLoader loader = shops.getClass().getClassLoader();
            Class<?> api = api(loader, "ShopkeepersAPI");
            Class<?> shopType = api(loader, "shopkeeper.Shopkeeper");
            adminType = api(loader, "shopkeeper.admin.regular.RegularAdminShopkeeper");
            tradingType = api(loader, "shopkeeper.player.trade.TradingPlayerShopkeeper");
            adminOffers = adminType.getMethod("getOffers");
            adminSetOffers = adminType.getMethod("setOffers", List.class);
            tradingOffers = tradingType.getMethod("getOffers");
            tradingSetOffers = tradingType.getMethod("setOffers", List.class);
            saveShop = shopType.getMethod("saveDelayed");
            Class<?> offer = api(loader, "shopkeeper.offers.TradeOffer");
            offerItem1 = offer.getMethod("getItem1"); offerItem2 = offer.getMethod("getItem2");
            offerResult = offer.getMethod("getResultItem");
            offerCreate = offer.getMethod("create", ItemStack.class, ItemStack.class, ItemStack.class);
            copyItem = api(loader, "util.UnmodifiableItemStack").getMethod("copy");
            Class<? extends Event> open = event(loader, "ShopkeeperOpenUIEvent");
            Class<? extends Event> edited = event(loader, "ShopkeeperEditedEvent");
            Class<? extends Event> trade = event(loader, "ShopkeeperTradeEvent");
            Class<? extends Event> completed = event(loader, "ShopkeeperTradeCompletedEvent");
            openShop = open.getMethod("getShopkeeper"); editedShop = edited.getMethod("getShopkeeper");
            tradeShop = trade.getMethod("getShopkeeper"); tradePlayer = trade.getMethod("getPlayer");
            tradeRecipe = trade.getMethod("getTradingRecipe");
            offered1 = trade.getMethod("getOfferedItem1"); offered2 = trade.getMethod("getOfferedItem2");
            Class<?> recipe = api(loader, "shopkeeper.TradingRecipe");
            recipe1 = recipe.getMethod("getItem1"); recipe2 = recipe.getMethod("getItem2");
            completedTrade = completed.getMethod("getCompletedTrade");
            register(open, EventPriority.HIGHEST, true, e -> normalize(invoke(openShop, e)));
            register(edited, EventPriority.MONITOR, false, e -> normalize(invoke(editedShop, e)));
            register(trade, EventPriority.HIGHEST, true, this::validate);
            register(completed, EventPriority.MONITOR, false, this::completed);
            Object registry = invoke(api.getMethod("getShopkeeperRegistry"), null);
            Method all = api(loader, "shopkeeper.ShopkeeperRegistry").getMethod("getAllShopkeepers");
            startup = Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    for (Object shop : (Collection<?>) invoke(all, registry)) normalize(shop);
                } catch (RuntimeException error) { log(error); }
            });
            if (shops.getConfig().getBoolean("use-strict-item-comparison", false)) {
                plugin.getLogger().warning("Harga Viti memerlukan use-strict-item-comparison: false di config Shopkeepers. Nominal dan serial tetap diperiksa Vitaemanager.");
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            close();
            plugin.getLogger().log(Level.SEVERE, "Integrasi harga Viti dengan API Shopkeepers belum dapat dipasang.", error);
        }
    }

    private static Class<?> api(ClassLoader loader, String name) throws ClassNotFoundException {
        return Class.forName("com.nisovin.shopkeepers.api." + name, true, loader);
    }
    private static Class<? extends Event> event(ClassLoader loader, String name) throws ClassNotFoundException {
        return api(loader, "events." + name).asSubclass(Event.class);
    }
    private static Object invoke(Method method, Object receiver, Object... args) {
        try { return method.invoke(receiver, args); }
        catch (InvocationTargetException error) { throw new IllegalStateException("Shopkeepers: " + method.getName(), error.getCause()); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException("Shopkeepers: " + method.getName(), error); }
    }
    private ItemStack copy(Object view) { return view == null ? null : (ItemStack) invoke(copyItem, view); }

    private void register(Class<? extends Event> type, EventPriority priority, boolean ignoreCancelled, Consumer<Event> handler) {
        Bukkit.getPluginManager().registerEvent(type, this, priority, (listener, event) -> {
            if (!type.isInstance(event) || !shops.isEnabled()) return;
            try { handler.accept(event); }
            catch (RuntimeException error) {
                if (event instanceof Cancellable cancelled) cancelled.setCancelled(true);
                log(error);
            }
        }, plugin, ignoreCancelled);
    }

    private void normalize(Object shop) {
        boolean admin = adminType.isInstance(shop);
        if (!admin && !tradingType.isInstance(shop)) return;
        List<?> offers = (List<?>) invoke(admin ? adminOffers : tradingOffers, shop);
        List<Object> replacements = new ArrayList<>(offers.size());
        boolean changed = false;
        for (Object offer : offers) {
            ItemStack first = copy(invoke(offerItem1, offer)), second = copy(invoke(offerItem2, offer));
            ItemStack cost1 = paper.shopCost(first), cost2 = paper.shopCost(second);
            if (!java.util.Objects.equals(first, cost1) || !java.util.Objects.equals(second, cost2)) {
                replacements.add(invoke(offerCreate, null, copy(invoke(offerResult, offer)), cost1, cost2));
                changed = true;
            } else replacements.add(offer);
        }
        if (!changed) return;
        backup();
        invoke(admin ? adminSetOffers : tradingSetOffers, shop, replacements);
        invoke(saveShop, shop);
    }

    private void backup() {
        if (backedUp) return;
        Path file = shops.getDataFolder().toPath().resolve("save.yml");
        try {
            if (Files.exists(file)) Files.copy(file, file.resolveSibling("save-viti-backup-" + System.currentTimeMillis() + ".yml"));
            backedUp = true;
        } catch (IOException error) { throw new IllegalStateException("Backup harga Shopkeepers gagal; harga belum diubah.", error); }
    }

    private void validate(Event event) {
        Player player = (Player) invoke(tradePlayer, event);
        Object recipe = invoke(tradeRecipe, event);
        ItemStack[] costs = {copy(invoke(recipe1, recipe)), copy(invoke(recipe2, recipe))};
        if (!paper.isShopCost(costs[0]) && !paper.isShopCost(costs[1])) return;
        ItemStack[] offered = {copy(invoke(offered1, event)), copy(invoke(offered2, event))};
        Set<UUID> serials = new HashSet<>();
        try {
            for (int i = 0; i < costs.length; i++) {
                if (!paper.isShopCost(costs[i])) continue;
                VitiPaper.Value value = service.checkShopPaper(offered[i], paper.shopDenomination(costs[i]));
                if (value.serial() != null && !serials.add(value.serial())) throw new VitiLedger.Failure("viti-note-invalid");
            }
            // Player barter shops retain the paid banknotes in their container. Only an admin
            // shop destroys its income, so those serials become spent after the trade succeeds.
            if (adminType.isInstance(invoke(tradeShop, event)) && !serials.isEmpty()) {
                payments.put(event, Set.copyOf(serials));
            }
        } catch (VitiLedger.Failure error) {
            ((Cancellable) event).setCancelled(true);
            messages.send(player, error.key());
        } catch (IllegalArgumentException error) {
            ((Cancellable) event).setCancelled(true);
            messages.send(player, "viti-note-invalid");
        }
    }

    private void completed(Event event) {
        Event trade = (Event) invoke(completedTrade, event);
        Set<UUID> ids = payments.remove(trade);
        if (ids != null) service.recordShopPayment(ids);
    }

    private void log(Throwable error) { plugin.getLogger().log(Level.SEVERE, "Integrasi kertas Viti/Shopkeepers gagal.", error); }

    @Override public void close() {
        if (startup != null) startup.cancel();
        HandlerList.unregisterAll(this);
        payments.clear();
    }
}
