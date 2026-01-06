package com.moneyflow.service;

import com.moneyflow.config.CurrencyProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Converts monetary amounts between currencies using statically-configured rates
 * ({@link CurrencyProperties}). An unknown currency falls back to a 1:1 rate with
 * a warning so reporting never fails on missing configuration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyService {

    private static final int SCALE = 4;

    private final CurrencyProperties properties;

    public String getBaseCurrency() {
        return properties.getBase();
    }

    /** Convert an amount in {@code currency} to the configured base currency. */
    public BigDecimal toBase(BigDecimal amount, String currency) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(rateOf(currency)).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** Convert between two arbitrary currencies. */
    public BigDecimal convert(BigDecimal amount, String from, String to) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal inBase = amount.multiply(rateOf(from));
        return inBase.divide(rateOf(to), SCALE, RoundingMode.HALF_UP);
    }

    /** Value of one unit of {@code currency} in the base currency. */
    private BigDecimal rateOf(String currency) {
        if (currency == null || currency.equalsIgnoreCase(properties.getBase())) {
            return BigDecimal.ONE;
        }
        BigDecimal rate = properties.getRates().get(currency);
        if (rate == null) {
            log.warn("No FX rate configured for currency '{}'; treating as 1:1 with base '{}'",
                    currency, properties.getBase());
            return BigDecimal.ONE;
        }
        return rate;
    }
}
