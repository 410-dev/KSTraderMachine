package me.hysong.apis.kstrader.v1.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public abstract class StrategySettingsV1 {
    public abstract HashMap<String, Class<?>> getTypes();
    public abstract HashMap<String, HashMap<String, String>> getDescriptions();
    public abstract HashMap<String, HashMap<String, String>> getLabels();
    public abstract HashMap<String, Object> getValues();
    public abstract HashMap<String, Object> getDefaults();
    public abstract ArrayList<String> getOrderedKey();
    public abstract String validateValue(String key);

    public ArrayList<String> getHidden() {
        if (getValues() != null && getValues().get("hidden") != null && getTypes() != null && getTypes().get("hidden").isAssignableFrom(String.class)) {
            String[] array = getValues().get("hidden").toString().split(",");
            ArrayList<String> dat = new ArrayList<>();
            Collections.addAll(dat, array);
            return dat;
        }
        return new ArrayList<>();
    }

}
