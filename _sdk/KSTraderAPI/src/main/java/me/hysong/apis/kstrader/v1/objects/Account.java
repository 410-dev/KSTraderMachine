package me.hysong.apis.kstrader.v1.objects;

import liblks.security.CoreSHA;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;

@Getter
public class Account {

    public static final String CREDENTIAL_KEY_PK = "pk";
    public static final String CREDENTIAL_KEY_SK = "sk";

    private final String accountType; // e.g. "spot", "futures"
    private final String exchange; // e.g. "binance", "upbit"

    private final HashMap<String, Object> credentials; // e.g. {"pk": "your_pk", "sk": "your_sk"}
    private HashMap<String, Chart> charts; // e.g. {"Spot:BTCUSDT": ChartObject}
    private HashMap<String, ArrayList<Order>> orders; // e.g. {"Spot:BTCUSDT": [OrderObject1, OrderObject2]}

    public Account(String accountType, String exchange, HashMap<String, Object> credentials) {
        this.accountType = accountType;
        this.exchange = exchange;
        this.credentials = credentials;
    }

    public Account(String accountType, String exchange, String pk, String sk) {
        this.accountType = accountType;
        this.exchange = exchange;
        this.credentials = new HashMap<>();
        this.credentials.put(CREDENTIAL_KEY_PK, pk);
        this.credentials.put(CREDENTIAL_KEY_SK, sk);
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountType='" + accountType + '\'' +
                ", exchange='" + exchange + '\'' +
                ", credentials=" + credentials +
                ", charts=" + charts +
                ", orders=" + orders +
                '}';
    }

    public String getUniqueID() {
        StringBuilder sb = new StringBuilder();
        for (Object v : credentials.values()) {
            sb.append(v.toString());
        }
        return CoreSHA.hash256(sb.toString()).toUpperCase();
    }
}
