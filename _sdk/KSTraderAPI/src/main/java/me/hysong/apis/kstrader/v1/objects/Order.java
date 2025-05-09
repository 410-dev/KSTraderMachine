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
        StringBuilder sb = new StringBuilder();
        sb.append("Order Details:\n");
        sb.append("     Order ID: ").append(orderId).append("\n");
        sb.append("     Owner ID: ").append(ownerId).append("\n");
        sb.append("     Exchange: ").append(exchange).append("\n");
        sb.append("    Time (ms): ").append(time).append("\n");
        sb.append("         Time: ").append(new java.util.Date(time)).append("\n");
        sb.append("       Symbol: ").append(symbol).append("\n");
        sb.append("     Buy Side: ").append(buySide).append("\n");
        sb.append("         Type: ").append(type).append("\n");
        sb.append("  Market Type: ").append(marketType).append("\n");
        sb.append("        Price: ").append(String.format("%.2f", price)).append("  (unit of base currency)").append("\n");
        sb.append("       Amount: ").append(String.format("%.2f", amount)).append("  (unit of crypto currency)").append("\n");
        sb.append("          Fee: ").append(fee).append("  (unit of base currency)").append("\n");
        sb.append("      Is Open: ").append(isOpen).append("\n");
        sb.append("  Is Canceled: ").append(isCanceled).append("\n");
        sb.append("    Is Filled: ").append(isFilled).append("\n");
        sb.append("   Is Expired: ").append(isExpired).append("\n");
        sb.append("Is Liquidated: ").append(isLiquidated).append("\n");
        sb.append("       Status: ").append(status).append("\n");
        sb.append("  Error value: ").append(error).append("\n");
        sb.append("   Error Code: ").append(errorCode).append("\n");

        return sb.toString();
    }

}
