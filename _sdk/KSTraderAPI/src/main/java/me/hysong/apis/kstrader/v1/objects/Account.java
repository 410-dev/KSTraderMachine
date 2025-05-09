package me.hysong.apis.kstrader.v1.objects;

import lombok.Getter;
import me.hysong.atlas.utils.SIDKit;

import java.util.ArrayList;
import java.util.HashMap;

@Getter
public class Account {

    public static final String CREDENTIAL_KEY_PK = "pk";
    public static final String CREDENTIAL_KEY_SK = "sk";

    private String accountId;
    private String accountName;
    private String accountType; // e.g. "spot", "futures"
    private String exchange; // e.g. "binance", "upbit"

    private HashMap<String, Object> credentials; // e.g. {"pk": "your_pk", "sk": "your_sk"}
    private HashMap<String, Chart> charts; // e.g. {"Spot:BTCUSDT": ChartObject}
    private HashMap<String, ArrayList<Order>> orders; // e.g. {"Spot:BTCUSDT": [OrderObject1, OrderObject2]}

    public Account(String accountId, String accountName, String accountType, String exchange, HashMap<String, Object> credentials) {
        this.accountId = accountId;
        this.accountName = accountName;
        this.accountType = accountType;
        this.exchange = exchange;
        this.credentials = credentials;
    }

    public Account(String accountType, String exchange, String pk, String sk) {
        this.accountId = SIDKit.generateSID(SIDKit.SIDType.USER_OBJECT);
        this.accountName = "Default Account";
        this.accountType = accountType;
        this.exchange = exchange;
        this.credentials = new HashMap<>();
        this.credentials.put(CREDENTIAL_KEY_PK, pk);
        this.credentials.put(CREDENTIAL_KEY_SK, sk);
    }

}
