package com.order.execution.engine.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_history")
@Setter
@Getter
public class OrderHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String orderId;
    private String errorReason;
    private LocalDateTime failedAt;

    public OrderHistory(String orderId, String errorReason, LocalDateTime failedAt) {
        this.orderId = orderId;
        this.errorReason = errorReason;
        this.failedAt = failedAt;
    }
}
