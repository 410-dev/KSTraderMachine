package me.hysong.apis.kstrader.v1.driver;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Setter;

import java.io.*;
import java.util.HashMap;

public abstract class TraderDriverSettings {

    @Setter private String driverCfgPath;

    public abstract HashMap<String, Class<?>> getTypes();
    public abstract HashMap<String, HashMap<String, String>> getDescriptions();
    public abstract HashMap<String, HashMap<String, String>> getLabels();
    public abstract HashMap<String, Object> getValues();
    public abstract HashMap<String, Object> getDefaults();

    public abstract String getExchange();
    public abstract String getEndpoint();

    public TraderDriverSettings(String driverCfgPath) {
        // Read the JSON file
        this.driverCfgPath = driverCfgPath;
        StringBuilder jsonContent = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(driverCfgPath));
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
            reader.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.err.println("Failed to read the JSON file: " + driverCfgPath + ". Using default settings.");
            getValues().putAll(getDefaults());
            return;
        }

        JsonObject jsonObject = JsonParser.parseString(jsonContent.toString()).getAsJsonObject();
        if (!jsonObject.has("version")) {
            throw new IllegalArgumentException("Invalid JSON content: missing 'version' field as integer");
        }
        if (!jsonObject.has("linkage")) {
            throw new IllegalArgumentException("Invalid JSON content: missing 'linkage' field as a dictionary");
        } else if (!jsonObject.get("linkage").isJsonObject()) {
            throw new IllegalArgumentException("Invalid JSON content: 'linkage' field is not a dictionary");
        } else if (!jsonObject.get("linkage").getAsJsonObject().has("exchange")) {
            throw new IllegalArgumentException("Invalid JSON content: 'linkage' field is missing 'exchange' field");
        } else if (!jsonObject.get("linkage").getAsJsonObject().has("endpoint")) {
            throw new IllegalArgumentException("Invalid JSON content: 'linkage' field is missing 'endpoint' field");
        } else if (!jsonObject.get("linkage").getAsJsonObject().get("exchange").isJsonPrimitive()) {
            throw new IllegalArgumentException("Invalid JSON content: 'linkage' field's 'exchange' field is not a string");
        } else if (!jsonObject.get("linkage").getAsJsonObject().get("endpoint").isJsonPrimitive()) {
            throw new IllegalArgumentException("Invalid JSON content: 'linkage' field's 'endpoint' field is not a string");
        }
        if (!jsonObject.has("settings")) {
            throw new IllegalArgumentException("Invalid JSON content: missing 'settings' field as a dictionary");
        } else if (!jsonObject.get("settings").isJsonObject()) {
            throw new IllegalArgumentException("Invalid JSON content: 'settings' field is not a dictionary");
        }

        // Check linkage
        JsonObject linkage = jsonObject.getAsJsonObject("linkage");
        if (linkage.get("exchange").getAsString().isEmpty()) {
            throw new IllegalArgumentException("Invalid JSON content: 'linkage' field's 'exchange' field is empty");
        }
        if (linkage.get("endpoint").getAsString().isEmpty()) {
            throw new IllegalArgumentException("Invalid JSON content: 'linkage' field's 'endpoint' field is empty");
        }
        String exchange = linkage.get("exchange").getAsString();
        String endpoint = linkage.get("endpoint").getAsString();
        if (!exchange.equals(getExchange())) {
            throw new IllegalArgumentException("Invalid JSON content: 'linkage' field's 'exchange' field is not equal to the driver exchange! Expected: " + getExchange() + ", but got: " + exchange);
        }
        if (!endpoint.equals(getEndpoint())) {
            throw new IllegalArgumentException("Invalid JSON content: 'linkage' field's 'endpoint' field is not equal to the driver endpoint! Expected: " + getEndpoint() + ", but got: " + endpoint);
        }


        // Parse the settings
        JsonObject settings = jsonObject.getAsJsonObject("settings");
        HashMap<String, Class<?>> types = getTypes();
        HashMap<String, Object> defaults = getDefaults();
        for (String key : settings.keySet()) {
            // Check type is correct
            if (!types.containsKey(key)) {
                throw new IllegalArgumentException("Invalid JSON content: 'settings' field contains unknown key '" + key + "'");
            }
            if (!settings.get(key).isJsonPrimitive()) {
                throw new IllegalArgumentException("Invalid JSON content: 'settings' field's '" + key + "' field is not a primitive");
            }
            if (settings.get(key).getAsJsonPrimitive().isString()) {
                if (!types.get(key).equals(String.class)) {
                    throw new IllegalArgumentException("Invalid JSON content: 'settings' field's '" + key + "' field is not a string");
                }
            } else if (settings.get(key).getAsJsonPrimitive().isNumber()) {
                if (!types.get(key).equals(Integer.class)
                        && !types.get(key).equals(Double.class)
                        && !types.get(key).equals(Long.class)
                        && !types.get(key).equals(Float.class)
                        && !types.get(key).equals(Short.class)
                        && !types.get(key).equals(Byte.class)
                        && !types.get(key).equals(Number.class)) {
                    throw new IllegalArgumentException("Invalid JSON content: 'settings' field's '" + key + "' field is not a number");
                }
            } else if (settings.get(key).getAsJsonPrimitive().isBoolean()) {
                if (!types.get(key).equals(Boolean.class)) {
                    throw new IllegalArgumentException("Invalid JSON content: 'settings' field's '" + key + "' field is not a boolean");
                }
            }
            // put value
            if (settings.get(key).getAsJsonPrimitive().isString()) {
                getValues().put(key, settings.get(key).getAsString());
            } else if (settings.get(key).getAsJsonPrimitive().isNumber()) {
                getValues().put(key, settings.get(key).getAsNumber());
            } else if (settings.get(key).getAsJsonPrimitive().isBoolean()) {
                getValues().put(key, settings.get(key).getAsBoolean());
            }
        }
        // Those not found in the settings will be set to default
        for (String key : types.keySet()) {
            if (!getValues().containsKey(key)) {
                if (defaults.containsKey(key)) {
                    getValues().put(key, defaults.get(key));
                } else {
                    throw new IllegalArgumentException("Invalid JSON content: 'settings' field is missing '" + key + "' field");
                }
            }
        }
    }

    public void save() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("version", 1);
        JsonObject linkage = new JsonObject();
        linkage.addProperty("exchange", getExchange());
        linkage.addProperty("endpoint", getEndpoint());
        jsonObject.add("linkage", linkage);
        JsonObject settings = new JsonObject();
        for (String key : getValues().keySet()) {
            if (getValues().get(key) instanceof String) {
                settings.addProperty(key, (String) getValues().get(key));
            } else if (getValues().get(key) instanceof Number) {
                settings.addProperty(key, (Number) getValues().get(key));
            } else if (getValues().get(key) instanceof Boolean) {
                settings.addProperty(key, (Boolean) getValues().get(key));
            }
        }
        jsonObject.add("settings", settings);
        // Write the JSON object to a file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(driverCfgPath))) {
            writer.write(jsonObject.toString());
        } catch (IOException e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.err.println("Failed to write the JSON file: " + driverCfgPath);
        }
    }

}
