package ru.spark108.social;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class TradeMenu extends AbstractContainerMenu {
    private static final int OFFER_SIZE = 9;
    private final UUID tradeId;
    private final String otherName;
    private final TradeManager.Session session;

    public TradeMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(containerId, inventory, data.readUUID(), data.readUtf(16),
                new SimpleContainer(OFFER_SIZE), new SimpleContainer(OFFER_SIZE), null);
    }

    TradeMenu(int containerId, Inventory inventory, TradeManager.Session session, boolean first) {
        this(containerId, inventory, session.id(), first ? session.secondName() : session.firstName(),
                first ? session.firstOffer() : session.secondOffer(),
                first ? session.secondOffer() : session.firstOffer(), session);
    }

    private TradeMenu(int containerId, Inventory inventory, UUID tradeId, String otherName,
                      Container own, Container other, TradeManager.Session session) {
        super(SocialMod.TRADE_MENU.get(), containerId);
        this.tradeId = tradeId;
        this.otherName = otherName;
        this.session = session;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int index = row * 3 + column;
                addSlot(new Slot(own, index, 65 + column * 18, 36 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int index = row * 3 + column;
                addSlot(new ReadOnlySlot(other, index, 250 + column * 18, 36 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column,
                        99 + column * 18, 152 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 99 + column * 18, 212));
        }
    }

    UUID tradeId() { return tradeId; }
    String otherName() { return otherName; }
    TradeManager.Session session() { return session; }

    @Override public boolean stillValid(Player player) {
        return session == null || session.validFor(player);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || index >= OFFER_SIZE && index < OFFER_SIZE * 2)
            return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < OFFER_SIZE) {
            if (!moveItemStackTo(stack, OFFER_SIZE * 2, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, OFFER_SIZE, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    @Override public boolean canDragTo(Slot slot) {
        return !(slot instanceof ReadOnlySlot);
    }

    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return !(slot instanceof ReadOnlySlot) && super.canTakeItemForPickAll(stack, slot);
    }

    @Override public void removed(Player player) {
        super.removed(player);
        if (session != null) session.cancelOnClose();
    }

    private static final class ReadOnlySlot extends Slot {
        ReadOnlySlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player player) { return false; }
        @Override public boolean allowModification(Player player) { return false; }
    }
}
