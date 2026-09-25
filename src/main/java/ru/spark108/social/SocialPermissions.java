package ru.spark108.social;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.server.level.ServerPlayer;

final class SocialPermissions {
    static final String TRADE = "ru.spark108.social.trade";
    static final String TRANSFER = "ru.spark108.social.transfer";

    private SocialPermissions() {}

    static boolean has(ServerPlayer player, String node) {
        try {
            User user = LuckPermsProvider.get().getUserManager().getUser(player.getUUID());
            return user != null && user.getCachedData().getPermissionData().checkPermission(node).asBoolean();
        } catch (IllegalStateException exception) {
            return false;
        }
    }
}
