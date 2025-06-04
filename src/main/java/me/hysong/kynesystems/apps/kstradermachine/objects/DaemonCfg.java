package me.hysong.kynesystems.apps.kstradermachine.objects;

import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;
import me.hysong.atlas.utils.MFS1;
import me.hysong.kynesystems.apps.kstradermachine.Application;
import me.hysong.kynesystems.apps.kstradermachine.subwins.SystemLogs;
import me.lks410.libhy2.jsoncoder.Codable;
import me.lks410.libhy2.jsoncoder.JsonCodable;

import java.util.HashMap;

@Getter
@Setter
@Codable
public class DaemonCfg implements JsonCodable {
    private final int version = 1;
    private int slot;
    private String label;
    private String exchangeDriverClass;
    private String strategyName;
    private String traderMode;
    private String symbol;
    private double maxCashAllocated;
    private String orderMode;
    private transient boolean running;
    private boolean autoRun;
    private HashMap<String, Object> settings;

    public boolean save() {
        SystemLogs.log("INFO", "Saving configurations...:" + this.toIndentedJsonString());
        return MFS1.write(Application.storagePath + "/configs/daemons/" + slot + ".json", this.toIndentedJsonString());
    }

    public void reload() {
        String content = MFS1.readString(Application.storagePath + "/configs/daemons/" + slot + ".json");
        if (content == null) {
            SystemLogs.log("WARNING", "Content is empty.");
            return;
        }
        fromJson(JsonParser.parseString(content).getAsJsonObject());
    }
}