package me.hysong.kynesystems.apps.kstradermachine.subwins;

import lombok.Getter;
import me.hysong.atlas.interfaces.KSApplication;
import me.hysong.atlas.sdk.graphite.v1.KSGraphicalApplication;
import me.hysong.atlas.sharedobj.KSEnvironment;

import javax.swing.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.function.UnaryOperator;

@Getter
public class ProfitLogs extends KSGraphicalApplication implements KSApplication {
    private final String appDisplayName = "Profit Logs";
    private final int closeBehavior = JFrame.DISPOSE_ON_CLOSE;
    private final int windowWidth = 800;
    private final int windowHeight = 600;

    private final static ArrayList<ProfitEntry> profitEntries = new ArrayList<>(); // Oldest goes latest


    @Override
    public int appMain(KSEnvironment environment, String execLocation, String[] args) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // UI building goes here

        // The log shows from latest to oldest - meaning that most recent trade log goes to the top of the scroll.

        return 0;
    }

    public static void add(ProfitEntry entry) {
        profitEntries.add(entry);
    }

    @Getter
    public static class ProfitEntry implements Serializable {
        private final int precision = 2;
        private final long epochTime;
        private final String timestamp;
        private final String symbol;
        private final String exchange;
        private double amountTradeRealCurrency;
        private String amountTradeRealCurrencyUnit;
        private double amountProfitRealCurrency;
        private String amountProfitRealCurrencyUnit;

        public ProfitEntry(long epochTime, String symbol, String exchange, double amountTradeRealCurrency, String amountTradeRealCurrencyUnit, double amountProfitRealCurrency, String amountProfitRealCurrencyUnit) {
            this.epochTime = epochTime;
            this.symbol = symbol;
            this.exchange = exchange;
            this.amountTradeRealCurrency = amountTradeRealCurrency;
            this.amountTradeRealCurrencyUnit = amountTradeRealCurrencyUnit;
            this.amountProfitRealCurrency = amountProfitRealCurrency;
            this.amountProfitRealCurrencyUnit = amountProfitRealCurrencyUnit;

            this.timestamp = ""; // TODO Update this to "YYYY-MM-DD HH:mm:ss" format based on epoch time
        }

        public ProfitEntry(String line) {
            // TODO Fill here
        }

        public void changeCurrencyForProfit(String newUnit, UnaryOperator<Double> originalToUSD, UnaryOperator<Double> usdToNewCurrency) {
            this.amountProfitRealCurrencyUnit = newUnit;
            this.amountProfitRealCurrency = usdToNewCurrency.apply(originalToUSD.apply(this.amountProfitRealCurrency));
        }

        public void changeCurrencyForTrade(String newUnit, UnaryOperator<Double> originalToUSD, UnaryOperator<Double> usdToNewCurrency) {
            this.amountTradeRealCurrencyUnit = newUnit;
            this.amountTradeRealCurrency = usdToNewCurrency.apply(originalToUSD.apply(this.amountTradeRealCurrency));
        }

        public String toSerializeString() {
            return new StringBuilder()
                    .append(this.epochTime).append("//")
                    .append(this.symbol).append("//")
                    .append(this.exchange).append("//")
                    .append(this.amountTradeRealCurrency).append("//")
                    .append(this.amountTradeRealCurrencyUnit).append("//")
                    .append(this.amountProfitRealCurrency).append("//")
                    .append(this.amountProfitRealCurrencyUnit).append("//")
                    .toString();
        }
    }

}
