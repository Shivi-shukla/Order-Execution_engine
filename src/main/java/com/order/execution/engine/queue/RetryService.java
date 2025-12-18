package com.order.execution.engine.queue;

import com.order.execution.engine.model.OrderStatus;
import com.order.execution.engine.model.dto.RetryStats;
import com.order.execution.engine.model.entity.Order;
import com.order.execution.engine.repository.jpa.OrderRepository;
import com.order.execution.engine.repository.redis.ActiveOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RetryService {

    private static final String RETRY_COUNT_KEY = "retry:count:%s";
    private static final String RETRY_NEXT_ATTEMPT_KEY = "retry:next:%s";
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_DELAY_MS = 1000;  // 1s
    private static final double BACKOFF_MULTIPLIER = 2.0; // Exponential

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedisOrderQueue redisOrderQueue;

    @Autowired
    private OrderRepository orderRepo;

    @Autowired
    private ActiveOrderRepository activeOrderRepo;

    /**
     * Handle failed order - implements exponential backoff (max 3 attempts)
     */
    public void retry(String orderId, Exception error) {
        int retryCount = getRetryCount(orderId);

        if (retryCount >= MAX_RETRIES) {
            failOrderPermanently(orderId, error);
            return;
        }

        long nextDelayMs = calculateBackoffDelay(retryCount);
        String nextAttemptKey = String.format(RETRY_NEXT_ATTEMPT_KEY, orderId);

        // Schedule next retry
        redisTemplate.opsForValue().set(nextAttemptKey, String.valueOf(nextDelayMs),
                nextDelayMs, TimeUnit.MILLISECONDS);

        // Increment retry count
        incrementRetryCount(orderId);

        log.warn("Order {} failed (attempt {}/{}): {}. Retrying in {}ms", orderId, retryCount + 1, MAX_RETRIES, error.getMessage(), nextDelayMs);
    }

    /**
     * Process scheduled retries
     */
    @Scheduled(fixedRate = 1000) // Check every second
    public void processRetries() {
        String pattern = RETRY_NEXT_ATTEMPT_KEY.replace("%s", "*");
        // Note: KEYS * in production is slow - use Redis Streams in prod

        redisTemplate.keys(pattern).forEach(key -> {
            String orderId = extractOrderId(key);
            Long ttl = redisTemplate.getExpire(key);

            if (ttl != null && ttl <= 1) { // Ready to retry
                processRetry(orderId);
                redisTemplate.delete(key);
            }
        });
    }

    private void processRetry(String orderId) {
        // Re-enqueue for processing
        redisOrderQueue.enqueue(orderId);
        log.info("Retrying order: {}", orderId);
    }

    /**
     * Permanently fail order after max retries
     */
    private void failOrderPermanently(String orderId, Exception error) {
        Order order = orderRepo.findByOrderId(orderId).orElse(null);
        if (order != null) {
            order.setStatus(OrderStatus.FAILED);
            order.setError(String.format("Max retries exceeded: %s", error.getMessage()));
            orderRepo.save(order);
        }

        // Cleanup Redis
        cleanupRetryKeys(orderId);
        activeOrderRepo.completeOrder(orderId, null);

        log.error("Order {} PERMANENTLY FAILED after {} retries: {}", orderId, MAX_RETRIES, error.getMessage());
    }

    /**
     * Calculate exponential backoff delay: 1s, 2s, 4s
     */
    private long calculateBackoffDelay(int retryCount) {
        return (long) (INITIAL_DELAY_MS * Math.pow(BACKOFF_MULTIPLIER, retryCount));
    }

    /**
     * Get current retry count from Redis
     */
    private int getRetryCount(String orderId) {
        String key = String.format(RETRY_COUNT_KEY, orderId);
        String countStr = redisTemplate.opsForValue().get(key);
        return countStr != null ? Integer.parseInt(countStr) : 0;
    }

    /**
     * Increment retry count (atomic)
     */
    private void incrementRetryCount(String orderId) {
        String key = String.format(RETRY_COUNT_KEY, orderId);
        redisTemplate.opsForValue().increment(key, 1);
        redisTemplate.expire(key, Duration.ofHours(1).toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Cleanup all retry keys for order
     */
    private void cleanupRetryKeys(String orderId) {
        String countKey = String.format(RETRY_COUNT_KEY, orderId);
        String nextKey = String.format(RETRY_NEXT_ATTEMPT_KEY, orderId);
        redisTemplate.delete(countKey);
        redisTemplate.delete(nextKey);
    }

    /**
     * Extract orderId from Redis key
     */
    private String extractOrderId(String key) {
        return key.replace(RETRY_NEXT_ATTEMPT_KEY.replace("%s", ""), "");
    }

    /**
     * Get retry stats for monitoring
     */
    public RetryStats getRetryStats() {
        return RetryStats.builder()
                .totalRetries(getTotalRetries())
                .build();
    }

    private long getTotalRetries() {
        return redisTemplate.keys(RETRY_COUNT_KEY.replace("%s", "*")).size();
    }

}
