package ru.spark108.social.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.spark108.social.SocialClient;

@Mixin(Minecraft.class)
abstract class HoveredOutlineMinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void spark108$showHoveredOutline(Entity entity, CallbackInfoReturnable<Boolean> result) {
        if (SocialClient.isHovered(entity)) result.setReturnValue(true);
    }
}
