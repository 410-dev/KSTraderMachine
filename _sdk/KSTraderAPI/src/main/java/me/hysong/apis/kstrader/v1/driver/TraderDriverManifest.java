package me.hysong.apis.kstrader.v1.driver;

import me.hysong.apis.kstrader.v1.objects.DriverExitCode;

public interface TraderDriverManifest {

    String getDriverName();          // Driver name, ex. "UpBit Generic Driver"
    String getDriverExchangeName();  // Exchange name, ex. "UpBit"
    String getDriverExchange();      // Exchange with supported types. Format: "<url>[support]", ex. "upbit.com[spot,future,option]"
    String getDriverAPIEndpoint();   // API endpoint, ex. "https://api.upbit.com/v1/"
    String getDriverVersion();
    boolean isSupportFuture();
    boolean isSupportOption();
    boolean isSupportPerpetual();
    boolean isSupportSpot();
    boolean isSupportREST();
    boolean isSupportWS();
    String getDriverUpdateDate();
    String[] getSupportedSymbols(); // Supported symbols, ex. ["KRW-BTC", "BTC-USDT"]
    DriverExitCode testConnection();
    TraderDriver getDriver();
    TraderDriverSettings getPreferenceObject(String driverCfgPath);

}
