package me.hysong.kstrader.drivers.v1.upbit;

import lombok.Getter;
import me.hysong.apis.kstrader.v1.driver.TraderDriverSettingsV1;

import java.util.HashMap;

@Getter
public class UpBitPreference extends TraderDriverSettingsV1 {

    private final String exchange = new UpBitDriverManifestV1().getDriverExchange();
    private final String endpoint = new UpBitDriverManifestV1().getDriverAPIEndpoint();

    private final HashMap<String, Object> values = new HashMap<>();

    public UpBitPreference(String driverCfgPath) {
        super(driverCfgPath);
    }

    @Override
    public HashMap<String, Class<?>> getTypes() {
        HashMap<String, Class<?>> types = new HashMap<>();
        types.put("auth.apiAK", String.class);
        types.put("auth.apiSK", String.class);
        types.put("future.leverage", Integer.class);
        types.put("option.leverage", Integer.class);
        return types;
    }

    @Override
    public HashMap<String, HashMap<String, String>> getDescriptions() {
        HashMap<String, String> en_us = new HashMap<>();
        en_us.put("auth.apiAK", "Open API access key from UpBit. Required permission: READ[asset, order] and WRITE[order]");
        en_us.put("auth.apiSK", "Open API Secret key from UpBit. Required permission: READ[asset, order] and WRITE[order]");
        en_us.put("future.leverage", "Future Leverage as integer. Default is 1.");
        en_us.put("option.leverage", "Option Leverage as integer. Default is 1.");

        HashMap<String, String> ko_kr = new HashMap<>();
        ko_kr.put("auth.apiAK", "UpBit 에서 발급받은 Open API 접근 키. 필요 권한: READ[asset, order] and WRITE[order]");
        ko_kr.put("auth.apiSK", "UpBit 에서 발급받은 Open API 비밀 키. 필요 권한: READ[asset, order] and WRITE[order]");
        ko_kr.put("future.leverage", "선물 레버리지. 기본값은 1입니다.");
        ko_kr.put("option.leverage", "옵션 레버리지. 기본값은 1입니다.");

        HashMap<String, HashMap<String, String>> descriptions = new HashMap<>();
        descriptions.put("en_us", en_us);
        descriptions.put("ko_kr", ko_kr);
        return descriptions;
    }

    @Override
    public HashMap<String, HashMap<String, String>> getLabels() {
        HashMap<String, String> en_us = new HashMap<>();
        en_us.put("auth.apiAK", "API Access Key");
        en_us.put("auth.apiSK", "API Secret Key");
        en_us.put("future.leverage", "Future Leverage");
        en_us.put("option.leverage", "Option Leverage");

        HashMap<String, String> ko_kr = new HashMap<>();
        ko_kr.put("auth.apiAK", "API 접근 키");
        ko_kr.put("auth.apiSK", "API 비밀 키");
        ko_kr.put("future.leverage", "선물 레버리지");
        ko_kr.put("option.leverage", "옵션 레버리지");

        HashMap<String, HashMap<String, String>> labels = new HashMap<>();
        labels.put("en_us", en_us);
        labels.put("ko_kr", ko_kr);
        return labels;
    }

    @Override
    public HashMap<String, Object> getDefaults() {
        HashMap<String, Object> defaults = new HashMap<>();
        defaults.put("auth.apiAK", "");
        defaults.put("auth.apiSK", "");
        defaults.put("future.leverage", 1);
        defaults.put("option.leverage", 1);
        return defaults;
    }
}
