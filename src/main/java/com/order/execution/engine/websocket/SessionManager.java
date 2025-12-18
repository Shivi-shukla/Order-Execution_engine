package com.order.execution.engine.websocket;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionManager {

    private final Map<String, String> orderToSession = new ConcurrentHashMap<>();

    public void register(String orderId, String sessionId) {
        orderToSession.put(orderId, sessionId);
    }

    public String getSession(String orderId) {
        return orderToSession.get(orderId);
    }

}
