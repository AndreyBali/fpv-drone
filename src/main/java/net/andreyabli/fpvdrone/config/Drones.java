package net.andreyabli.fpvdrone.config;

public enum Drones {
    PIXEL("drone.fpv-drone.pixel", 0.225, 0.125, 0.01, 0.05, 0.4, 1, 5),
    VOXEL_RACER_ONE("drone.fpv-drone.voxel_racer_one", 0.55, 0.3, 0.5, 0.135, 65, 1, 45),
    VOYAGER("drone.fpv-drone.voyager", 0.9, 0.2, 1.2, 0.2, 55, 0.8, 20);

    private final String translation;
    public final float width;
    public final float height;
    public final float mass;
    public final float dragCoefficient;
    public final float thrust;
    public final float thrustCurve;
    public final int cameraAngle;

    Drones(String translation, double width, double height, double mass, double dragCoefficient, double thrust, double thrustCurve, int cameraAngle) {
        this.translation = translation;
        this.width = (float) width;
        this.height = (float) height;
        this.mass = (float) mass;
        this.dragCoefficient = (float) dragCoefficient;
        this.thrust = (float) thrust;
        this.thrustCurve = (float) thrustCurve;
        this.cameraAngle = cameraAngle;
    }

//    @Override
//    public String toString(){
//        return Text.translatable(this.translation).getString();
//    } //TODO uncomment after translation
}
