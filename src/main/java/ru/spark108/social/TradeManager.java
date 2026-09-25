package ru.spark108.social;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class TradeManager {
    private static final Map<UUID, Session> ACTIVE = new HashMap<>();
    private static final double MAX_DISTANCE_SQUARED = 36.0;

    private TradeManager() {}

    static void open(Player requester, UUID targetId) {
        if (!(requester instanceof ServerPlayer first)) return;
        if (!SocialConfig.allows(first)) {
            first.sendSystemMessage(Component.translatable("screen.spark108_social.spectator_disabled"));
            return;
        }
        if (!SocialPermissions.has(first, SocialPermissions.TRADE)) {
            first.sendSystemMessage(Component.translatable("screen.spark108_social.no_trade_permission"));
            return;
        }
        ServerPlayer second = first.getServer().getPlayerList().getPlayer(targetId);
        if (second == null || second == first || !first.isAlive() || !second.isAlive()
                || !SocialConfig.allows(second) || first.level() != second.level()
                || first.distanceToSqr(second) > MAX_DISTANCE_SQUARED) {
            first.sendSystemMessage(Component.translatable("screen.spark108_social.target_unavailable"));
            return;
        }
        if (!SocialPermissions.has(second, SocialPermissions.TRADE)) {
            first.sendSystemMessage(Component.translatable("screen.spark108_social.trade_target_no_permission"));
            return;
        }
        if (ACTIVE.containsKey(first.getUUID()) || ACTIVE.containsKey(second.getUUID())) {
            first.sendSystemMessage(Component.translatable("screen.spark108_social.trade_busy"));
            return;
        }
        Session session = new Session(first, second);
        ACTIVE.put(first.getUUID(), session);
        ACTIVE.put(second.getUUID(), session);
        boolean openedFirst = openFor(session, first, true);
        boolean openedSecond = openedFirst && openFor(session, second, false);
        if (!openedSecond) {
            session.cancel();
            return;
        }
        session.sendState();
    }

    private static boolean openFor(Session session, ServerPlayer player, boolean first) {
        String otherName = first ? session.secondName() : session.firstName();
        var provider = new SimpleMenuProvider((containerId, inventory, ignored) ->
                new TradeMenu(containerId, inventory, session, first),
                Component.translatable("screen.spark108_social.trade_title"));
        return player.openMenu(provider, buffer -> {
            buffer.writeUUID(session.id());
            buffer.writeUtf(otherName, 16);
        }).isPresent();
    }

    static void confirm(Player requester, SocialPackets.TradeDecision packet) {
        if (!(requester instanceof ServerPlayer player)) return;
        Session session = ACTIVE.get(player.getUUID());
        if (session == null || !session.id().equals(packet.tradeId())
                || !(player.containerMenu instanceof TradeMenu menu) || menu.session() != session) return;
        if (!SocialPermissions.has(player, SocialPermissions.TRADE)) {
            session.cancel();
            return;
        }
        session.confirm(player, packet.confirmed());
    }

    static void onServerTick(ServerTickEvent.Post event) {
        for (Session session : new HashSet<>(ACTIVE.values())) session.tick();
    }

    static void onServerStopping(ServerStoppingEvent event) {
        for (Session session : new HashSet<>(ACTIVE.values())) session.cancel();
    }

    static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Session session = ACTIVE.get(event.getEntity().getUUID());
        if (session != null) session.cancel();
    }

    static final class Session {
        private final UUID id = UUID.randomUUID();
        private final ServerPlayer first;
        private final ServerPlayer second;
        private final SimpleContainer firstOffer = new SimpleContainer(9);
        private final SimpleContainer secondOffer = new SimpleContainer(9);
        private boolean firstConfirmed;
        private boolean secondConfirmed;
        private int countdownTicks;
        private boolean closed;

        Session(ServerPlayer first, ServerPlayer second) {
            this.first = first;
            this.second = second;
            firstOffer.addListener(ignored -> changed());
            secondOffer.addListener(ignored -> changed());
        }

        UUID id() { return id; }
        String firstName() { return first.getGameProfile().getName(); }
        String secondName() { return second.getGameProfile().getName(); }
        SimpleContainer firstOffer() { return firstOffer; }
        SimpleContainer secondOffer() { return secondOffer; }

        boolean validFor(Player player) {
            return !closed && (player == first || player == second);
        }

        void confirm(ServerPlayer player, boolean value) {
            if (closed) return;
            if (player == first) {
                if (firstConfirmed == value) return;
                firstConfirmed = value;
            } else if (player == second) {
                if (secondConfirmed == value) return;
                secondConfirmed = value;
            } else return;
            countdownTicks = firstConfirmed && secondConfirmed ? 100 : 0;
            sendState();
        }

        private void changed() {
            if (closed) return;
            firstConfirmed = false;
            secondConfirmed = false;
            countdownTicks = 0;
            if (first.containerMenu instanceof TradeMenu menu && menu.session() == this) menu.broadcastChanges();
            if (second.containerMenu instanceof TradeMenu menu && menu.session() == this) menu.broadcastChanges();
            sendState();
        }

        void tick() {
            if (closed) return;
            if (!first.isAlive() || !second.isAlive() || first.hasDisconnected() || second.hasDisconnected()
                    || !SocialConfig.allows(first) || !SocialConfig.allows(second)
                    || !SocialPermissions.has(first, SocialPermissions.TRADE)
                    || !SocialPermissions.has(second, SocialPermissions.TRADE)
                    || first.level() != second.level() || first.distanceToSqr(second) > MAX_DISTANCE_SQUARED
                    || !(first.containerMenu instanceof TradeMenu firstMenu) || firstMenu.session() != this
                    || !(second.containerMenu instanceof TradeMenu secondMenu) || secondMenu.session() != this) {
                cancel();
                return;
            }
            if (countdownTicks > 0) {
                countdownTicks--;
                if (countdownTicks == 0) complete();
                else if (countdownTicks % 20 == 0) sendState();
            }
        }

        void cancelOnClose() { cancel(); }

        void cancel() {
            if (closed) return;
            closed = true;
            unregister();
            for (ItemStack stack : takeAll(firstOffer)) first.getInventory().placeItemBackInInventory(stack);
            for (ItemStack stack : takeAll(secondOffer)) second.getInventory().placeItemBackInInventory(stack);
            first.sendSystemMessage(Component.translatable("screen.spark108_social.trade_cancelled"));
            second.sendSystemMessage(Component.translatable("screen.spark108_social.trade_cancelled"));
            closeMenus();
        }

        private void complete() {
            if (closed) return;
            closed = true;
            unregister();
            for (ItemStack stack : takeAll(firstOffer)) second.getInventory().placeItemBackInInventory(stack);
            for (ItemStack stack : takeAll(secondOffer)) first.getInventory().placeItemBackInInventory(stack);
            first.sendSystemMessage(Component.translatable("screen.spark108_social.trade_done"));
            second.sendSystemMessage(Component.translatable("screen.spark108_social.trade_done"));
            closeMenus();
        }

        private void unregister() {
            ACTIVE.remove(first.getUUID(), this);
            ACTIVE.remove(second.getUUID(), this);
        }

        private void closeMenus() {
            first.getServer().execute(() -> {
                if (first.containerMenu instanceof TradeMenu menu && menu.session() == this) first.closeContainer();
                if (second.containerMenu instanceof TradeMenu menu && menu.session() == this) second.closeContainer();
            });
        }

        private static List<ItemStack> takeAll(SimpleContainer offer) {
            List<ItemStack> items = new ArrayList<>();
            for (int index = 0; index < offer.getContainerSize(); index++) {
                ItemStack stack = offer.removeItemNoUpdate(index);
                if (!stack.isEmpty()) items.add(stack);
            }
            return items;
        }

        void sendState() {
            if (closed) return;
            int seconds = (countdownTicks + 19) / 20;
            PacketDistributor.sendToPlayer(first, new SocialPackets.TradeState(
                    id, firstConfirmed, secondConfirmed, seconds));
            PacketDistributor.sendToPlayer(second, new SocialPackets.TradeState(
                    id, secondConfirmed, firstConfirmed, seconds));
        }
    }
}
