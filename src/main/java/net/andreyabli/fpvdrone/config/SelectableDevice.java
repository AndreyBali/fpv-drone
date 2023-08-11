package net.andreyabli.fpvdrone.config;

import net.andreyabli.fpvdrone.util.ControllerManager;

import java.util.ArrayList;
import java.util.List;
public class SelectableDevice {
    private final List<String> list = new ArrayList<>(List.of("keyboard"));
    private int selectedId = 0;

    public void select(String s){
        selectedId = list.indexOf(s);
        if(selectedId == -1) selectedId = 0;
    }
    public void reAdd(List<String> list){
        String selectedObject = this.list.get(selectedId);
        this.list.clear();
        this.list.addAll(list);
        //this.list.sort(Comparator.naturalOrder());
        selectedId = list.indexOf(selectedObject);
        if(selectedId == -1) selectedId = 0;
    }
    public String getSelectedString(){
        return this.list.get(selectedId);
    }

    public Object[] getArray(){
        ControllerManager.updateControllers();
        reAdd(ControllerManager.getControllers());
        return list.toArray();
    }
}
