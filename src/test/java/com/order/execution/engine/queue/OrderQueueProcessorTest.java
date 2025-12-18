package com.order.execution.engine.queue;

import com.order.execution.engine.model.OrderStatus;
import com.order.execution.engine.model.dto.CreateOrderRequest;
import com.order.execution.engine.model.entity.Order;
import com.order.execution.engine.repository.jpa.OrderRepository;
import com.order.execution.engine.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Duration;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/orderengine",
        "spring.datasource.username=root",
        "spring.datasource.password=root",   // match your docker-compose
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.redis.host=localhost",
        "spring.redis.port=6379"
})
public class OrderQueueProcessorTest {

    @Autowired
    private OrderRepository orderRepo;

    @Autowired
    private RedisOrderQueue queue;

    @Autowired
    private OrderService orderService;

    @Test
    void shouldProcessQueueEndToEnd() {
        String orderId = orderService.submitOrder(new CreateOrderRequest(
                "SOL", "USDC", BigDecimal.valueOf(0.1), BigDecimal.valueOf(0.01)
        ));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Order order = orderRepo.findByOrderId(orderId).orElse(null);
            assertNotNull(order);
            assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        });
    }

}
