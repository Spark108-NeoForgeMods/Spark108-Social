package ru.spark108.social.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.spark108.social.SocialClient;

@Mixin(Entity.class)
abstract class HoveredOutlineEntityMixin {
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void spark108$greenHoveredOutline(CallbackInfoReturnable<Integer> result) {
        if (SocialClient.isHovered((Entity) (Object) this)) result.setReturnValue(0x55FF55);
    }
}
