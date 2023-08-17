package net.andreyabli.fpvdrone.entity;

import net.andreyabli.fpvdrone.Freecam;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DroneRenderer extends GeoEntityRenderer<FreeCamera> {
    public DroneRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new DroneModel());
    }

    @Override
    public Identifier getTextureLocation(FreeCamera animatable) {
        return new Identifier(Freecam.MOD_ID, "textures/entity/tiger.png");
    }

    @Override
    public void render(FreeCamera entity, float entityYaw, float partialTick, MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
