package net.andreyabli.fpvdrone.mixins;

import net.andreyabli.fpvdrone.Freecam;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    // Disables freecam when the player respawns/switches dimensions.
    @Inject(method = "onPlayerRespawn", at = @At("HEAD"))
    private void onPlayerRespawn(CallbackInfo ci) {
        if (Freecam.isEnabled()) {
            Freecam.toggle();
        }
    }
}
