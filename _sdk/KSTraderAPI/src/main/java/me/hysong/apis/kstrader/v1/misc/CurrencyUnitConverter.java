package me.hysong.apis.kstrader.v1.misc;

import java.util.function.UnaryOperator;

public interface CurrencyUnitConverter extends UnaryOperator<Double> {

    String getFrom();
    String getTo();

}
