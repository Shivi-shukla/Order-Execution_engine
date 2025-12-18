package com.order.execution.engine.repository.redis;

import com.order.execution.engine.model.OrderStatus;
import com.order.execution.engine.model.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Repository
@Slf4j
@RequiredArgsConstructor
public class ActiveOrderRepository {

    private static final String ACTIVE_ORDERS_PREFIX = "active:order:";
    private static final String RETRY_PREFIX = "retry:count:";  // ✅ NEW
    private static final String ACTIVE_ORDERS_SET = "active:orders:set";
    private static final long DEFAULT_TTL_SECONDS = 300;

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Add order to active tracking with TTL (5 min default)
     */
    public void addActiveOrder(String orderId, Order order) {
        String key = ACTIVE_ORDERS_PREFIX + orderId;

        // Store order metadata as JSON (status, tokens, amount)
        redisTemplate.opsForValue().set(key, serializeOrder(order));

        // Set TTL (auto-expire after 5 min if not completed)
        redisTemplate.expire(key, DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);

        // Track in active set for quick listing
        redisTemplate.opsForSet().add(ACTIVE_ORDERS_SET, orderId);
        redisTemplate.expire(ACTIVE_ORDERS_SET, Duration.ofHours(1));

        log.debug("Added active order: {} (TTL: {}s)", orderId, DEFAULT_TTL_SECONDS);
    }

    /**
     * Get active order details
     */
    public Order getActiveOrder(String orderId) {
        String key = ACTIVE_ORDERS_PREFIX + orderId;
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            redisTemplate.opsForSet().remove(ACTIVE_ORDERS_SET, orderId);
            return null;  // ✅ Returns null, not orderId
        }

        return deserializeOrder(json);
    }

    public int getRetryCount(String orderId) {
        String key = RETRY_PREFIX + orderId;
        String countStr = redisTemplate.opsForValue().get(key);
        return countStr != null ? Integer.parseInt(countStr) : 0;
    }

    public void incrementRetry(String orderId) {
        String key = RETRY_PREFIX + orderId;
        Long newCount = redisTemplate.opsForValue().increment(key, 1);
        redisTemplate.expire(key, DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);
    }

    // ✅ FIXED: No Optional
    public void updateOrderStatus(String orderId, OrderStatus status) {
        Order order = getActiveOrder(orderId);
        if (order != null) {
            order.setStatus(status);
            addActiveOrder(orderId, order);
            log.debug("Updated order {} status: {}", orderId, status);
        }
    }

    /**
     * Mark order as completed (removes from active tracking)
     */
    public void completeOrder(String orderId, String txHash) {
        String key = ACTIVE_ORDERS_PREFIX + orderId;
        redisTemplate.delete(key);
        redisTemplate.opsForSet().remove(ACTIVE_ORDERS_SET, orderId);
        log.info("Completed order: {} (tx: {})", orderId, txHash);
    }

    /**
     * Get all active orders (for monitoring)
     */
    public List<String> getAllActiveOrderIds() {
        Set<String> activeIds = redisTemplate.opsForSet().members(ACTIVE_ORDERS_SET);
        return activeIds != null ? new ArrayList<>(activeIds) : Collections.emptyList();
    }

    /**
     * Get active orders count
     */
    public Long getActiveOrdersCount() {
        return redisTemplate.opsForSet().size(ACTIVE_ORDERS_SET);
    }

    /**
     * Cleanup expired orders (manual sweep)
     */
    public void cleanupExpiredOrders() {
        List<String> activeIds = getAllActiveOrderIds();
        long cleaned = 0;

        for (String orderId : activeIds) {
            String key = ACTIVE_ORDERS_PREFIX + orderId;
            if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
                redisTemplate.opsForSet().remove(ACTIVE_ORDERS_SET, orderId);
                cleaned++;
            }
        }

        log.info("Cleaned {} expired orders", cleaned);
    }

    /**
     * Check if order is still active
     */
    public boolean isOrderActive(String orderId) {
        String key = ACTIVE_ORDERS_PREFIX + orderId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // === Private Helpers ===

    private String serializeOrder(Order order) {
        // Simple serialization (in production: use Jackson JSON)
        return String.format("%s|%s|%s|%s",
                order.getStatus(),
                order.getTokenIn(),
                order.getTokenOut(),
                order.getAmountIn().toPlainString()
        );
    }

    private Order deserializeOrder(String data) {
        String[] parts = data.split("\\|");
        Order order = new Order();
        order.setStatus(OrderStatus.valueOf(parts[0]));
        order.setTokenIn(parts[1]);
        order.setTokenOut(parts[2]);
        order.setAmountIn(new java.math.BigDecimal(parts[3]));
        return order;
    }

    /**
     * Extend TTL for long-running orders
     */
    public void extendTTL(String orderId, long seconds) {
        String key = ACTIVE_ORDERS_PREFIX + orderId;
        redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
    }

}
