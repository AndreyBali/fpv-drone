package net.andreyabli.fpvdrone.entity;

import net.andreyabli.fpvdrone.Freecam;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class DroneModel extends GeoModel<FreeCamera> {
    @Override
    public Identifier getModelResource(FreeCamera animatable) {
        return new Identifier(Freecam.MOD_ID, "geo/voyager.geo.json");
    }

    @Override
    public Identifier getTextureResource(FreeCamera animatable) {
        return new Identifier(Freecam.MOD_ID, "textures/entity/voyager.png");
    }

    @Override
    public Identifier getAnimationResource(FreeCamera animatable) {
        return new Identifier(Freecam.MOD_ID, "animations/voyager.animation.json");
    }
}
