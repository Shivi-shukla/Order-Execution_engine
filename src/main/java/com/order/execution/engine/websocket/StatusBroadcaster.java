package com.order.execution.engine.websocket;

import com.order.execution.engine.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StatusBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;
    private final SessionManager sessionManager;

    public void send(String orderId, OrderStatus status) {
        messagingTemplate.convertAndSend("/topic/orders/" + orderId, status);
    }

    public void send(String orderId, OrderStatus status, String txHash) {
        StatusMessage message = new StatusMessage(orderId, status, txHash);
        String sessionId = sessionManager.getSession(orderId);
        if (sessionId != null) {
            messagingTemplate.convertAndSendToUser(sessionId, "/queue/order/" + orderId, message);
        }
    }

}
