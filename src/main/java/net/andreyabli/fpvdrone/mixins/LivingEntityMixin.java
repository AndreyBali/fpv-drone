package net.andreyabli.fpvdrone.mixins;

import net.andreyabli.fpvdrone.Freecam;
import net.minecraft.entity.LivingEntity;
import net.andreyabli.fpvdrone.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    public abstract float getHealth();

    // Disables freecam upon receiving damage if disableOnDamage is enabled.
    @Inject(method = "setHealth", at = @At("HEAD"))
    private void onSetHealth(float health, CallbackInfo ci) {
        if (Freecam.isEnabled() && ModConfig.INSTANCE.utility.disableOnDamage && this.equals(Freecam.MC.player)) {
            if (!Freecam.MC.player.isCreative() && getHealth() > health) {
                Freecam.setDisableNextTick(true);
            }
        }
    }
}
