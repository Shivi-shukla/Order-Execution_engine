package com.order.execution.engine.controller;

import com.order.execution.engine.model.OrderStatus;
import com.order.execution.engine.model.SwapResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class OrderWebSocketController {

    @Autowired private SimpMessagingTemplate messagingTemplate;

    // Broadcast status updates to specific order channel
    public void sendStatus(String orderId, OrderStatus status) {
        messagingTemplate.convertAndSend("/topic/orders/" + orderId, status);
    }

    public void sendStatus(String orderId, OrderStatus status, SwapResult result) {
        Map<String, Object> message = Map.of(
                "status", status,
                "orderId", orderId,
                "txHash", result.txHash(),
                "executedPrice", result.executedPrice(),
                "dex", result.dexName()
        );
        messagingTemplate.convertAndSend("/topic/orders/" + orderId, message);
    }

}
