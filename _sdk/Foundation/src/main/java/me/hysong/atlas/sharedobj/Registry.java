package me.hysong.atlas.sharedobj;

import lombok.Getter;
import me.lks410.libhy2.jsoncoder.Decodable;
import me.lks410.libhy2.jsoncoder.Encodable;
import me.lks410.libhy2.jsoncoder.JsonCodable;

import java.util.HashMap;
import java.util.Objects;

@Encodable
@Decodable
@Getter
public class Registry implements JsonCodable {

    private String hive;
    private String version;
    private HashMap<String, String> registry = new HashMap<>();

    // File structure
    //
    // {
    //     "hive": "HKEY_LOCAL_MACHINE",
    //     "version": "1.0",
    //     "registry": {
    //         "SOFTWARE/Microsoft/Windows NT/CurrentVersion/Winlogon": {}
    //         "SOFTWARE/Microsoft/Windows NT/CurrentVersion/Winlogon/AutoRestartShell": 1,
    //     }
    // }

    public Registry() {
        this.hive = "";
        this.version = "1.0";
        this.registry = new HashMap<>();
    }

    public Registry(String hive, String version) {
        this.hive = hive;
        this.version = version;
        this.registry = new HashMap<>();
    }

    public void configureAsVirtualRegistry() {
        this.hive = "HKEY_VIRTUAL_MACHINE";
        this.version = "1.0";
        this.registry = new HashMap<>();

        // Set values
        this.registry.put("SOFTWARE/Microsoft/Windows NT/CurrentVersion/Winlogon", "");
    }
}
