package me.hysong.apis.kstrader.v1.objects;

import lombok.Getter;

import java.util.ArrayList;

@Getter
public class Chart {
    private String symbol;
    private String interval;
    private long startTime;
    private long endTime;
    private MarketTypes type;
    private String exchange;
    private String accountId;
    private ArrayList<Candlestick> candlesticks;

    public Chart(String symbol, String interval, long startTime, long endTime, MarketTypes type, String exchange, String accountId) {
        this.symbol = symbol;
        this.interval = interval;
        this.startTime = startTime;
        this.endTime = endTime;
        this.type = type;
        this.exchange = exchange;
        this.accountId = accountId;
        this.candlesticks = new ArrayList<>();
    }

    public void addCandlestick(Candlestick candlestick) {
        this.candlesticks.add(candlestick);
    }

    @Override
    public String toString() {
        return "Chart{" +
                "symbol='" + symbol + '\'' +
                ", interval='" + interval + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", type=" + type +
                ", exchange='" + exchange + '\'' +
                ", accountId='" + accountId + '\'' +
                ", candlesticks=" + candlesticks +
                '}';
    }

    public String toStructuredString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Chart Details:\n");
        sb.append("Symbol: ").append(symbol).append("\n");
        sb.append("Interval: ").append(interval).append("\n");
        sb.append("Start Time: ").append(startTime).append("\n");
        sb.append("End Time: ").append(endTime).append("\n");
        sb.append("Type: ").append(type).append("\n");
        sb.append("Exchange: ").append(exchange).append("\n");
        sb.append("Account ID: ").append(accountId).append("\n");
        sb.append("Candlesticks:\n");

        for (Candlestick candlestick : candlesticks) {
            sb.append(candlestick.toStructuredString()).append("\n");
        }

        return sb.toString();
    }
}
