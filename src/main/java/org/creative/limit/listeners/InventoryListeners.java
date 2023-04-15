package org.creative.limit.listeners;

import org.creative.limit.CreativeLimitPlugin;
import org.creative.limit.data.transfer.TransferType;
import org.creative.limit.data.transfer.TransferTypeData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.item.inventory.container.ClickContainerEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.*;
import org.spongepowered.api.item.inventory.entity.PlayerInventory;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Ticks;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class InventoryListeners {

    @Listener
    public void onCreativeDropEvent(DropItemEvent.Pre event, @First ServerPlayer player) {
        if (!player.gameMode().get().equals(GameModes.CREATIVE.get())) {
            return;
        }

        List<ItemStackSnapshot> allowedDroppingItems = event
                .droppedItems()
                .stream()
                .filter(item -> item.get(TransferTypeData.getTransferTypeKey()).map(type -> TransferType.FROM_CREATIVE == type).orElse(true))
                .collect(Collectors.toList());

        event.droppedItems().removeAll(allowedDroppingItems);
    }

    @Listener
    public void onPickupItemEvent(ChangeInventoryEvent.Pickup event, @First ServerPlayer player) {
        //mark item if player is creative
        if (!player.gameMode().get().equals(GameModes.CREATIVE.get())) {
            return;
        }

        //single transaction

        System.out.println("Event: ChangeInventoryEvent.Pickup");
    }

    @Listener
    public void onTransferInventoryEvent(ClickContainerEvent event, @First ServerPlayer player) {
        //mark item if item is going into inventory of creative player
        if (!player.gameMode().get().equals(GameModes.CREATIVE.get())) {
            return;
        }
        Container currentContainer = event.container();
        Optional<Slot> opSlot = event.slot();
        if (opSlot.isEmpty()) {
            return;
        }
        Slot slot = opSlot.get();
        if (this.isInTopInventory(currentContainer, slot)) {
            this.handleTopInventory(event);
            return;
        }
        this.handleBottomInventory(event);
    }

    private void handleBottomInventory(ClickContainerEvent event) {
        //Need to check if Shift click was used
        List<SlotTransaction> transactions = event.transactions();
        if (!(event instanceof ClickContainerEvent.Shift)) {
            if (transactions.isEmpty()) {
                throw new RuntimeException("this should be impossible");
            }
            SlotTransaction toTransaction = transactions.get(0);
            this.fromNoneCreativeIntoCreative(event, toTransaction, false);
            return;
        }
        //should only be 2 transactions. clicked slot being 0 and the slot it is going to being 1.
        //there is a chance this will be 1, whereby the item came from outside source .... we handle this in another event so return with no data change
        if (2 != transactions.size()) {
            return;
        }
        SlotTransaction toTransaction = transactions.get(1);
        this.fromCreativeIntoOther(event, toTransaction, true);
    }

    private void handleTopInventory(ClickContainerEvent event) {
        //Need to check if Shift click was used
        if (!(event instanceof ClickContainerEvent.Shift)) {
            this.fromCreativeIntoOther(event, event.cursorTransaction(), true);
            return;
        }
        List<SlotTransaction> transactions = event.transactions();
        if (2 != transactions.size()) {
            //this should be impossible
            throw new RuntimeException("This should be impossible. TransactionSize was " + transactions.size());
        }
        SlotTransaction fromTransaction = transactions.get(1);
        this.fromNoneCreativeIntoCreative(event, fromTransaction, false);
    }

    //this is a stupid fix - api 8+ only as root is an inventory menu with children being generic inventory copies
    private boolean isInTopInventory(Container container, ValueContainer slot) {
        int slotIndex = slot.get(Keys.SLOT_INDEX).orElse(0);
        List<Inventory> viewedInventories = container.viewed();
        if (viewedInventories.isEmpty()) {
            //should be impossible
            throw new RuntimeException("Inventory being viewed doesn't exist");
        }

        Inventory topInventory = viewedInventories.get(0);
        int topInventoryCapacity = topInventory.capacity();
        if (0 == topInventoryCapacity) {
            //Came from none inventory source .... or creative
            return false;
        }
        return topInventoryCapacity > slotIndex;
    }

    private boolean fromNoneCreativeIntoCreative(Cancellable event, Transaction<ItemStackSnapshot> transaction, boolean useOriginal) {
        return this.applyData(transaction, useOriginal, TransferType.FROM_NONE_CREATIVE);
    }

    private void fromCreativeIntoOther(Cancellable event, Transaction<ItemStackSnapshot> transaction, boolean useOriginal) {
        ItemStackSnapshot snapshot = useOriginal ? transaction.original() : transaction.custom().orElse(transaction.finalReplacement());
        if (this.isIgnoreItem(snapshot.type())) {
            return;
        }
        Key<Value<TransferType>> key = TransferTypeData.getTransferTypeKey();
        Optional<TransferType> opData = snapshot.get(key);
        if (opData.isEmpty() || (TransferType.FROM_CREATIVE == opData.get())) {
            System.out.println("Will cancel event");
            //event.setCancelled(true);
            return;
        }
    }

    private boolean isIgnoreItem(ItemType type) {
        //these cannot have data on and can break logic ... so let them pass
        if (type.equals(ItemTypes.AIR.get())) {
            return true;
        }
        return false;
    }

    private boolean applyData(Transaction<ItemStackSnapshot> transaction, boolean original, TransferType type) {
        ItemStackSnapshot snapshot = original ? transaction.original() : transaction.custom().orElse(transaction.finalReplacement());
        Key<Value<TransferType>> key = TransferTypeData.getTransferTypeKey();
        Optional<TransferType> opType = snapshot.get(key);
        if (opType.isPresent()) {
            return false;
        }
        Optional<ItemStackSnapshot> opWith = snapshot.with(key, type);
        opWith.ifPresent(transaction::setCustom);
        return opWith.isPresent();
    }
}
