package com.order.execution.engine.model.dto;

import lombok.Builder;

@Builder
public record RetryStats(long totalRetries, long failedOrders) {
}
