package me.hysong.kstrader.drivers.v1.upbit;

import lombok.Getter;
import me.hysong.apis.kstrader.v1.misc.CurrencyUnitConverter;

@Getter
public class USD2KRW implements CurrencyUnitConverter {
    private final String from = "USD";
    private final String to = "KRW";

    @Override
    public Double apply(Double price) {
        return price * 1400.0; // TODO Sync with exchange rate center, or use price of KRWUSDT
    }
}
