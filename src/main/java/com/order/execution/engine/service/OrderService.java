package com.order.execution.engine.service;

import com.order.execution.engine.model.OrderStatus;
import com.order.execution.engine.model.dto.CreateOrderRequest;
import com.order.execution.engine.model.entity.Order;
import com.order.execution.engine.repository.jpa.OrderRepository;
import com.order.execution.engine.repository.redis.ActiveOrderRepository;
import com.order.execution.engine.repository.redis.RedisOrderQueue;
import com.order.execution.engine.websocket.StatusBroadcaster;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class OrderService {

    @Autowired
    private DexRouterService dexRouter;
    @Autowired
    private RedisOrderQueue queue;
    @Autowired
    private OrderRepository orderRepo;
    @Autowired
    private StatusBroadcaster broadcaster;
    @Autowired
    private ActiveOrderRepository activeOrderRepo;
    @Autowired
    @Qualifier("orderExecutor")
    private TaskExecutor executor;

    @Transactional
    public String submitOrder(CreateOrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        Order order = new Order(orderId, request);
        order.setStatus(OrderStatus.PENDING);

        orderRepo.save(order);
        activeOrderRepo.addActiveOrder(orderId, order);

        // 🔥 Fixed executor call
        executor.execute(() -> processOrder(orderId));
        broadcaster.send(orderId, OrderStatus.PENDING);
        return orderId;
    }

    @Transactional
    public void processOrder(String orderId) {
        Order order = orderRepo.findById(orderId).orElse(null);
        if (order == null) return;

        try {
            // PENDING → ROUTING
            updateStatus(orderId, OrderStatus.ROUTING);
            Thread.sleep(2000 + (int)(Math.random() * 3000)); // 2-5s

            // ROUTING → BUILDING
            updateStatus(orderId, OrderStatus.BUILDING);
            Thread.sleep(3000 + (int)(Math.random() * 5000)); // 3-8s

            // BUILDING → CONFIRMED
            updateStatus(orderId, OrderStatus.CONFIRMED);

        } catch (Exception e) {
            updateStatus(orderId, OrderStatus.FAILED);
        }
    }

    private void updateStatus(String orderId, OrderStatus status) {
        Order order = orderRepo.findById(orderId).orElseThrow();
        order.setStatus(status);
        orderRepo.save(order);
        broadcaster.send(orderId, status);
    }

}
