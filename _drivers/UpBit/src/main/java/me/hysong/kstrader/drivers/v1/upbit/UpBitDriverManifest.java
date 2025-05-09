package me.hysong.kstrader.drivers.v1.upbit;

import lombok.Getter;
import me.hysong.apis.kstrader.v1.driver.TraderDriver;
import me.hysong.apis.kstrader.v1.driver.TraderDriverManifest;
import me.hysong.apis.kstrader.v1.driver.TraderDriverSettings;
import me.hysong.apis.kstrader.v1.objects.DriverExitCode;

@Getter
public class UpBitDriverManifest implements TraderDriverManifest {
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
    public TraderDriver getDriver() {
        return new UpBitDriver();
    }

    @Override
    public TraderDriverSettings getPreferenceObject(String driverCfgPath) {
        return new UpBitPreference(driverCfgPath);
    }
}
