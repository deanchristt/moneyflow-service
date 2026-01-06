package com.moneyflow.model.dto.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceSummaryResponse {

    private String baseCurrency;
    private BigDecimal totalInBase;
    private List<CurrencyBalance> byCurrency;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrencyBalance {
        private String currency;
        private BigDecimal total;
        private BigDecimal totalInBase;
    }
}
