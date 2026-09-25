package ru.spark108.social;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.spark108.economyapi.EconomyResult;
import ru.spark108.economyapi.SparkEconomyApi;

import java.util.UUID;
import java.util.List;

final class SocialServer {
    private static final double MAX_DISTANCE_SQUARED = 36.0;

    private SocialServer() {}

    static void halo(Player sender, SocialPackets.HaloRequest packet) {
        if (!(sender instanceof ServerPlayer viewer)) return;
        ServerPlayer target = recipient(viewer, packet.target());
        List<SocialPackets.HaloLine> lines = target == null ? List.of() : HaloApi.fields(viewer, target).stream()
                .map(field -> new SocialPackets.HaloLine(field.position(), limit(field.name(), 64),
                        limit(field.value(), 256), field.color())).toList();
        PacketDistributor.sendToPlayer(viewer, new SocialPackets.HaloReply(packet.target(), lines));
    }

    private static String limit(String text, int length) {
        return text.length() <= length ? text : text.substring(0, length - 1) + "…";
    }

    static void actions(Player sender, SocialPackets.ActionsRequest packet) {
        if (!(sender instanceof ServerPlayer player)) return;
        if (!SocialConfig.allows(player)) {
            PacketDistributor.sendToPlayer(player, new SocialPackets.ActionsReply(packet.target(), false, false, true));
            return;
        }
        ServerPlayer target = recipient(player, packet.target());
        boolean available = target != null;
        PacketDistributor.sendToPlayer(player, new SocialPackets.ActionsReply(packet.target(),
                available && SocialPermissions.has(player, SocialPermissions.TRANSFER),
                available && SocialPermissions.has(player, SocialPermissions.TRADE)
                        && SocialConfig.allows(target)
                        && SocialPermissions.has(target, SocialPermissions.TRADE), false));
    }

    static void balance(Player sender, SocialPackets.BalanceRequest packet) {
        if (!(sender instanceof ServerPlayer player)) return;
        if (!SocialConfig.allows(player)) {
            balanceFailure(player, packet.target(), "screen.spark108_social.spectator_disabled");
            return;
        }
        if (!SocialPermissions.has(player, SocialPermissions.TRANSFER)) {
            balanceFailure(player, packet.target(), "screen.spark108_social.no_transfer_permission");
            return;
        }
        if (recipient(player, packet.target()) == null) {
            balanceFailure(player, packet.target(), "screen.spark108_social.target_unavailable");
            return;
        }
        String currency = SparkEconomyApi.currency();
        if (!SparkEconomyApi.available() || currency == null) {
            balanceFailure(player, packet.target(), "screen.spark108_social.economy_unavailable");
            return;
        }
        EconomyResult result = SparkEconomyApi.balance(player.getServer(), player.getUUID());
        if (!result.success()) {
            balanceFailure(player, packet.target(), "screen.spark108_social.balance_failed");
            return;
        }
        PacketDistributor.sendToPlayer(player, new SocialPackets.BalanceReply(packet.target(), true,
                result.balance(), currency, ""));
    }

    static void transfer(Player sender, SocialPackets.TransferRequest packet) {
        if (!(sender instanceof ServerPlayer player)) return;
        if (!SocialConfig.allows(player)) {
            reply(player, packet.target(), EconomyResult.fail(EconomyResult.Status.FAILED),
                    "screen.spark108_social.spectator_disabled");
            return;
        }
        if (!SocialPermissions.has(player, SocialPermissions.TRANSFER)) {
            reply(player, packet.target(), EconomyResult.fail(EconomyResult.Status.FAILED),
                    "screen.spark108_social.no_transfer_permission");
            return;
        }
        if (recipient(player, packet.target()) == null) {
            reply(player, packet.target(), EconomyResult.fail(EconomyResult.Status.FAILED),
                    "screen.spark108_social.target_unavailable");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(packet.amount().replace(',', '.'));
        } catch (NumberFormatException exception) {
            amount = Double.NaN;
        }
        if (!Double.isFinite(amount) || amount <= 0 || !SparkEconomyApi.validAmount(amount)) {
            reply(player, packet.target(), EconomyResult.fail(EconomyResult.Status.INVALID_AMOUNT),
                    "screen.spark108_social.invalid_amount");
            return;
        }
        if (!SparkEconomyApi.available() || SparkEconomyApi.currency() == null) {
            reply(player, packet.target(), EconomyResult.fail(EconomyResult.Status.UNAVAILABLE),
                    "screen.spark108_social.economy_unavailable");
            return;
        }
        EconomyResult result = SparkEconomyApi.transfer(player.getServer(), player.getUUID(), packet.target(), amount);
        reply(player, packet.target(), result, switch (result.status()) {
            case OK -> "screen.spark108_social.success";
            case INSUFFICIENT_FUNDS -> "screen.spark108_social.insufficient_funds";
            case INVALID_AMOUNT -> "screen.spark108_social.invalid_amount";
            case UNAVAILABLE, CURRENCY_NOT_FOUND -> "screen.spark108_social.economy_unavailable";
            default -> "screen.spark108_social.transfer_failed";
        });
    }

    private static ServerPlayer recipient(ServerPlayer sender, UUID target) {
        if (sender.getUUID().equals(target)) return null;
        ServerPlayer other = sender.getServer().getPlayerList().getPlayer(target);
        if (other == null || !other.isAlive() || other.level() != sender.level()
                || sender.distanceToSqr(other) > MAX_DISTANCE_SQUARED) return null;
        return other;
    }

    private static void balanceFailure(ServerPlayer player, UUID target, String message) {
        PacketDistributor.sendToPlayer(player, new SocialPackets.BalanceReply(target, false, 0, "", message));
    }

    private static void reply(ServerPlayer player, UUID target, EconomyResult result, String message) {
        PacketDistributor.sendToPlayer(player, new SocialPackets.TransferReply(target, result.success(),
                result.success() ? result.balance() : 0, message));
    }
}
