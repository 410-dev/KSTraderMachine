package me.hysong.kynesystems.apps.kstradermachine.backend;

import lombok.Getter;
import lombok.Setter;
import me.lks410.libhy2.jsoncoder.Codable;
import me.lks410.libhy2.jsoncoder.JsonCodable;

import java.util.HashMap;

@Getter
@Setter
public class Config implements JsonCodable {
    public static Config config = null;

    @Codable private String version = "0.0.1";
    @Codable private HashMap<String, JsonCodable> configurations = new HashMap<>();


    public void save(String path) {
        // TODO: Implement saving logic
    }

    public static boolean load(String path) {
        // TODO: Implement loading logic
        return false;
    }
}
