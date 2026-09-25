package ru.spark108.social;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@Mod(SocialMod.ID)
public final class SocialMod {
    public static final String ID = "spark108_social";
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, ID);
    public static final Supplier<MenuType<TradeMenu>> TRADE_MENU = MENUS.register("trade",
            () -> IMenuTypeExtension.create(TradeMenu::new));

    public SocialMod(IEventBus modBus) {
        SocialConfig.load();
        MENUS.register(modBus);
        modBus.addListener(this::registerPackets);
        NeoForge.EVENT_BUS.addListener(TradeManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(TradeManager::onServerStopping);
        NeoForge.EVENT_BUS.addListener(TradeManager::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.addListener(SocialClient::onUse);
            NeoForge.EVENT_BUS.addListener(SocialClient::onRenderPlayer);
            NeoForge.EVENT_BUS.addListener(SocialClient::onClientLogout);
            modBus.addListener(this::registerScreens);
        }
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(TRADE_MENU.get(), TradeScreen::new);
    }

    private void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)
            PacketDistributor.sendToPlayer(player, new SocialPackets.ClientSettings(
                    SocialConfig.spectatorInteraction(), SocialConfig.alignments()));
    }

    private void registerPackets(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(SocialPackets.HaloRequest.TYPE, SocialPackets.HaloRequest.CODEC,
                (packet, context) -> context.enqueueWork(() -> SocialServer.halo(context.player(), packet)));
        registrar.playToServer(SocialPackets.ActionsRequest.TYPE, SocialPackets.ActionsRequest.CODEC,
                (packet, context) -> context.enqueueWork(() -> SocialServer.actions(context.player(), packet)));
        registrar.playToServer(SocialPackets.BalanceRequest.TYPE, SocialPackets.BalanceRequest.CODEC,
                (packet, context) -> context.enqueueWork(() -> SocialServer.balance(context.player(), packet)));
        registrar.playToServer(SocialPackets.TransferRequest.TYPE, SocialPackets.TransferRequest.CODEC,
                (packet, context) -> context.enqueueWork(() -> SocialServer.transfer(context.player(), packet)));
        registrar.playToServer(SocialPackets.TradeOpenRequest.TYPE, SocialPackets.TradeOpenRequest.CODEC,
                (packet, context) -> context.enqueueWork(() -> TradeManager.open(context.player(), packet.target())));
        registrar.playToServer(SocialPackets.TradeDecision.TYPE, SocialPackets.TradeDecision.CODEC,
                (packet, context) -> context.enqueueWork(() -> TradeManager.confirm(context.player(), packet)));
        if (FMLEnvironment.dist == Dist.CLIENT) {
            registrar.playToClient(SocialPackets.ClientSettings.TYPE, SocialPackets.ClientSettings.CODEC,
                    (packet, context) -> context.enqueueWork(() -> SocialClient.settings(packet)));
            registrar.playToClient(SocialPackets.HaloReply.TYPE, SocialPackets.HaloReply.CODEC,
                    (packet, context) -> context.enqueueWork(() -> SocialClient.halo(packet)));
            registrar.playToClient(SocialPackets.ActionsReply.TYPE, SocialPackets.ActionsReply.CODEC,
                    (packet, context) -> context.enqueueWork(() -> SocialClient.actions(packet)));
            registrar.playToClient(SocialPackets.BalanceReply.TYPE, SocialPackets.BalanceReply.CODEC,
                    (packet, context) -> context.enqueueWork(() -> SocialClient.balance(packet)));
            registrar.playToClient(SocialPackets.TransferReply.TYPE, SocialPackets.TransferReply.CODEC,
                    (packet, context) -> context.enqueueWork(() -> SocialClient.transfer(packet)));
            registrar.playToClient(SocialPackets.TradeState.TYPE, SocialPackets.TradeState.CODEC,
                    (packet, context) -> context.enqueueWork(() -> SocialClient.tradeState(packet)));
        } else {
            registrar.playToClient(SocialPackets.ClientSettings.TYPE, SocialPackets.ClientSettings.CODEC,
                    (packet, context) -> {});
            registrar.playToClient(SocialPackets.HaloReply.TYPE, SocialPackets.HaloReply.CODEC,
                    (packet, context) -> {});
            registrar.playToClient(SocialPackets.ActionsReply.TYPE, SocialPackets.ActionsReply.CODEC,
                    (packet, context) -> {});
            registrar.playToClient(SocialPackets.BalanceReply.TYPE, SocialPackets.BalanceReply.CODEC,
                    (packet, context) -> {});
            registrar.playToClient(SocialPackets.TransferReply.TYPE, SocialPackets.TransferReply.CODEC,
                    (packet, context) -> {});
            registrar.playToClient(SocialPackets.TradeState.TYPE, SocialPackets.TradeState.CODEC,
                    (packet, context) -> {});
        }
    }
}
