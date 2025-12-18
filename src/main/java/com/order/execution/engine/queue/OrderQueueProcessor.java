package com.order.execution.engine.queue;

import com.order.execution.engine.model.OrderStatus;
import com.order.execution.engine.model.entity.Order;
import com.order.execution.engine.repository.jpa.OrderRepository;
import com.order.execution.engine.repository.redis.ActiveOrderRepository;
import com.order.execution.engine.repository.redis.RedisOrderQueue;
import com.order.execution.engine.service.DexRouterService;
import com.order.execution.engine.service.MockDexService;
import com.order.execution.engine.websocket.StatusBroadcaster;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderQueueProcessor {

    @Autowired
    private RedisOrderQueue queue;
    @Autowired
    private DexRouterService dexRouter;
    @Autowired
    private MockDexService dexService;
    @Autowired
    private StatusBroadcaster broadcaster;
    @Autowired
    private OrderRepository orderRepo;
    @Autowired
    private RetryService retryService;
    @Autowired
    private ActiveOrderRepository activeOrderRepo;

    @Scheduled(fixedDelay = 100)
    @Async("orderExecutor")
    public void processQueue() {
        String orderId = queue.dequeue();
        if (orderId == null) return;

        try {
            processOrder(orderId);
        } catch (Exception e) {
            retryService.retry(orderId, e);
        }
    }

    private void processOrder(String orderId) {
        // Verify still active
        if (!activeOrderRepo.isOrderActive(orderId)) {
            log.warn("Skipping expired order: {}", orderId);
            return;
        }

        Order order = orderRepo.findByOrderId(orderId).orElseThrow();

        // Update statuses via ActiveOrderRepository (extends TTL)
        activeOrderRepo.updateOrderStatus(orderId, OrderStatus.ROUTING);
        broadcaster.send(orderId, OrderStatus.ROUTING);

        // ... DEX routing ...

        activeOrderRepo.updateOrderStatus(orderId, OrderStatus.BUILDING);
        broadcaster.send(orderId, OrderStatus.BUILDING);

        // ... execute swap ...
        String txHash = String.valueOf(dexService.executeSwap(orderId,order));

        // Complete processing
        order.setStatus(OrderStatus.CONFIRMED);
        order.setTxHash(txHash);
        orderRepo.save(order);

        activeOrderRepo.completeOrder(orderId, txHash);  // Redis cleanup
        broadcaster.send(orderId, OrderStatus.CONFIRMED, txHash);
    }

}
