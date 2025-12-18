package com.order.execution.engine.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class OrderWebSocketHandler {

    @Autowired
    private SessionManager sessionManager;

    @MessageMapping("/order/{orderId}")
    public void subscribeToOrder(@DestinationVariable String orderId, StompHeaderAccessor headers) {
        String sessionId = headers.getSessionId();
        sessionManager.register(orderId, sessionId);
    }

}
