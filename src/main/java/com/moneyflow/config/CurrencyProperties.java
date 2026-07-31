package com.moneyflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * FX configuration. {@code base} is the reporting currency; {@code rates} maps
 * each currency code to the value of one of its units expressed in the base
 * currency (base itself = 1).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "moneyflow.currency")
public class CurrencyProperties {

    private String base = "USD";
    private Map<String, BigDecimal> rates = new HashMap<>();
}
