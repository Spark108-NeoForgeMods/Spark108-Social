package ru.spark108.social;

import net.minecraft.server.level.ServerPlayer;

/** Narrow exception to vanilla's spectator container-click restriction. */
public final class SocialSpectatorTrade {
    private SocialSpectatorTrade() {}

    public static boolean mayClick(ServerPlayer player) {
        return SocialConfig.spectatorInteraction() && player.containerMenu instanceof TradeMenu menu
                && menu.session() != null && menu.session().validFor(player)
                && SocialPermissions.has(player, SocialPermissions.TRADE);
    }
}
