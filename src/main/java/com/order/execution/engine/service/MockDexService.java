package com.order.execution.engine.service;

import com.order.execution.engine.model.SwapResult;
import com.order.execution.engine.model.dto.DexQuote;
import com.order.execution.engine.model.entity.Order;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class MockDexService {

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ✅ SYNCHRONOUS for DexRouterService
    public DexQuote getRaydiumQuote(String tokenIn, String tokenOut, BigDecimal amount) {
        sleep(200);
        BigDecimal price = amount.multiply(BigDecimal.valueOf(0.98 + Math.random() * 0.04))
                .setScale(8, RoundingMode.HALF_UP);
        BigDecimal fee = BigDecimal.valueOf(0.003);
        BigDecimal netPrice = price.multiply(BigDecimal.ONE.subtract(fee));
        return new DexQuote("Raydium", price, fee, netPrice);
    }

    public DexQuote getMeteoraQuote(String tokenIn, String tokenOut, BigDecimal amount) {
        sleep(200);
        BigDecimal price = amount.multiply(BigDecimal.valueOf(0.97 + Math.random() * 0.05))
                .setScale(8, RoundingMode.HALF_UP);
        BigDecimal fee = BigDecimal.valueOf(0.002);
        BigDecimal netPrice = price.multiply(BigDecimal.ONE.subtract(fee));
        return new DexQuote("Meteora", price, fee, netPrice);
    }

    // ✅ FIXED: Returns SwapResult
    public SwapResult executeSwap(String dexName, Order order) {
        sleep(2000 + (long)(Math.random() * 1000));
        BigDecimal executedPrice = order.getAmountIn().multiply(BigDecimal.valueOf(0.99));
        return new SwapResult("mock-tx-" + UUID.randomUUID(), executedPrice, dexName);
    }

}
