package io.github.zct_studio.teleportgui.gui.menu.anvil;

import io.github.zct_studio.teleportgui.ServerMain;
import io.github.zct_studio.teleportgui.Styles;
import io.github.zct_studio.teleportgui.commands.Home;
import io.github.zct_studio.teleportgui.commands.Warp;
import io.github.zct_studio.teleportgui.gui.GUI;
import io.github.zct_studio.teleportgui.storage.Manager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 「新建家园 / 新建传送点」的命名界面。
 *
 * <h2>为什么要复用铁砧菜单</h2>
 * 原版 Java 版玩家没有任何「自由输入文本」的 UI。铁砧的 {@link AnvilMenu} 是唯一一个
 * 能接收玩家输入字符串的原生容器，所以这里借它的输入框当文本框用。
 *
 * <h2>槽位含义</h2>
 * <ul>
 *   <li>槽位 0：铁砧的输入槽。放一个床或末影珍珠作为占位图标，
 *       玩家在铁砧界面里改名时，{@link #setItemName(String)} 会被调用，从而拿到输入内容。</li>
 *   <li>槽位 1：放一个羽毛，点击 = 放弃并返回列表。</li>
 *   <li>槽位 2：铁砧的结果槽。原本是「取出成品」，这里被改造成「确认」按钮。</li>
 * </ul>
 *
 * <h2>为什么 {@link #mayPickup} 恒为 false</h2>
 * 结果槽里的物品只是用来显示「确认」二字，玩家绝对不能把它拿进背包，
 * 否则会凭空造出一个命名牌。
 *
 * <h2>{@code navigating} 的作用</h2>
 * {@link #removed(Player)} 会在容器关闭时被调用。如果关闭是因为玩家点了「确认」或「返回」
 * 而主动跳转，我们不能再自作聪明地「重新打开列表」；只有玩家真的按 ESC 关掉界面时才需要
 * 回到原列表。{@code navigating} 就是用来区分这两种情况的。
 */
@SuppressWarnings("unused")
public final class Create extends AnvilMenu {
    private final ServerPlayer player;
    private final boolean warp;
    private final int returnPage;
    private String newName = "";
    private boolean navigating;

    private Create(int windowId, Inventory inv, ServerPlayer player,
                   boolean warp, int returnPage) {
        super(windowId, inv);
        this.player = player;
        this.warp = warp;
        this.returnPage = returnPage;

        ItemStack input;
        if (warp) {
            input = new ItemStack(Items.ENDER_PEARL);
        } else {
            boolean hasHome = Manager.getPlayer(player.getStringUUID())
                    .map(data -> !data.getHomes().isEmpty())
                    .orElse(false);
            input = new ItemStack(hasHome ? Items.BED.white() : Items.BED.red());
        }
        input.set(DataComponents.ITEM_NAME, Component.empty());
        this.inputSlots.setItem(0, input);

        ItemStack back = new ItemStack(Items.FEATHER);
        back.set(DataComponents.ITEM_NAME,
                GUI.Tr(player, "gui.teleportgui.rename.back").withStyle(Styles.WARNING));
        this.inputSlots.setItem(1, back);

        refresh();
    }

    public static void open(ServerPlayer player, boolean warp, int returnPage) {
        player.openMenu(new Provider(player, warp, returnPage));
    }

    /**
     * 刷新结果槽里的提示文字。
     * <p>
     * 名字为空时提示「请输入名字」，否则显示「将创建 xxx」。
     * 注意结果槽的图标本身不能有任何交互效果，只作为确认按钮的载体。
     */
    private void refresh() {
        //noinspection ConstantValue
        if (player == null || resultSlots == null) return;
        String shown = newName == null || newName.isBlank() ? null : newName.trim();
        MutableComponent label = shown == null
                ? GUI.Tr(player, "gui.teleportgui.rename.newEmpty")
                : GUI.Tr(player, "gui.teleportgui.rename.create", shown);
        resultSlots.setItem(0, GUI.stack(Items.NAME_TAG,
                label.withStyle(Styles.SUCCESS),
                List.of(GUI.Tr(player, "gui.teleportgui.rename.createHint")
                        .withStyle(Styles.WARNING))));
    }

    /**
     * 玩家在铁砧输入框里改动文字时由原版调用。
     * <p>
     * 先让父类完成它自己的重命名逻辑（保持铁砧界面行为正常），再把新名字记下来并刷新提示。
     * {@code name} 可能为 {@code null}（输入被清空），这里统一收敛成空串处理。
     *
     * @param name 重命名输入框里的新内容
     * @return 父类是否接受了这次改名
     */
    @Override
    public boolean setItemName(@NonNull String name) {
        boolean ok = super.setItemName(name);
        //noinspection ConstantValue
        this.newName = name == null ? "" : name;
        refresh();
        return ok;
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Override
    protected boolean mayPickup(@NonNull Player player, boolean hasResult) {
        return false;
    }

    @Override
    public void clicked(int slotIndex, int button, @NonNull ContainerInput input, @NonNull Player clicker) {
        if (input == ContainerInput.SWAP) {
            if (clicker instanceof ServerPlayer sp) {
                sp.inventoryMenu.sendAllDataToRemote();
            }
            return;
        }
        if (clicker instanceof ServerPlayer sp && slotIndex >= 0 && slotIndex <= 2) {
            if (slotIndex == 1) {
                openList(sp);
            } else if (slotIndex == 2) {
                confirmCreate(sp);
            }
            return;
        }
        super.clicked(slotIndex, button, input, clicker);
    }

    private void confirmCreate(ServerPlayer sp) {
        String target = newName == null ? "" : newName.trim();
        if (target.isEmpty()) {
            sp.sendSystemMessage(
                    GUI.Tr(sp, "gui.teleportgui.rename.newEmpty").withStyle(Styles.ERROR),
                    true);
            return;
        }
        if (warp) {
            Warp.setWarp(sp, target);
        } else {
            Home.setHome(sp, target);
        }
        openList(sp);
    }

    private void openList(ServerPlayer sp) {
        navigating = true;
        if (warp) {
            GUI.openWarp(sp, returnPage);
        } else {
            GUI.openHome(sp, returnPage);
        }
    }

    /**
     * 容器被关闭时触发。
     * <p>
     * 需要区分两种关闭：
     * <ul>
     *   <li>玩家点了「确认」/「返回」触发的主动跳转——此时 {@code navigating} 为 true，
     *       直接返回，交给跳转目标自己管理界面。</li>
     *   <li>玩家按 ESC 或断线导致界面关闭——此时应当把玩家带回原来的列表页，
     *       否则玩家会突然卡在空界面上。</li>
     * </ul>
     * 玩家已经离线时不要再打开菜单，因此先查在线状态。
     */
    @Override
    public void removed(@NonNull Player p) {
        super.removed(p);
        if (navigating || player == null) return;
        var server = ServerMain.SERVER;
        if (server == null || server.getPlayerList().getPlayer(p.getUUID()) == null) return;
        openList(player);
    }

    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    public static class Provider implements MenuProvider {
        private final ServerPlayer player;
        private final boolean warp;
        private final int returnPage;

        Provider(ServerPlayer player, boolean warp, int returnPage) {
            this.player = player;
            this.warp = warp;
            this.returnPage = returnPage;
        }

        @Override
        public @NonNull Component getDisplayName() {
            return GUI.Tr(player, "gui.teleportgui.rename.createTitle");
        }

        @Override
        public AbstractContainerMenu createMenu(int windowId, @NonNull Inventory inv, @NonNull Player p) {
            return new Create(windowId, inv, player, warp, returnPage);
        }
    }
}
