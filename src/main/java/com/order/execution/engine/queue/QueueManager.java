package com.order.execution.engine.queue;

import com.order.execution.engine.model.OrderStatus;
import com.order.execution.engine.model.dto.QueueHealth;
import com.order.execution.engine.model.dto.QueueMetrics;
import com.order.execution.engine.repository.jpa.OrderRepository;
import com.order.execution.engine.repository.redis.ActiveOrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
@Slf4j
public class QueueManager {

    private static final String QUEUE_KEY = "order:queue";
    private static final String METRICS_PREFIX = "orderengine.queue.";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedisOrderQueue redisOrderQueue;

    @Autowired
    private ActiveOrderRepository activeOrderRepo;

    @Autowired
    private OrderRepository orderRepo;

    private final MeterRegistry meterRegistry;

    public QueueManager(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // Metrics
    private Counter ordersSubmittedCounter;
    private Counter ordersCompletedCounter;
    private Counter ordersFailedCounter;

    private final AtomicLong peakQueueSize = new AtomicLong(0);
    private final Map<OrderStatus, Long> statusCounts = new ConcurrentHashMap<>();

    @PostConstruct
    public void initMetrics() {
        ordersSubmittedCounter = Counter.builder(METRICS_PREFIX + "submitted")
                .description("Total orders submitted")
                .register(meterRegistry);

        ordersCompletedCounter = Counter.builder(METRICS_PREFIX + "completed")
                .description("Total orders successfully completed")
                .register(meterRegistry);

        ordersFailedCounter = Counter.builder(METRICS_PREFIX + "failed")
                .description("Total failed orders")
                .register(meterRegistry);
    }

    public QueueMetrics getQueueMetrics() {
        long queueSize = getQueueSize();
        long activeCount = activeOrderRepo.getActiveOrdersCount();

        Map<OrderStatus, Long> statusSnapshot = statusCounts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return QueueMetrics.builder()
                .queueSize(queueSize)
                .activeOrders(activeCount)
                .peakQueueSize(peakQueueSize.get())
                .processingRate(getProcessingRate())
                .statusCounts(statusSnapshot)
                .health(isHealthy(queueSize, activeCount))
                .build();
    }

    public long getQueueSize() {
        Long size = redisTemplate.opsForList().size(QUEUE_KEY);
        return size != null ? size : 0L;
    }

    public List<String> peekQueue(int limit) {
        return redisTemplate.opsForList().range(QUEUE_KEY, 0, Math.min(limit - 1, 100));
    }

    public double getProcessingRate() {
        return 0.0;
    }

    public void drainQueue() {
        log.warn("Draining queue - emergency action");
        while (redisOrderQueue.dequeue() != null) {
            // mark as failed, etc.
        }
    }

    public QueueHealth isHealthy(long queueSize, long activeCount) {
        boolean queueHealthy = queueSize < 1000;
        boolean activeHealthy = activeCount < 50;
        boolean stalled = queueSize > 0 && getQueueAge().compareTo(Duration.ofMinutes(5)) > 0;

        return QueueHealth.builder()
                .healthy(queueHealthy && activeHealthy && !stalled)
                .queueSize(queueSize)
                .activeOrders(activeCount)
                .stalled(stalled)
                .build();
    }

    public Duration getQueueAge() {
        List<String> firstOrders = peekQueue(1);
        if (firstOrders == null || firstOrders.isEmpty()) {
            return Duration.ZERO;
        }
        // TODO: load order from DB and compute age
        return Duration.ZERO;
    }

    public void recordOrderSubmitted() {
        if (ordersSubmittedCounter != null) {
            ordersSubmittedCounter.increment();
        }
    }

    public void recordOrderCompleted() {
        if (ordersCompletedCounter != null) {
            ordersCompletedCounter.increment();
        }
    }

    public void recordOrderFailed() {
        if (ordersFailedCounter != null) {
            ordersFailedCounter.increment();
        }
    }

    @Scheduled(fixedRate = 5000)
    public void updateMetrics() {
        long queueSize = getQueueSize();
        peakQueueSize.accumulateAndGet(queueSize, Math::max);
        long active = activeOrderRepo.getActiveOrdersCount();
        log.debug("Queue: {} | Active: {} | Peak: {}", queueSize, active, peakQueueSize.get());
    }

    @Scheduled(fixedRate = 30000)
    public void cleanupAndReport() {
        activeOrderRepo.cleanupExpiredOrders();
        QueueMetrics metrics = getQueueMetrics();
        if (!metrics.health().healthy()) {
            log.warn("QUEUE ALERT: {}", metrics);
        }
    }

    public void pauseQueue() {
        redisTemplate.opsForSet().add("queue:paused", "true");
        log.info("Queue processing PAUSED");
    }

    public void resumeQueue() {
        redisTemplate.opsForSet().remove("queue:paused", "true");
        log.info("Queue processing RESUMED");
    }

    public boolean isPaused() {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember("queue:paused", "true"));
    }
}
