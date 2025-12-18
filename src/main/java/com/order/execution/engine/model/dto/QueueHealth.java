package com.order.execution.engine.model.dto;

import lombok.Builder;

@Builder
public record QueueHealth(boolean healthy,
                           long queueSize,
                           long activeOrders,
                           boolean stalled) {
}
