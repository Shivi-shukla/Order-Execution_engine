package com.order.execution.engine.model.entity;

import com.order.execution.engine.model.OrderStatus;
import com.order.execution.engine.model.dto.CreateOrderRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String tokenIn;

    @Column(nullable = false)
    private String tokenOut;

    @Column(nullable = false, precision = 38, scale = 8)
    private BigDecimal amountIn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    private String txHash;
    private String error;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors, getters, setters
    public Order() {}

    public Order(String orderId, CreateOrderRequest req) {
        this.orderId = orderId;
        this.tokenIn = req.tokenIn();
        this.tokenOut = req.tokenOut();
        this.amountIn = req.amountIn();
        this.status = OrderStatus.PENDING;
    }
}