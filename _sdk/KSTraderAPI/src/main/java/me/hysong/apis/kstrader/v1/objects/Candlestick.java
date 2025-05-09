package me.hysong.apis.kstrader.v1.objects;

import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Getter
public class Candlestick {
    private final String exchange;
    private final String symbol;
    private final long openTimeMS;
    private final long closeTimeMS;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final double volume;
    
    public Candlestick(String exchange, String symbol, long openTimeMS, long closeTimeMS, double open, double high, double low, double close, double volume) {
        this.exchange = exchange;
        this.symbol = symbol;
        this.openTimeMS = openTimeMS;
        this.closeTimeMS = closeTimeMS;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    @Override
    public String toString() {
        return "Candlestick{" +
                "exchange='" + exchange + '\'' +
                ", symbol='" + symbol + '\'' +
                ", openTime=" + openTimeMS +
                ", closeTime=" + closeTimeMS +
                ", open=" + String.format("%.2f", open) +
                ", high=" + String.format("%.2f", high) +
                ", low=" + String.format("%.2f", low) +
                ", close=" + String.format("%.2f", close) +
                ", volume=" + String.format("%.2f", volume) +
                '}';
    }

    public String toStructuredString() {
        return "Exchange: " + exchange +
                "\tSymbol: " + symbol +
                "\tO: " + String.format("%.2f", open) +
                "\tH: " + String.format("%.2f", high) +
                "\tL: " + String.format("%.2f", low) +
                "\tC: " + String.format("%.2f", close) +
                "\tV: " + String.format("%.2f", volume) +
                "\tOT: " + openTimeMS +
                "\tOT: " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(
                        LocalDateTime.ofEpochSecond(openTimeMS / 1000, 0, ZoneOffset.systemDefault().getRules().getOffset(LocalDateTime.now()))
                ) +
                "\tCT: " + closeTimeMS +
                "\tCT: " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(
                        LocalDateTime.ofEpochSecond(closeTimeMS / 1000, 0, ZoneOffset.systemDefault().getRules().getOffset(LocalDateTime.now()))
                );
    }
}
