package com.order.execution.engine.repository.jpa;

import com.order.execution.engine.model.OrderStatus;
import com.order.execution.engine.model.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    Optional<Order> findByOrderId(String orderId);
    List<Order> findByStatus(OrderStatus status);

}
