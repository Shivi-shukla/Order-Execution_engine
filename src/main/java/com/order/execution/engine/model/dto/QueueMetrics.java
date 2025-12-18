package com.order.execution.engine.model.dto;

import com.order.execution.engine.model.OrderStatus;
import lombok.Builder;
import java.util.Map;

@Builder
public record QueueMetrics(long queueSize,
                           long activeOrders,
                           long peakQueueSize,
                           double processingRate,
                           Map<OrderStatus, Long> statusCounts,  // FIXED: Long not AtomicLong
                           QueueHealth health) {
}
