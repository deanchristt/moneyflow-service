package com.moneyflow.service;

import com.moneyflow.config.CurrencyProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyServiceTest {

    private CurrencyService service;

    @BeforeEach
    void setUp() {
        CurrencyProperties props = new CurrencyProperties();
        props.setBase("USD");
        props.setRates(Map.of(
                "USD", BigDecimal.ONE,
                "EUR", new BigDecimal("1.08"),
                "IDR", new BigDecimal("0.000063")));
        service = new CurrencyService(props);
    }

    @Test
    void toBaseConvertsForeignCurrency() {
        assertThat(service.toBase(new BigDecimal("100"), "EUR")).isEqualByComparingTo("108");
        assertThat(service.toBase(new BigDecimal("1000000"), "IDR")).isEqualByComparingTo("63");
    }

    @Test
    void baseCurrencyIsUnchanged() {
        assertThat(service.toBase(new BigDecimal("50"), "USD")).isEqualByComparingTo("50");
    }

    @Test
    void convertBetweenTwoCurrenciesRoundTrips() {
        BigDecimal inUsd = service.convert(new BigDecimal("100"), "EUR", "USD");
        assertThat(inUsd).isEqualByComparingTo("108");
        assertThat(service.convert(inUsd, "USD", "EUR")).isEqualByComparingTo("100");
    }

    @Test
    void unknownCurrencyFallsBackToOneToOne() {
        assertThat(service.toBase(new BigDecimal("100"), "XXX")).isEqualByComparingTo("100");
    }
}
