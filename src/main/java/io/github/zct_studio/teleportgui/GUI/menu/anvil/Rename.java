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
 * 「重命名家园 / 传送点」的界面。
 *
 * <p>整体设计与 {@link Create} 相同：借用 {@link AnvilMenu} 的输入框当作文本框，
 * 槽位 0 是占位图标兼输入源，槽位 1 是返回，槽位 2 是确认。
 * 区别在于输入框会预填旧名字，方便玩家在此基础上修改。
 *
 * <p>占位图标的材质带有预览含义：家园是「默认家园红色床品 / 普通白色床」，
 * 传送点是末影珍珠。
 */
@SuppressWarnings("unused")
public final class Rename extends AnvilMenu {
    private final ServerPlayer player;
    private final String oldName;
    private final boolean warp;
    private final int returnPage;
    private String newName = "";
    private boolean navigating;

    private Rename(int windowId, Inventory inv, ServerPlayer player, String oldName,
                   boolean warp, int returnPage) {
        super(windowId, inv);
        this.player = player;
        this.oldName = oldName;
        this.warp = warp;
        this.returnPage = returnPage;
        this.newName = oldName;

        ItemStack input;
        if (warp) {
            input = new ItemStack(Items.ENDER_PEARL);
        } else {
            boolean isDefault = Manager.getPlayer(player.getStringUUID())
                    .map(data -> oldName.equals(data.getDefaultHome()))
                    .orElse(false);
            input = new ItemStack(isDefault ? Items.BED.red() : Items.BED.white());
        }
        input.set(DataComponents.CUSTOM_NAME, Component.literal(oldName));
        this.inputSlots.setItem(0, input);

        ItemStack back = new ItemStack(Items.FEATHER);
        back.set(DataComponents.ITEM_NAME,
                GUI.Tr(player, "gui.teleportgui.rename.back").withStyle(Styles.WARNING));
        this.inputSlots.setItem(1, back);

        refresh();
    }

    public static void open(ServerPlayer player, String oldName, boolean warp, int returnPage) {
        player.openMenu(new Provider(player, oldName, warp, returnPage));
    }

    /**
     * 刷新结果槽里的提示文字。
     * <p>
     * 名字为空时提示「名字不能为空」，否则显示「将重命名为 xxx」并附上确认提示。
     */
    private void refresh() {
        //noinspection ConstantValue
        if (player == null || resultSlots == null) return;
        String shown = newName == null || newName.isBlank() ? null : newName.trim();
        MutableComponent label = shown == null
                ? GUI.Tr(player, "gui.teleportgui.rename.newEmpty")
                : GUI.Tr(player, "gui.teleportgui.rename.new", shown);
        MutableComponent hint = GUI.Tr(player, "gui.teleportgui.rename.confirm");
        resultSlots.setItem(0, GUI.stack(Items.NAME_TAG,
                label.withStyle(Styles.SUCCESS),
                List.of(hint.withStyle(Styles.WARNING))));
    }

    /**
     * 玩家在铁砧输入框里改动文字时由原版调用。
     * <p>
     * 先让父类完成它自己的重命名逻辑，再把新名字记下来并刷新提示。
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
                confirm(sp);
            }
            return;
        }
        super.clicked(slotIndex, button, input, clicker);
    }

    private void confirm(ServerPlayer sp) {
        String target = newName == null ? "" : newName.trim();
        if (target.isEmpty()) {
            sp.sendSystemMessage(
                    GUI.Tr(sp, "gui.teleportgui.rename.newEmpty")
                            .withStyle(Styles.ERROR),
                    true);
            return;
        }
        if (warp) {
            Warp.renameWarp(sp, oldName, target);
        } else {
            Home.renameHome(sp, oldName, target);
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
     * {@code navigating} 为 true 说明玩家是通过「确认」/「返回」主动跳转的，不能再插手；
     * 否则视为玩家按 ESC 关闭，把玩家带回原来的列表页。玩家已离线时直接跳过。
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
        private final String oldName;
        private final boolean warp;
        private final int returnPage;

        Provider(ServerPlayer player, String oldName, boolean warp, int returnPage) {
            this.player = player;
            this.oldName = oldName;
            this.warp = warp;
            this.returnPage = returnPage;
        }

        @Override
        public @NonNull Component getDisplayName() {
            return GUI.Tr(player, "gui.teleportgui.rename.title", oldName);
        }

        @Override
        public AbstractContainerMenu createMenu(int windowId, @NonNull Inventory inv, @NonNull Player p) {
            return new Rename(windowId, inv, player, oldName, warp, returnPage);
        }
    }
}
