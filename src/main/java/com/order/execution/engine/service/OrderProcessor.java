package com.order.execution.engine.service;

import com.order.execution.engine.model.OrderStatus;
import com.order.execution.engine.model.entity.Order;
import com.order.execution.engine.repository.jpa.OrderRepository;
import com.order.execution.engine.repository.redis.RedisOrderQueue;
import com.order.execution.engine.websocket.StatusBroadcaster;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderProcessor {

    @Autowired
    private RedisOrderQueue queue;
    @Autowired
    private OrderRepository orderRepo;
    @Autowired
    private StatusBroadcaster broadcaster;
    @Autowired
    private DexRouterService dexRouter;

    @Async("orderExecutor")
    public void processOrdersAsync() {
        log.info("🚀 Order processor STARTED"); // Debug log
        while (true) {
            try {
                String orderId = queue.dequeue();
                if (orderId != null) {
                    processOrder(orderId);
                }
                Thread.sleep(100);
            } catch (Exception e) {
                log.error("Order processing error", e);
                try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
    }

    @Transactional
    public void processOrder(String orderId) {
        // 1. Load order
        Order order = orderRepo.findById(orderId).orElse(null);
        if (order == null) return;

        try {
            // 2. Update status step-by-step
            updateStatus(orderId, OrderStatus.ROUTING);
            simulateRouting(order); // Replace with real DEX routing

            updateStatus(orderId, OrderStatus.BUILDING);
            simulateBuilding(order); // Replace with tx building

            updateStatus(orderId, OrderStatus.CONFIRMED);
            simulateConfirm(order); // Replace with real confirmation

        } catch (Exception e) {
            updateStatus(orderId, OrderStatus.FAILED);
            log.error("Order {} failed", orderId, e);
        }
    }

    private void updateStatus(String orderId, OrderStatus status) {
        Order order = orderRepo.findById(orderId).orElseThrow();
        order.setStatus(status);
        orderRepo.save(order);
        broadcaster.send(orderId, status);
        log.info("Order {} → {}", orderId, status);
    }

    // Simulation (replace with real DEX calls)
    private void simulateRouting(Order order) throws InterruptedException {
        Thread.sleep(2000 + (int)(Math.random() * 3000)); // 2-5s
    }
    private void simulateBuilding(Order order) throws InterruptedException {
        Thread.sleep(3000 + (int)(Math.random() * 5000)); // 3-8s
    }
    private void simulateConfirm(Order order) throws InterruptedException {
        Thread.sleep(2000); // 2s
    }

}
