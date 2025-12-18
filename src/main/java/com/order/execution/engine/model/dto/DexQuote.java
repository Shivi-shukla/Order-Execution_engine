package com.order.execution.engine.model.dto;

import java.math.BigDecimal;

public record DexQuote(String dexName, BigDecimal price, BigDecimal fee, BigDecimal netPrice){
}
