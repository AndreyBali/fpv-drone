package net.andreyabli.fpvdrone.mixins;

import net.andreyabli.fpvdrone.Main;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    // Needed for Baritone compatibility.
    @Inject(method = "isCamera", at = @At("HEAD"), cancellable = true)
    private void onIsCamera(CallbackInfoReturnable<Boolean> cir) {
        if (Main.isEnabled() && this.equals(Main.MC.player)) {
            cir.setReturnValue(true);
        }
    }
}
