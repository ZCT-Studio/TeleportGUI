package io.github.zct_studio.teleportgui.gui.menu.anvil;

import io.github.zct_studio.teleportgui.ServerMain;
import io.github.zct_studio.teleportgui.commands.Home;
import io.github.zct_studio.teleportgui.commands.Warp;
import io.github.zct_studio.teleportgui.gui.GUI;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.item.component.ItemLore;
import org.jspecify.annotations.NonNull;

import java.util.List;

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

        ItemStack back = new ItemStack(Items.FEATHER);
        back.set(DataComponents.ITEM_NAME, Component.empty());
        back.set(DataComponents.LORE, new ItemLore(List.of(
                GUI.Tr(player, "gui.teleportgui.rename.back").withStyle(ChatFormatting.YELLOW))));
        this.inputSlots.setItem(0, back);

        ItemStack old = new ItemStack(Items.BED.red());
        old.set(DataComponents.ITEM_NAME,
                GUI.Tr(player, "gui.teleportgui.rename.old", oldName).withStyle(ChatFormatting.AQUA));
        this.inputSlots.setItem(1, old);

        refresh();
    }

    private void refresh() {
        if (player == null || resultSlots == null) return;
        String shown = newName == null || newName.isBlank() ? null : newName.trim();
        MutableComponent label = shown == null
                ? GUI.Tr(player, "gui.teleportgui.rename.newEmpty")
                : GUI.Tr(player, "gui.teleportgui.rename.new", shown);
        MutableComponent hint = GUI.Tr(player, "gui.teleportgui.rename.confirm");
        resultSlots.setItem(0, GUI.stack(Items.NAME_TAG,
                label.withStyle(ChatFormatting.GREEN),
                List.of(hint.withStyle(ChatFormatting.YELLOW))));
    }

    @Override
    public boolean setItemName(@NonNull String name) {
        boolean ok = super.setItemName(name);
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
            if (slotIndex == 0) {
                openList(sp);
            } else if (slotIndex == 2) {
                confirm(sp);
            }
            // slotIndex == 1：只展示原名，点了啥也不干
            return;
        }
        super.clicked(slotIndex, button, input, clicker);
    }

    private void confirm(ServerPlayer sp) {
        String target = newName == null ? "" : newName.trim();
        if (target.isEmpty()) {
            sp.sendSystemMessage(
                    GUI.Tr(sp, "gui.teleportgui.rename.newEmpty")
                            .withStyle(ChatFormatting.RED),
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

    public static void open(ServerPlayer player, String oldName, boolean warp, int returnPage) {
        player.openMenu(new Provider(player, oldName, warp, returnPage));
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
