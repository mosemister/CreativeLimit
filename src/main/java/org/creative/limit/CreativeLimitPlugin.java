package org.creative.limit;

import com.google.inject.Inject;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.creative.limit.data.transfer.TransferType;
import org.creative.limit.data.transfer.TransferTypeBuilder;
import org.creative.limit.data.transfer.TransferTypeData;
import org.creative.limit.listeners.InventoryListeners;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.data.DataProvider;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataStore;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.util.Optional;

@Plugin("creative_limit")
public class CreativeLimitPlugin {

    private final PluginContainer container;
    private static CreativeLimitPlugin plugin;

    @Inject
    public CreativeLimitPlugin(PluginContainer container) {
        this.container = container;
        plugin = this;
    }

    @Listener
    public void onEngineStarting(StartingEngineEvent<Server> event) {
        Sponge.eventManager().registerListeners(this.container, new InventoryListeners());
    }

    @Listener
    public void onCommand(RegisterCommandEvent<Command.Parameterized> event) {
        Command.Parameterized viewDataCmd = Command.builder().executor(context -> {
            Subject subject = context.subject();
            if (!(subject instanceof ServerPlayer)) {
                return CommandResult.error(Component.text("player only command"));
            }
            ServerPlayer player = (ServerPlayer) subject;
            Hotbar hotbar = player.inventory().hotbar();
            Optional<ItemStack> opItem = hotbar.peekAt(hotbar.selectedSlotIndex());
            if (opItem.isEmpty()) {
                return CommandResult.error(Component.text("No item in hand"));
            }

            //item data
            Optional<TransferType> opData = opItem.get().get(TransferTypeData.getTransferTypeKey());
            if (opData.isEmpty()) {
                return CommandResult.error(Component.text("No data found"));
            }
            context.sendMessage(Identity.nil(), Component.text("Data found of: " + opData.get().name()));
            return CommandResult.success();
        }).build();

        Command.Parameterized setFromCreativeDataCmd = Command.builder().executor(context -> {
            Subject subject = context.subject();
            if (!(subject instanceof ServerPlayer)) {
                return CommandResult.error(Component.text("player only command"));
            }
            ServerPlayer player = (ServerPlayer) subject;
            Hotbar hotbar = player.inventory().hotbar();
            Optional<ItemStack> opItem = hotbar.peekAt(hotbar.selectedSlotIndex());
            if (opItem.isEmpty()) {
                return CommandResult.error(Component.text("No item in hand"));
            }

            //item data
            opItem.get().offer(TransferTypeData.getTransferTypeKey(), TransferType.FROM_NONE_CREATIVE);
            Optional<TransferType> opData = opItem.get().get(TransferTypeData.getTransferTypeKey());
            if (opData.isEmpty()) {
                return CommandResult.error(Component.text("Data did not apply"));
            }
            context.sendMessage(Identity.nil(), Component.text("Data set of: " + opData.get().name()));

            //shouldnt need this but do
            hotbar.set(hotbar.selectedSlotIndex(), opItem.get());


            return CommandResult.success();
        }).build();

        event.register(this.container, setFromCreativeDataCmd, "setCreativeData");
        event.register(this.container, viewDataCmd, "viewCreativeData");
    }

    @Listener
    public void onRegisterData(RegisterDataEvent event) {
        ResourceKey transferTypeResource = ResourceKey.of(this.container, "transfer_type");
        Key<Value<TransferType>> transferTypeKey = Key.from(transferTypeResource, TransferType.class);
        TransferTypeData.setTransferTypeKey(transferTypeKey);

        DataRegistration registration = DataRegistration.of(transferTypeKey, ItemStack.class, ItemStackSnapshot.class);

        event.register(registration);
        Sponge.dataManager().registerBuilder(TransferType.class, new TransferTypeBuilder());
    }

    public PluginContainer getContainer() {
        return this.container;
    }

    public static CreativeLimitPlugin getPlugin() {
        return plugin;
    }
}
