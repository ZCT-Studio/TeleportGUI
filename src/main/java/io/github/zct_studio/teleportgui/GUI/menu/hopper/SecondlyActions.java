package io.github.zct_studio.teleportgui.gui.menu.hopper;

import io.github.zct_studio.teleportgui.ServerMain;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public final class SecondlyActions extends AbstractContainerMenu {

    public record Action(ItemStack icon, Runnable runnable) {}

    private static final int SIZE = 5;

    private final Action[] actions;
    private final Runnable onClose;
    private final Container container;
    private boolean navigating;

    private SecondlyActions(int windowId, Inventory inv, ServerPlayer player,
                            Action[] actions, Runnable onClose) {
        super(MenuType.HOPPER, windowId);
        this.actions = actions;
        this.onClose = onClose;
        this.container = new SimpleContainer(SIZE);
        for (int i = 0; i < SIZE; i++) {
            if (actions[i] != null && actions[i].icon() != null) {
                container.setItem(i, actions[i].icon());
            }
        }
        for (int i = 0; i < SIZE; i++) {
            addSlot(new Slot(container, i, 44 + i * 18, 20));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 51 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 109));
        }
    }

    @Override
    public void clicked(int slotIndex, int button, @NonNull ContainerInput input, @NonNull Player clicker) {
        if (!(clicker instanceof ServerPlayer sp)) {
            super.clicked(slotIndex, button, input, clicker);
            return;
        }
        if (input == ContainerInput.SWAP) {
            sp.inventoryMenu.sendAllDataToRemote();
            return;
        }
        if (slotIndex >= 0 && slotIndex < SIZE) {
            Action action = actions[slotIndex];
            if (action != null && action.runnable() != null) {
                navigating = true;
                action.runnable().run();
            }
        }
    }

    @Override
    public void removed(@NonNull Player p) {
        super.removed(p);
        if (navigating || onClose == null || isOffline(p)) return;
        onClose.run();
    }

    private static boolean isOffline(Player p) {
        var server = ServerMain.SERVER;
        return server == null || server.getPlayerList().getPlayer(p.getUUID()) == null;
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return true;
    }

    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    public static void open(ServerPlayer player, Component title, Action[] actions, Runnable onClose) {
        player.openMenu(new Provider(player, title, actions, onClose));
    }

    private record Provider(ServerPlayer player, Component title, Action[] actions, Runnable onClose)
            implements MenuProvider {

        @Override
        public @NonNull Component getDisplayName() {
            return title;
        }

        @Override
        public AbstractContainerMenu createMenu(int windowId, @NonNull Inventory inv, @NonNull Player p) {
            return new SecondlyActions(windowId, inv, player, actions, onClose);
        }
    }
}
