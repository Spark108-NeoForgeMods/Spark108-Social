package ru.spark108.social.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.spark108.social.SocialSpectatorTrade;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class SpectatorTradeClickMixin {
    @Shadow public ServerPlayer player;

    @Redirect(method = "handleContainerClick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSpectator()Z"))
    private boolean spark108$allowSpectatorTradeClicks(ServerPlayer checkedPlayer) {
        return checkedPlayer.isSpectator() && !SocialSpectatorTrade.mayClick(player);
    }
}
