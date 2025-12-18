package com.order.execution.engine.websocket;

import com.order.execution.engine.model.OrderStatus;

public record StatusMessage(String orderId, OrderStatus status, String txHash) {
}
