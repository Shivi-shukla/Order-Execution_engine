package com.order.execution.engine.model;

import java.math.BigDecimal;
import java.util.Objects;

public record SwapResult(
        String txHash,           // Mock/real transaction hash
        BigDecimal executedPrice, // Final price after execution
        String dexName           // "Raydium" or "Meteora"
) {

    public SwapResult {
        Objects.requireNonNull(txHash, "txHash cannot be null");
        Objects.requireNonNull(executedPrice, "executedPrice cannot be null");
        Objects.requireNonNull(dexName, "dexName cannot be null");
    }
}
