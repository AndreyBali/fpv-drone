package net.andreyabli.fpvdrone.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SelectableDrone {

    public SelectableDrone(List<ModConfig.DroneConfig.Drone> list){
        if (list.isEmpty()) return;
        this.list = list;
    }
    private List<ModConfig.DroneConfig.Drone> list = new ArrayList<>(List.of(new ModConfig.DroneConfig.Drone()));
    private int selectedId = 0;

    public void select(ModConfig.DroneConfig.Drone drone){
        selectedId = list.indexOf(drone);
        if(selectedId == -1) selectedId = 0;
    }
    public void reAdd(List<ModConfig.DroneConfig.Drone> list){
        String selectedObjectName = this.list.get(selectedId).name;
        selectedId = 0;
        this.list.clear();
        if(list.isEmpty()){
            this.list = new ArrayList<>(List.of(new ModConfig.DroneConfig.Drone()));
            return;
        }
        this.list.addAll(list);
        //this.list.sort(Comparator.naturalOrder());
        for (int i = 0; i < list.size(); i++) {
            if(Objects.equals(selectedObjectName, list.get(i).name)) {
                selectedId = i;
                break;
            }
        }
        if(selectedId == -1) selectedId = 0;
    }
    public ModConfig.DroneConfig.Drone getSelectedDrone(){
        return this.list.get(selectedId);
    }

    public Object[] getArray(){
        reAdd(ModConfig.INSTANCE.droneConfig.drones);
        return list.toArray();
    }
}
