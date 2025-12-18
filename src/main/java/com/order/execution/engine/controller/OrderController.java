package com.order.execution.engine.controller;

import com.order.execution.engine.model.dto.CreateOrderRequest;
import com.order.execution.engine.model.entity.Order;
import com.order.execution.engine.repository.jpa.OrderRepository;
import com.order.execution.engine.repository.redis.ActiveOrderRepository;
import com.order.execution.engine.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private ActiveOrderRepository activeOrders;
    @Autowired
    private OrderRepository orderRepo;

    @PostMapping("/execute")
    public ResponseEntity<Map<String, String>> executeOrder(@RequestBody CreateOrderRequest request, HttpServletRequest httpRequest) {
        String orderId = orderService.submitOrder(request);
        return ResponseEntity.ok(Map.of("orderId", orderId, "wsEndpoint", "ws://localhost:8080/ws"));
    }

    @GetMapping("/{orderId}/status")
    public ResponseEntity<?> getOrderStatus(@PathVariable String orderId) {
        // Check Redis (active orders) first
        Order active = activeOrders.getActiveOrder(orderId);
        if (active != null) {
            return ResponseEntity.ok(Map.of(
                    "status", active.getStatus().name(),
                    "source", "redis-active"
            ));
        }

        // Fallback to PostgreSQL (completed orders)
        Optional<Order> completed = orderRepo.findById(orderId);
        if (completed.isPresent()) {
            Order order = completed.get();
            return ResponseEntity.ok(Map.of(
                    "status", order.getStatus().name(),
                    "txHash", order.getTxHash() != null ? order.getTxHash() : "pending",
                    "source", "mysql"
            ));
        }

        return ResponseEntity.ok(Map.of("status", "NOT_FOUND", "source", "none"));
    }
}
