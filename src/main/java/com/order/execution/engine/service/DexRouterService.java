package com.order.execution.engine.service;

import com.order.execution.engine.model.SwapResult;
import com.order.execution.engine.model.dto.DexQuote;
import com.order.execution.engine.model.entity.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class DexRouterService {

    @Autowired private MockDexService mockDex;

    // ✅ Now works with synchronous MockDexService
    public DexQuote getRaydiumQuote(Order order) {
        return mockDex.getRaydiumQuote(order.getTokenIn(), order.getTokenOut(), order.getAmountIn());
    }

    public DexQuote getMeteoraQuote(Order order) {
        return mockDex.getMeteoraQuote(order.getTokenIn(), order.getTokenOut(), order.getAmountIn());
    }

    public SwapResult execute(String dexName, Order order) {
        return mockDex.executeSwap(dexName, order);
    }

    public DexQuote getBestQuote(String tokenIn, String tokenOut, BigDecimal amount) {
        DexQuote raydium = mockDex.getRaydiumQuote(tokenIn, tokenOut, amount);
        DexQuote meteora = mockDex.getMeteoraQuote(tokenIn, tokenOut, amount);
        return compareQuotes(raydium, meteora);  // Direct call, no CompletableFuture
    }

    private DexQuote compareQuotes(DexQuote r, DexQuote m) {
        return r.netPrice().compareTo(m.netPrice()) > 0 ? r : m;  // Best = higher net price
    }

}
