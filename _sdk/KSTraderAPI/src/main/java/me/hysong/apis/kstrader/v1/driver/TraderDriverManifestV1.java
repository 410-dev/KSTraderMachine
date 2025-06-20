package me.hysong.apis.kstrader.v1.driver;

import com.google.gson.JsonObject;
import me.hysong.apis.kstrader.v1.objects.Account;
import me.hysong.apis.kstrader.v1.objects.DriverExitCode;

public interface TraderDriverManifestV1 {

    String getDriverName();          // Driver name, ex. "UpBit Generic Driver"
    String getDriverExchangeName();  // Exchange name, ex. "UpBit"
    String getDriverExchange();      // Exchange with supported types. Format: "<url>[support]", ex. "upbit.com[spot,future,option]"
    String getDriverAPIEndpoint();   // API endpoint, ex. "https://api.upbit.com/v1/"
    String getDriverVersion();
    boolean isSupportREST();
    boolean isSupportWS();
    boolean isSupportFuture();
    boolean isSupportOption();
    boolean isSupportPerpetual();
    boolean isSupportSpot();
    boolean isSupportOrderAsLimit();
    boolean isSupportOrderAsMarket();
    String getDriverUpdateDate();
    String[] getSupportedSymbols(); // Supported symbols, ex. ["KRW-BTC", "BTC-USDT"]
    DriverExitCode testConnection();
    TraderDriverV1 getDriver();
    TraderDriverSettingsV1 getPreferenceObject(String driverCfgPath);
    Account getAccount(String type, JsonObject preferenceFile);

    default String getFileSystemIdentifier() {
        return getDriverExchange() + "@" + getDriverAPIEndpoint().replace("/", "_").replace(":", "_");
    }
}
