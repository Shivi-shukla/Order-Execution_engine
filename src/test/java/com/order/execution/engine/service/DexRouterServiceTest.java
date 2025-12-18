package com.order.execution.engine.service;

import com.order.execution.engine.model.dto.DexQuote;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class DexRouterServiceTest {

    @Autowired
    private DexRouterService dexRouter;

    @Test
    void shouldGetBestQuote() {
        DexQuote quote = dexRouter.getBestQuote(
                "SOL", "USDC", BigDecimal.valueOf(1.0)
        );

        assertNotNull(quote);
        assertNotNull(quote.dexName());
        System.out.println("Best quote: " + quote.dexName() + " @ " + quote.netPrice());
    }

}
