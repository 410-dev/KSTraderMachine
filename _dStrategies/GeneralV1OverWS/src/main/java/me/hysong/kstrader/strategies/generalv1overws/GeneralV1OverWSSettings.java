package me.hysong.kstrader.strategies.generalv1overws;

import lombok.Getter;
import me.hysong.apis.kstrader.v1.strategy.StrategySettingsV1;

import java.util.ArrayList;
import java.util.HashMap;

@Getter
public class GeneralV1OverWSSettings extends StrategySettingsV1 {

    private final HashMap<String, Object> values = new HashMap<>();

    public GeneralV1OverWSSettings(HashMap<String, Object> values) {
        this.values.putAll(values);
    }

    @Override
    public HashMap<String, Class<?>> getTypes() {
        HashMap<String, Class<?>> types = new HashMap<>();
        types.put("aggressivenessLimit", Integer.class);
        types.put("aggressiveness", Integer.class);
        types.put("spacing", Integer.class);
        types.put("hidden", Object[].class);
        return types;
    }

    public ArrayList<String> getOrderedKey() {
        ArrayList<String> keys = new ArrayList<>();
        keys.add("aggressivenessLimit");
        keys.add("aggressiveness");
        keys.add("spacing");
        keys.add("hidden");
        return keys;
    }

    @Override
    public String validateValue(String key) {
        switch (key) {
            case "auth.apiAK", "auth.apiSK": {
                if (values.get(key) instanceof String) {
                    return null;
                } else {
                    return "Expected String";
                }
            }
            case "future.leverage": {
                if (values.get(key) instanceof Integer) {
                    if (((Integer) values.get(key)) <= 10 && ((Integer) values.get(key)) > 0) {
                        return null;
                    } else {
                        return "Future Leverage expected value between 0 exclusive to 10 inclusive.";
                    }
                } else {
                    return "Future leverage expected integer.";
                }
            }
            case "option.leverage": {
                if (values.get(key) instanceof Integer) {
                    if (((Integer) values.get(key)) <= 50 && ((Integer) values.get(key)) > 0) {
                        return null;
                    } else {
                        return "Option Leverage expected value between 0 exclusive to 10 inclusive.";
                    }
                } else {
                    return "Option leverage expected integer.";
                }
            }
            default:
                return "Unidentified setting ID: " + key;
        }
    }

    @Override
    public HashMap<String, HashMap<String, String>> getDescriptions() {
        HashMap<String, String> en_us = new HashMap<>();
        en_us.put("aggressiveness", "Future Leverage as integer. Default is 1.");
        en_us.put("spacing", "Option Leverage as integer. Default is 1.");

        HashMap<String, String> ko_kr = new HashMap<>();
        ko_kr.put("aggressiveness", "선물 레버리지. 기본값은 1입니다.");
        ko_kr.put("spacing", "옵션 레버리지. 기본값은 1입니다.");

        HashMap<String, HashMap<String, String>> descriptions = new HashMap<>();
        descriptions.put("en-us", en_us);
        descriptions.put("ko-kr", ko_kr);
        return descriptions;
    }

    @Override
    public HashMap<String, HashMap<String, String>> getLabels() {
        HashMap<String, String> en_us = new HashMap<>();
        en_us.put("aggressiveness", "Future Leverage");
        en_us.put("spacing", "Option Leverage");

        HashMap<String, String> ko_kr = new HashMap<>();
        ko_kr.put("aggressiveness", "선물 레버리지");
        ko_kr.put("spacing", "옵션 레버리지");

        HashMap<String, HashMap<String, String>> labels = new HashMap<>();
        labels.put("en-us", en_us);
        labels.put("ko-kr", ko_kr);
        return labels;
    }

    @Override
    public HashMap<String, Object> getDefaults() {
        HashMap<String, Object> defaults = new HashMap<>();
        defaults.put("aggressivenessLimit", 30);
        defaults.put("aggressiveness", 1);
        defaults.put("spacing", 1);
        defaults.put("hidden", "aggressivenessLimit,");
        return defaults;
    }
}
