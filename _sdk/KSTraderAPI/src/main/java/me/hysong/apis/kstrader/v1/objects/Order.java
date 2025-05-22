package me.hysong.apis.kstrader.v1.objects;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;

@Getter
@Setter
public class Order {

    public static final String SPOT = "SPOT";
    public static final String FUTURE = "FUTURE";
    public static final String OPTION = "OPTION";
    public static final String PERPETUAL = "PERPETUAL";

    // * means required for making order
    private String orderId;   //   RC-xxxxxxxx-xxxxxxxx or UUID
    private String ownerId;   //   User ID
    private String exchange;  //   e.g. "BITHUMB", "BINANCE", "UPBIT"
    private long time;        //   Order creation time in epoch milliseconds
    private String symbol;    // * e.g. "KRW-BTC"
    private boolean buySide;  // * "buy" or "sell"
    private String type;      // * e.g. "LIMIT", "MARKET"
    private String marketType;//  e.g. "SPOT", "FUTURE", "OPTION", "PERPETUAL"
    private double price;     // * Order price
    private double amount;    // * Order amount (In base currency, product of leverage and amount in quote currency)
    private double fee;       //   Fee charged
    private boolean isOpen;
    private boolean isCanceled;
    private boolean isFilled;
    private boolean isExpired;
    private boolean isLiquidated;
    private boolean isClosed;
    private String status;    //   e.g. "OPEN", "CLOSED", "CANCELED", "EXPIRED", "LIQUIDATED"
    private String error;     //   Error message if any
    private String errorCode; //   Error code if any
    private HashMap<String, Object> xattr = new HashMap<>();


    public Order(String symbol, String marketType, boolean buySide, String type, double price, double amount) {
        this.symbol = symbol;
        this.marketType = marketType;
        this.buySide = buySide;
        this.type = type;
        this.price = price;
        this.amount = amount;
    }

    public Order() {
        // Default constructor
    }


    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", ownerId='" + ownerId + '\'' +
                ", exchange='" + exchange + '\'' +
                ", time=" + time +
                ", symbol='" + symbol + '\'' +
                ", buySide=" + buySide +
                ", type='" + type + '\'' +
                ", marketType='" + marketType + '\'' +
                ", price=" + price +
                ", amount=" + amount +
                ", fee=" + fee +
                ", isOpen=" + isOpen +
                ", isCanceled=" + isCanceled +
                ", isFilled=" + isFilled +
                ", isExpired=" + isExpired +
                ", isLiquidated=" + isLiquidated +
                ", isClosed=" + isClosed +
                ", status='" + status + '\'' +
                ", error='" + error + '\'' +
                ", errorCode='" + errorCode + '\'' +
                '}';
    }

    public String toStructuredString() {
        String sb = "Order Details:\n" +
                "     Order ID: " + orderId + "\n" +
                "     Owner ID: " + ownerId + "\n" +
                "     Exchange: " + exchange + "\n" +
                "    Time (ms): " + time + "\n" +
                "         Time: " + new java.util.Date(time) + "\n" +
                "       Symbol: " + symbol + "\n" +
                "     Buy Side: " + buySide + "\n" +
                "         Type: " + type + "\n" +
                "  Market Type: " + marketType + "\n" +
                "        Price: " + String.format("%.2f", price) + "  (unit of base currency)" + "\n" +
                "       Amount: " + String.format("%.2f", amount) + "  (unit of crypto currency)" + "\n" +
                "          Fee: " + fee + "  (unit of base currency)" + "\n" +
                "      Is Open: " + isOpen + "\n" +
                "  Is Canceled: " + isCanceled + "\n" +
                "    Is Filled: " + isFilled + "\n" +
                "   Is Expired: " + isExpired + "\n" +
                "Is Liquidated: " + isLiquidated + "\n" +
                "       Status: " + status + "\n" +
                "  Error value: " + error + "\n" +
                "   Error Code: " + errorCode + "\n";

        return sb;
    }

}
