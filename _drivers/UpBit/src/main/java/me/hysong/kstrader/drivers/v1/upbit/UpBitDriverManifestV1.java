package me.hysong.kstrader.drivers.v1.upbit;

import lombok.Getter;
import me.hysong.apis.kstrader.v1.driver.TraderDriverV1;
import me.hysong.apis.kstrader.v1.driver.TraderDriverManifestV1;
import me.hysong.apis.kstrader.v1.driver.TraderDriverSettingsV1;
import me.hysong.apis.kstrader.v1.objects.DriverExitCode;

@Getter
public class UpBitDriverManifestV1 implements TraderDriverManifestV1 {
    private final String driverName = "UpBit";
    private final String driverExchangeName = "UpBit";
    private final String driverExchange = "upbit.com[spot]";
    private final String driverAPIEndpoint = "https://api.upbit.com/v1/";;
    private final String driverVersion = "1.0.0";
    private final boolean supportFuture = false;
    private final boolean supportOption = false;
    private final boolean supportPerpetual = false;
    private final boolean supportSpot = true;
    private final boolean supportWS = true;
    private final boolean supportREST = true;
    private final String driverUpdateDate = "2025-05-30";
    private final String[] supportedSymbols = new String[] {
            "KRW-BTC",
            "KRW-ETH",
            "KRW-XRP",
            "KRW-LTC",
            "KRW-BCH"
    };

    public DriverExitCode testConnection() {
        return DriverExitCode.DRIVER_TEST_OK;
    }

    @Override
    public TraderDriverV1 getDriver() {
        return new UpBitDriverV1();
    }

    @Override
    public TraderDriverSettingsV1 getPreferenceObject(String driverCfgPath) {
        return new UpBitPreference(driverCfgPath);
    }
}
