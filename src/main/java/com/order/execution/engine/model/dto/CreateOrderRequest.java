package com.order.execution.engine.model.dto;

import java.math.BigDecimal;

public record CreateOrderRequest(String tokenIn, String tokenOut, BigDecimal amountIn, BigDecimal slippage){

}
