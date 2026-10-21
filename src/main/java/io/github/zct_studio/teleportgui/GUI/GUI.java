package io.github.zct_studio.teleportgui.gui;

import io.github.zct_studio.teleportgui.ServerMain;
import io.github.zct_studio.teleportgui.Styles;
import io.github.zct_studio.teleportgui.Tr_i18n;
import io.github.zct_studio.teleportgui.commands.Home;
import io.github.zct_studio.teleportgui.commands.Tpa;
import io.github.zct_studio.teleportgui.commands.Warp;
import io.github.zct_studio.teleportgui.gui.menu.anvil.Create;
import io.github.zct_studio.teleportgui.gui.menu.anvil.Rename;
import io.github.zct_studio.teleportgui.gui.menu.hopper.SecondlyActions;
import io.github.zct_studio.teleportgui.storage.Manager;
import io.github.zct_studio.teleportgui.types.NamedLoc;
import io.github.zct_studio.teleportgui.types.PlayerData;
import io.github.zct_studio.teleportgui.utils.CommandGuard;
import io.github.zct_studio.teleportgui.utils.TeleportUtils;
import io.github.zct_studio.teleportgui.utils.config.CommandLoader;
import io.github.zct_studio.teleportgui.utils.config.ConfigKeys;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("unused")
public final class GUI {
    private static final int MAX_CONTENT_ROWS = 5;
    private static final int NAV_GUIDE = 0;
    private static final int NAV_EXTRA = 1;
    private static final int NAV_REQUESTS = 2;
    private static final int NAV_BACK = 6;
    private static final int NAV_PREV = 7;
    private static final int NAV_NEXT = 8;
    private static final int TPA_PLAYERS = 0;
    private static final int TPA_REQUESTS = 1;
    private GUI() {
    }

    private static boolean bedrockHopper(ServerPlayer player) {
        return TeleportUtils.isBedrockPlayer(player) && !TeleportUtils.hasFloodgate();
    }

    private static boolean bedrockForm(ServerPlayer player) {
        return TeleportUtils.isBedrockPlayer(player) && TeleportUtils.hasFloodgate();
    }

    public static void openHome(ServerPlayer player) {
        openHome(player, 0);
    }

    public static void openHome(ServerPlayer player, int page) {
        if (bedrockForm(player)) {
            Form.openHomeForm(player);
            return;
        }
        player.openMenu(new HomeMenuProvider(player, page, bedrockHopper(player)));
    }

    public static void openWarp(ServerPlayer player) {
        openWarp(player, 0);
    }

    public static void openWarp(ServerPlayer player, int page) {
        if (bedrockForm(player)) {
            Form.openWarpForm(player);
            return;
        }
        player.openMenu(new WarpMenuProvider(player, page, bedrockHopper(player)));
    }

    public static void openTpa(ServerPlayer player) {
        openTpa(player, 0, TPA_PLAYERS);
    }

    public static void openTpaRequests(ServerPlayer player) {
        openTpa(player, 0, TPA_REQUESTS);
    }

    public static void openTpa(ServerPlayer player, int page, int mode) {
        if (bedrockForm(player)) {
            Form.openTpaForm(player);
            return;
        }
        player.openMenu(new TpaMenuProvider(player, page, mode, bedrockHopper(player)));
    }

    public static MutableComponent Tr(ServerPlayer player, String key, String... args) {
        MutableComponent[] comps = new MutableComponent[args.length];
        for (int i = 0; i < args.length; i++) comps[i] = Component.literal(args[i]);
        return Tr_i18n.tr(key, player, comps);
    }

    public static ItemStack stack(Item item, Component name, List<? extends Component> lore) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponents.ITEM_NAME, name);
        if (lore != null && !lore.isEmpty()) {
            s.set(DataComponents.LORE, new ItemLore(new ArrayList<>(lore)));
        }
        return s;
    }

    public static ItemStack stackGlint(Item item, Component name, List<? extends Component> lore) {
        ItemStack s = stack(item, name, lore);
        s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return s;
    }

    private static ItemStack playerHead(ServerPlayer owner, UUID fallbackId,
                                        Component name, List<? extends Component> lore,
                                        boolean glint) {
        ItemStack s = glint
                ? stackGlint(Items.PLAYER_HEAD, name, lore)
                : stack(Items.PLAYER_HEAD, name, lore);
        ResolvableProfile profile = null;
        if (owner != null) {
            profile = ResolvableProfile.createResolved(owner.getGameProfile());
        } else if (fallbackId != null) {
            profile = ResolvableProfile.createUnresolved(fallbackId);
        }
        if (profile != null) {
            s.set(DataComponents.PROFILE, profile);
        }
        if (glint) {
            s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return s;
    }

    private static int contentRowsFor(int count) {
        //noinspection MathClampMigration
        return Math.min(MAX_CONTENT_ROWS, Math.max(1, (count + 8) / 9));
    }

    private static int rowsFor(int count) {
        return contentRowsFor(count) + 1; // +1 导航行
    }

    private static int totalPagesFor(int count, boolean hasAdd) {
        int contentSize = contentRowsFor(count) * 9;
        if (!hasAdd) return Math.max(1, (count + contentSize - 1) / contentSize);
        int firstCap = contentSize - 1;
        int rest = Math.max(0, count - firstCap);
        return 1 + (rest + contentSize - 1) / contentSize;
    }

    private static MenuType<?> menuTypeFor(int count) {
        return switch (rowsFor(count)) {
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }

    private static MutableComponent titled(ServerPlayer player, String baseKey, int count, int page, boolean hasAdd) {
        return Tr(player, "gui.teleportgui.universal.gui.titlePage",
                Tr_i18n.text(baseKey, player),
                String.valueOf(page + 1),
                String.valueOf(totalPagesFor(count, hasAdd)));
    }

    private static List<Component> infoLore(NamedLoc loc) {
        return List.of(
                Component.literal(String.format("X%d Y%d Z%d", loc.getX(), loc.getY(), loc.getZ()))
                        .withStyle(Styles.MUTED),
                Component.literal(loc.getWorldId()).withStyle(Styles.SUBTLE));
    }

    static List<Component> guideLore(ServerPlayer player, String key) {
        List<Component> lines = new ArrayList<>();
        for (String line : Tr_i18n.raw(key, player).split("\n")) {
            lines.add(Component.literal(line).withStyle(Styles.MUTED));
        }
        return lines;
    }

    private static List<ServerPlayer> onlineOthers(ServerPlayer player) {
        var server = ServerMain.SERVER;
        if (server == null) return List.of();
        return server.getPlayerList().getPlayers().stream()
                .filter(p -> !p.getUUID().equals(player.getUUID()))
                .toList();
    }

    static void openHomeActions(ServerPlayer sp, String home, int page) {
        SecondlyActions.Action[] slots = new SecondlyActions.Action[5];
        slots[0] = new SecondlyActions.Action(
                stack(Items.FEATHER, Tr(sp, "gui.teleportgui.universal.gui.back"), null),
                () -> openHome(sp, page));
        slots[1] = new SecondlyActions.Action(
                stack(Items.ENDER_PEARL, Tr(sp, "gui.teleportgui.universal.gui.tp"), null),
                () -> {
                    Home.goHome(sp, home);
                    sp.closeContainer();
                });
        slots[2] = new SecondlyActions.Action(
                stack(Items.BARRIER, Tr(sp, "gui.teleportgui.universal.gui.del"), null),
                () -> {
                    Home.deleteHome(sp, home);
                    openHome(sp, page);
                });
        slots[3] = new SecondlyActions.Action(
                stack(Items.NETHER_STAR, Tr(sp, "gui.teleportgui.home.homeactiongui.defaulthome"), null),
                () -> {
                    Home.setDefaultHome(sp, home);
                    openHome(sp, page);
                });
        slots[4] = new SecondlyActions.Action(
                stack(Items.NAME_TAG, Tr(sp, "gui.teleportgui.universal.gui.rename"), null),
                () -> Rename.open(sp, home, false, page));
        SecondlyActions.open(sp, Tr(sp, "gui.teleportgui.home.homegui.title"), slots, () -> openHome(sp, page));
    }

    static void openWarpActions(ServerPlayer sp, String warp, int page) {
        boolean canDelete = CommandGuard.hasPermission(sp, ConfigKeys.MODULE_WARP, ConfigKeys.WARP_DEL);
        boolean canRename = CommandGuard.hasPermission(sp, ConfigKeys.MODULE_WARP, ConfigKeys.WARP_RENAME);
        SecondlyActions.Action[] slots = new SecondlyActions.Action[5];
        slots[0] = new SecondlyActions.Action(
                stack(Items.FEATHER, Tr(sp, "gui.teleportgui.universal.gui.back"), null),
                () -> openWarp(sp, page));
        slots[1] = new SecondlyActions.Action(
                stack(Items.ENDER_PEARL, Tr(sp, "gui.teleportgui.universal.gui.tp"), null),
                () -> {
                    Warp.goToWarp(sp, warp);
                    sp.closeContainer();
                });
        if (canDelete) {
            slots[2] = new SecondlyActions.Action(
                    stack(Items.BARRIER, Tr(sp, "gui.teleportgui.universal.gui.del"), null),
                    () -> {
                        Warp.deleteWarp(sp, warp);
                        openWarp(sp, page);
                    });
        }
        if (canRename) {
            slots[3] = new SecondlyActions.Action(
                    stack(Items.NAME_TAG, Tr(sp, "gui.teleportgui.universal.gui.rename"), null),
                    () -> Rename.open(sp, warp, true, page));
        }
        SecondlyActions.open(sp, Tr(sp, "gui.teleportgui.warp.warpgui.title"), slots, () -> openWarp(sp, page));
    }

    static void openTpaPlayerActions(ServerPlayer sp, String targetName, int page, int mode) {
        SecondlyActions.Action[] slots = new SecondlyActions.Action[5];
        slots[0] = new SecondlyActions.Action(
                stack(Items.ENDER_PEARL, Tr(sp, "gui.teleportgui.tpa.tpagui.typeTpa"), null),
                () -> {
                    Tpa.requestByName(sp, targetName, false);
                    sp.closeContainer();
                });
        slots[2] = new SecondlyActions.Action(
                stack(Items.FEATHER, Tr(sp, "gui.teleportgui.universal.gui.back"), null),
                () -> openTpa(sp, page, mode));
        slots[4] = new SecondlyActions.Action(
                stack(Items.ENDER_EYE, Tr(sp, "gui.teleportgui.tpa.tpagui.typeHere"), null),
                () -> {
                    Tpa.requestByName(sp, targetName, true);
                    sp.closeContainer();
                });
        SecondlyActions.open(sp, Tr(sp, "gui.teleportgui.tpa.tpagui.title"), slots,
                () -> openTpa(sp, page, mode));
    }

    static void openTpaRequestActions(ServerPlayer sp, String fromUuid) {
        SecondlyActions.Action[] slots = new SecondlyActions.Action[5];
        slots[0] = new SecondlyActions.Action(
                stack(Items.BARRIER, Tr(sp, "gui.teleportgui.tpa.tpareqdialog.deny"), null),
                () -> denyRequest(sp, fromUuid));
        slots[2] = new SecondlyActions.Action(
                stack(Items.FEATHER, Tr(sp, "gui.teleportgui.universal.gui.back"), null),
                () -> openTpa(sp, 0, TPA_REQUESTS));
        slots[4] = new SecondlyActions.Action(
                stack(Items.EMERALD, Tr(sp, "gui.teleportgui.tpa.tpareqdialog.accept"), null),
                () -> acceptRequest(sp, fromUuid));
        SecondlyActions.open(sp, Tr(sp, "gui.teleportgui.tpa.tpareqdialog.title"), slots,
                () -> openTpa(sp, 0, TPA_REQUESTS));
    }

    public static void openSafetyConfirm(ServerPlayer sp, Runnable onForce) {
        SecondlyActions.Action[] slots = new SecondlyActions.Action[5];
        slots[0] = new SecondlyActions.Action(
                stack(Items.BARRIER, Tr(sp, "gui.teleportgui.universal.gui.cancel"), null),
                sp::closeContainer);
        slots[4] = new SecondlyActions.Action(
                stack(Items.ENDER_PEARL, Tr(sp, "commands.teleportgui.common.forceTeleport"), null),
                () -> {
                    onForce.run();
                    sp.closeContainer();
                });
        SecondlyActions.open(sp, Tr(sp, "commands.teleportgui.common.noSafeLocation"), slots, null);
    }

    private static void acceptRequest(ServerPlayer sp, String fromUuid) {
        ServerPlayer from = Tpa.resolveOnline(fromUuid);
        if (from != null) Tpa.accept(from, sp);
        else sp.sendSystemMessage(Tr(sp, "commands.teleportgui.tpa.notFound").withStyle(Styles.ERROR), true);
        sp.closeContainer();
    }

    private static void denyRequest(ServerPlayer sp, String fromUuid) {
        ServerPlayer from = Tpa.resolveOnline(fromUuid);
        if (from != null) Tpa.deny(from, sp);
        else sp.sendSystemMessage(Tr(sp, "commands.teleportgui.tpa.notFound").withStyle(Styles.ERROR), true);
        sp.closeContainer();
    }

    private abstract static class PagedMenu extends ChestMenu {

        protected final ServerPlayer player;
        protected final int contentRows;
        protected final int contentSize;
        protected final int totalPages;
        protected final int page;
        protected final String[] slotNames;

        PagedMenu(MenuType<?> type, int windowId, Inventory inv, int rows,
                  int entryCount, ServerPlayer player, int page) {
            super(type, windowId, inv, new SimpleContainer(rows * 9), rows);
            this.player = player;
            this.contentRows = rows - 1;
            this.contentSize = this.contentRows * 9;
            this.totalPages = totalPagesFor(entryCount, hasAdd());
            //noinspection MathClampMigration
            this.page = Math.max(0, Math.min(page, this.totalPages - 1));
            this.slotNames = new String[rows * 9];
        }

        private static void resyncInventory(ServerPlayer sp) {
            sp.inventoryMenu.sendAllDataToRemote();
        }

        protected abstract void fillContent(SimpleContainer container);

        protected abstract void onEntryClick(ServerPlayer sp, String id, int button, ContainerInput input);

        protected abstract void reopen(ServerPlayer sp, int page);

        protected abstract String guideKey();

        @SuppressWarnings("UnusedReturnValue")
        protected boolean onSwap(ServerPlayer sp, String id) {
            return false;
        }

        protected void fillExtraNav(SimpleContainer c, int base) {
        }

        @SuppressWarnings("UnusedReturnValue")
        protected boolean onExtraNav(ServerPlayer sp) {
            return false;
        }

        protected boolean onRequestsNav(ServerPlayer sp) {
            return false;
        }

        protected boolean hasAdd() {
            return false;
        }

        protected boolean addLimitReached() {
            return false;
        }

        protected void onAdd(ServerPlayer sp, int page) {
        }

        protected final int slotBase() {
            return (hasAdd() && page == 0) ? 1 : 0;
        }

        protected final int entryStart() {
            if (!hasAdd()) return page * contentSize;
            return page == 0 ? 0 : (contentSize - 1) + (page - 1) * contentSize;
        }

        protected final int pageCapacity() {
            return (hasAdd() && page == 0) ? contentSize - 1 : contentSize;
        }

        protected final void build() {
            SimpleContainer c = (SimpleContainer) getContainer();
            fillContent(c);
            fillNav(c);
        }

        private void fillNav(SimpleContainer c) {
            int base = contentRows * 9;
            c.setItem(base + NAV_GUIDE, stack(Items.WRITTEN_BOOK,
                    Tr(player, "gui.teleportgui.universal.guide"), guideLore(player, guideKey())));
            fillExtraNav(c, base);
            c.setItem(base + NAV_BACK, stack(Items.BARRIER,
                    Tr(player, "gui.teleportgui.universal.gui.close"), null));
            c.setItem(base + NAV_PREV, stack(Items.ARROW,
                    Tr(player, "gui.teleportgui.universal.gui.pgup"), null));
            c.setItem(base + NAV_NEXT, stack(Items.SPECTRAL_ARROW,
                    Tr(player, "gui.teleportgui.universal.gui.pgdn"), null));
        }

        @Override
        public void clicked(int slotIndex, int button, @NonNull ContainerInput input, @NonNull Player clicker) {
            if (!(clicker instanceof ServerPlayer sp)) {
                super.clicked(slotIndex, button, input, clicker);
                return;
            }

            if (input == ContainerInput.SWAP) {
                int navBase = contentRows * 9;
                boolean isContentSlot = slotIndex >= 0 && slotIndex < navBase
                        && !(hasAdd() && page == 0 && slotIndex == 0);
                if (isContentSlot && button == 40) {
                    String id = slotNames[slotIndex];
                    if (id != null) {
                        onSwap(sp, id);
                    }
                }
                resyncInventory(sp);
                return;
            }
            int size = (contentRows + 1) * 9;
            if (slotIndex < 0 || slotIndex >= size) {
                super.clicked(slotIndex, button, input, clicker);
                return;
            }

            int navBase = contentRows * 9;
            if (slotIndex >= navBase) {
                int off = slotIndex - navBase;
                if (off == NAV_GUIDE) {
                    sendGuide(sp);
                    sp.closeContainer();
                } else if (off == NAV_BACK) {
                    sp.closeContainer();
                } else if (off == NAV_PREV) {
                    reopen(sp, (page - 1 + totalPages) % totalPages);
                } else if (off == NAV_NEXT) {
                    reopen(sp, (page + 1) % totalPages);
                } else if (off == NAV_REQUESTS) {
                    onRequestsNav(sp);
                } else if (off == NAV_EXTRA) {
                    onExtraNav(sp);
                }
                return;
            }

            if (hasAdd() && page == 0 && slotIndex == 0) {
                onAdd(sp, page);
                return;
            }

            String id = slotNames[slotIndex];
            if (id == null) return;
            onEntryClick(sp, id, button, input);
        }

        private void sendGuide(ServerPlayer sp) {
            MutableComponent msg = Component.empty();
            String[] lines = Tr_i18n.raw(guideKey(), sp).split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) msg.append("\n");
                msg.append(Component.literal(lines[i]).withStyle(Styles.MUTED));
            }
            sp.sendSystemMessage(msg, false);
        }

        @Override
        public @NonNull ItemStack quickMoveStack(@NonNull Player player, int slotIndex) {
            return ItemStack.EMPTY;
        }
    }

    public static class HomeMenuProvider implements MenuProvider {
        private final ServerPlayer player;
        private final int page;
        private final boolean hopper;

        public HomeMenuProvider(ServerPlayer player, int page, boolean hopper) {
            this.player = player;
            this.page = page;
            this.hopper = hopper;
        }

        @Override
        public @NonNull Component getDisplayName() {
            int count = Manager.getPlayer(player.getStringUUID())
                    .map(d -> d.getHomes().size()).orElse(0);
            return titled(player, "gui.teleportgui.home.homegui.title", count, page, true);
        }

        @Override
        public AbstractContainerMenu createMenu(int windowId, @NonNull Inventory inv, @NonNull Player p) {
            Optional<PlayerData> opt = Manager.getPlayer(player.getStringUUID());
            List<NamedLoc> homes = opt.map(PlayerData::getHomes).orElseGet(List::of);
            String def = opt.map(PlayerData::getDefaultHome).orElse("");
            return new HomeMenu(windowId, inv, player, homes, def, page, hopper);
        }
    }

    public static class HomeMenu extends PagedMenu {
        private final List<NamedLoc> homes;
        private final String def;
        private final boolean hopper;

        HomeMenu(int windowId, Inventory inv, ServerPlayer player, List<NamedLoc> homes, String def,
                 int page, boolean hopper) {
            super(menuTypeFor(homes.size()), windowId, inv, rowsFor(homes.size()), homes.size(), player, page);
            this.homes = homes;
            this.def = def;
            this.hopper = hopper;
            build();
        }

        @Override
        protected boolean hasAdd() {
            return true;
        }

        @Override
        protected boolean addLimitReached() {
            int max = CommandLoader.getMaxCount(ConfigKeys.MODULE_HOME, ConfigKeys.HOME_CREATE);
            return max >= 0 && homes.size() >= max;
        }

        @Override
        protected void onAdd(ServerPlayer sp, int page) {
            if (addLimitReached()) {
                sendLimitReached(sp);
                return;
            }
            Create.open(sp, false, page);
        }

        @Override
        protected void fillContent(SimpleContainer c) {
            fillAddSlot(c);
            int start = entryStart();
            int base = slotBase();
            int cap = pageCapacity();
            for (int i = 0; i < cap && start + i < homes.size(); i++) {
                NamedLoc home = homes.get(start + i);
                int slot = base + i;
                slotNames[slot] = home.getName();
                MutableComponent name = Component.literal(home.getName()).withStyle(Styles.INFO);
                boolean isDefault = home.getName().equals(def);
                if (isDefault) {
                    name.append(Tr(player, "gui.teleportgui.home.homegui.defaulthome"));
                }
                c.setItem(slot, stack(isDefault ? Items.BED.red() : Items.BED.white(),
                        name, infoLore(home)));
            }
        }

        private void fillAddSlot(SimpleContainer c) {
            if (!hasAdd() || page != 0) return;
            if (addLimitReached()) {
                c.setItem(0, stack(Items.BARRIER,
                        Tr(player, "commands.teleportgui.common.maxCountReached",
                                String.valueOf(addLimit())).withStyle(Styles.ERROR), null));
            } else {
                c.setItem(0, stack(Items.NETHER_STAR,
                        Tr(player, "gui.teleportgui.universal.gui.add").withStyle(Styles.SUCCESS),
                        homes.isEmpty()
                                ? List.of(Tr(player, "gui.teleportgui.home.homegui.empty")
                                .withStyle(Styles.MUTED))
                                : null));
            }
        }

        private int addLimit() {
            return CommandLoader.getMaxCount(ConfigKeys.MODULE_HOME, ConfigKeys.HOME_CREATE);
        }

        @Override
        protected void onEntryClick(ServerPlayer sp, String id, int button, ContainerInput input) {
            if (hopper) {
                if (button == 0) openHomeActions(sp, id, page);
                return;
            }
            if (input == ContainerInput.QUICK_MOVE) {
                Home.deleteHome(sp, id);
                reopen(sp, page);
            } else if (button == 0) {
                Home.goHome(sp, id);
                sp.closeContainer();
            } else if (button == 1) {
                Home.setDefaultHome(sp, id);
                reopen(sp, page);
            }
        }

        @Override
        protected boolean onSwap(ServerPlayer sp, String id) {
            Rename.open(sp, id, false, page);
            return true;
        }

        @Override
        protected void reopen(ServerPlayer sp, int page) {
            openHome(sp, page);
        }

        @Override
        protected String guideKey() {
            return "gui.teleportgui.guide.home";
        }

        private void sendLimitReached(ServerPlayer sp) {
            sp.sendSystemMessage(
                    Tr(sp, "commands.teleportgui.common.maxCountReached",
                            String.valueOf(addLimit())).withStyle(Styles.ERROR),
                    true);
        }
    }

    public static class WarpMenuProvider implements MenuProvider {
        private final ServerPlayer player;
        private final int page;
        private final boolean hopper;

        public WarpMenuProvider(ServerPlayer player, int page, boolean hopper) {
            this.player = player;
            this.page = page;
            this.hopper = hopper;
        }

        @Override
        public @NonNull Component getDisplayName() {
            return titled(player, "gui.teleportgui.warp.warpgui.title",
                    Manager.getWarps().size(), page, true);
        }

        @Override
        public AbstractContainerMenu createMenu(int windowId, @NonNull Inventory inv, @NonNull Player p) {
            return new WarpMenu(windowId, inv, player, Manager.getWarps(), page, hopper);
        }
    }

    public static class WarpMenu extends PagedMenu {
        private final List<NamedLoc> warps;
        private final boolean hopper;

        WarpMenu(int windowId, Inventory inv, ServerPlayer player, List<NamedLoc> warps,
                 int page, boolean hopper) {
            super(menuTypeFor(warps.size()), windowId, inv, rowsFor(warps.size()), warps.size(), player, page);
            this.warps = warps;
            this.hopper = hopper;
            build();
        }

        @Override
        protected boolean hasAdd() {
            return CommandGuard.hasPermission(player, ConfigKeys.MODULE_WARP, ConfigKeys.WARP_CREATE);
        }

        @Override
        protected boolean addLimitReached() {
            int max = addLimit();
            return max >= 0 && warps.size() >= max;
        }

        @Override
        protected void onAdd(ServerPlayer sp, int page) {
            if (!CommandGuard.hasPermission(sp, ConfigKeys.MODULE_WARP, ConfigKeys.WARP_CREATE)) {
                sp.sendSystemMessage(
                        Tr(sp, "commands.teleportgui.common.noPermission")
                                .withStyle(Styles.ERROR_STRONG),
                        true);
                return;
            }
            if (addLimitReached()) {
                sendLimitReached(sp);
                return;
            }
            Create.open(sp, true, page);
        }

        @Override
        protected void fillContent(SimpleContainer c) {
            fillAddSlot(c);
            int start = entryStart();
            int base = slotBase();
            int cap = pageCapacity();
            for (int i = 0; i < cap && start + i < warps.size(); i++) {
                NamedLoc warp = warps.get(start + i);
                int slot = base + i;
                slotNames[slot] = warp.getName();
                c.setItem(slot, stack(Items.ENDER_PEARL,
                        Component.literal(warp.getName()).withStyle(Styles.ACTION),
                        infoLore(warp)));
            }
        }

        private void fillAddSlot(SimpleContainer c) {
            if (!hasAdd() || page != 0) return;
            if (addLimitReached()) {
                c.setItem(0, stack(Items.BARRIER,
                        Tr(player, "commands.teleportgui.common.maxCountReached",
                                String.valueOf(addLimit())).withStyle(Styles.ERROR), null));
            } else {
                c.setItem(0, stack(Items.NETHER_STAR,
                        Tr(player, "gui.teleportgui.universal.gui.add").withStyle(Styles.SUCCESS),
                        warps.isEmpty()
                                ? List.of(Tr(player, "gui.teleportgui.warp.warpgui.empty")
                                .withStyle(Styles.MUTED))
                                : null));
            }
        }

        private int addLimit() {
            return CommandLoader.getMaxCount(ConfigKeys.MODULE_WARP, ConfigKeys.WARP_CREATE);
        }

        @Override
        protected void onEntryClick(ServerPlayer sp, String id, int button, ContainerInput input) {
            if (hopper) {
                if (button == 0) openWarpActions(sp, id, page);
                return;
            }
            if (input == ContainerInput.QUICK_MOVE) {
                if (CommandGuard.hasPermission(sp, ConfigKeys.MODULE_WARP, ConfigKeys.WARP_DEL)) {
                    Warp.deleteWarp(sp, id);
                    reopen(sp, page);
                } else {
                    sp.sendSystemMessage(
                            Tr(sp, "commands.teleportgui.common.noPermission")
                                    .withStyle(Styles.ERROR_STRONG),
                            true);
                }
            } else if (button == 0) {
                Warp.goToWarp(sp, id);
                sp.closeContainer();
            }
        }

        @Override
        protected boolean onSwap(ServerPlayer sp, String id) {
            if (!CommandGuard.hasPermission(sp, ConfigKeys.MODULE_WARP, ConfigKeys.WARP_RENAME)) {
                sp.sendSystemMessage(
                        Tr(sp, "commands.teleportgui.common.noPermission")
                                .withStyle(Styles.ERROR_STRONG),
                        true);
                return true;
            }
            Rename.open(sp, id, true, page);
            return true;
        }

        @Override
        protected void reopen(ServerPlayer sp, int page) {
            openWarp(sp, page);
        }

        @Override
        protected String guideKey() {
            return "gui.teleportgui.guide.warp";
        }

        private void sendLimitReached(ServerPlayer sp) {
            sp.sendSystemMessage(
                    Tr(sp, "commands.teleportgui.common.maxCountReached",
                            String.valueOf(addLimit())).withStyle(Styles.ERROR),
                    true);
        }
    }

    public static class TpaMenuProvider implements MenuProvider {
        private final ServerPlayer player;
        private final int page;
        private final int mode;
        private final boolean hopper;

        public TpaMenuProvider(ServerPlayer player, int page, int mode, boolean hopper) {
            this.player = player;
            this.page = page;
            this.mode = mode;
            this.hopper = hopper;
        }

        @Override
        public @NonNull Component getDisplayName() {
            int count = mode == TPA_REQUESTS
                    ? Tpa.getRequestsFor(player.getStringUUID()).size()
                    : onlineOthers(player).size();
            return titled(player, "gui.teleportgui.tpa.tpagui.title", count, page, false);
        }

        @Override
        public AbstractContainerMenu createMenu(int windowId, @NonNull Inventory inv, @NonNull Player p) {
            return new TpaMenu(windowId, inv, player, page, mode, hopper);
        }
    }

    public static class TpaMenu extends PagedMenu {
        private final int mode;
        private final boolean hopper;
        private final List<Tpa.TpaRequest> requests;
        private final List<ServerPlayer> others;

        TpaMenu(int windowId, Inventory inv, ServerPlayer player, int page, int mode, boolean hopper) {
            super(menuTypeFor(entryCount(player, mode)), windowId, inv,
                    rowsFor(entryCount(player, mode)), entryCount(player, mode), player, page);
            this.mode = mode;
            this.hopper = hopper;
            this.requests = Tpa.getRequestsFor(player.getStringUUID());
            this.others = onlineOthers(player);
            build();
        }

        private static int entryCount(ServerPlayer player, int mode) {
            return mode == TPA_REQUESTS
                    ? Tpa.getRequestsFor(player.getStringUUID()).size()
                    : onlineOthers(player).size();
        }

        private static String senderName(Tpa.TpaRequest req) {
            ServerPlayer from = Tpa.resolveOnline(req.fromUuid);
            return from == null ? req.fromUuid.substring(0, Math.min(8, req.fromUuid.length()))
                    : from.getName().getString();
        }

        private static String ageText(Tpa.TpaRequest req) {
            long ageSec = req.ageSeconds();
            return ageSec < 60 ? ageSec + "s" : ageSec / 60 + "m";
        }

        @Override
        protected void fillContent(SimpleContainer c) {
            if (mode == TPA_REQUESTS) {
                fillRequests(c);
            } else {
                fillPlayers(c);
            }
        }

        private void fillPlayers(SimpleContainer c) {
            int start = page * contentSize;
            for (int i = 0; i < contentSize && start + i < others.size(); i++) {
                ServerPlayer other = others.get(start + i);
                slotNames[i] = other.getName().getString();
                List<Component> lore = List.of(
                        Tr(player, "gui.teleportgui.tpa.tpagui.hintPlayers")
                                .withStyle(Styles.MUTED));
                c.setItem(i, playerHead(other, null,
                        Component.literal(other.getName().getString()).withStyle(Styles.WARNING),
                        lore, false));
            }
            if (others.isEmpty()) {
                c.setItem(0, stack(Items.PAPER, Tr(player, "gui.teleportgui.tpa.tpagui.noPlayers"), null));
            }
        }

        private ItemStack glintedPlayerHead(MutableComponent name, Tpa.TpaRequest req) {
            ServerPlayer from = Tpa.resolveOnline(req.fromUuid);
            UUID fallbackId;
            try {
                fallbackId = UUID.fromString(req.fromUuid);
            } catch (IllegalArgumentException e) {
                fallbackId = null;
            }
            List<Component> lore = List.of(
                    Tr(player, req.here
                            ? "gui.teleportgui.tpa.tpagui.here"
                            : "gui.teleportgui.tpa.tpagui.tocome")
                            .withStyle(Styles.INFO),
                    Tr(player, "gui.teleportgui.tpa.tpagui.hintRequests")
                            .withStyle(Styles.MUTED),
                    Tr(player, "gui.teleportgui.tpa.tpagui.age", ageText(req))
                            .withStyle(Styles.SUBTLE));
            return playerHead(from, fallbackId, name, lore, !req.here);
        }

        private void fillRequests(SimpleContainer c) {
            int start = page * contentSize;
            for (int i = 0; i < contentSize && start + i < requests.size(); i++) {
                Tpa.TpaRequest req = requests.get(start + i);
                slotNames[i] = req.fromUuid;
                MutableComponent name = Component.literal(senderName(req)).withStyle(Styles.WARNING);
                ItemStack head = glintedPlayerHead(name, req);
                c.setItem(i, head);
            }
            if (requests.isEmpty()) {
                c.setItem(0, stack(Items.PAPER, Tr(player, "gui.teleportgui.tpa.tpagui.empty"), null));
            }
        }

        @Override
        protected void fillExtraNav(SimpleContainer c, int base) {
            boolean allowed = Manager.getPlayer(player.getStringUUID())
                    .map(PlayerData::isRequestAllowed)
                    .orElse(true);
            c.setItem(base + NAV_EXTRA, stack(
                    allowed ? Items.EMERALD_BLOCK : Items.REDSTONE_BLOCK,
                    Tr(player, allowed
                            ? "gui.teleportgui.tpa.tpagui.allowOn"
                            : "gui.teleportgui.tpa.tpagui.allowOff"),
                    List.of(Tr(player, "gui.teleportgui.tpa.tpagui.allowHint")
                            .withStyle(Styles.MUTED))));

            if (mode == TPA_REQUESTS) {
                c.setItem(base + NAV_REQUESTS, stack(Items.COMPASS,
                        Tr(player, "gui.teleportgui.tpa.tpagui.backToPlayers"), null));
            } else if (!requests.isEmpty()) {
                c.setItem(base + NAV_REQUESTS, stack(Items.REDSTONE,
                        Tr(player, "gui.teleportgui.tpa.tpagui.viewRequests"), null));
            }
        }

        @Override
        protected boolean onExtraNav(ServerPlayer sp) {
            boolean allowed = Manager.getPlayer(sp.getStringUUID())
                    .map(PlayerData::isRequestAllowed)
                    .orElse(true);
            PlayerData data = Manager.getPlayer(sp.getStringUUID())
                    .orElseGet(() -> Manager.addPlayer(sp.getStringUUID()));
            data.setRequestAllowed(!allowed);
            openTpa(sp, page, mode);
            return true;
        }

        @Override
        protected boolean onRequestsNav(ServerPlayer sp) {
            if (mode == TPA_REQUESTS) {
                openTpa(sp, 0, TPA_PLAYERS);
                return true;
            }
            if (!requests.isEmpty()) {
                openTpa(sp, 0, TPA_REQUESTS);
                return true;
            }
            return false;
        }

        @Override
        protected void onEntryClick(ServerPlayer sp, String id, int button, ContainerInput input) {
            if (mode == TPA_REQUESTS) {
                if (hopper) {
                    if (button == 0) openTpaRequestActions(sp, id);
                    return;
                }
                ServerPlayer from = Tpa.resolveOnline(id);
                if (from == null) {
                    sp.sendSystemMessage(
                            Tr(sp, "commands.teleportgui.tpa.notFound").withStyle(Styles.ERROR), true);
                    reopen(sp, 0);
                } else if (button == 0) {
                    Tpa.accept(from, sp);
                    sp.closeContainer();
                } else if (button == 1) {
                    Tpa.deny(from, sp);
                    sp.closeContainer();
                }
                return;
            }
            if (hopper) {
                if (button == 0) openTpaPlayerActions(sp, id, page, mode);
                return;
            }
            if (button == 0) {
                Tpa.requestByName(sp, id, false);
                sp.closeContainer();
            } else if (button == 1) {
                Tpa.requestByName(sp, id, true);
                sp.closeContainer();
            }
        }

        @Override
        protected void reopen(ServerPlayer sp, int page) {
            openTpa(sp, page, mode);
        }

        @Override
        protected String guideKey() {
            return "gui.teleportgui.guide.tpa";
        }
    }
}
