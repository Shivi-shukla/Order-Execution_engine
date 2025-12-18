package com.order.execution.engine.service;

import com.order.execution.engine.model.OrderStatus;
import com.order.execution.engine.websocket.StatusBroadcaster;
import com.order.execution.engine.websocket.StatusMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebSocketService {

    private final StatusBroadcaster broadcaster;

    // Track connected sessions per order for broadcasting
    private final Map<String, Integer> orderSessionCount = new ConcurrentHashMap<>();

    /**
     * Register WebSocket client for order updates
     */
    public void registerClient(String orderId, String sessionId) {
        orderSessionCount.merge(orderId, 1, Integer::sum);
        log.debug("Client registered for order: {} (total: {})", orderId, orderSessionCount.get(orderId));
    }

    /**
     * Unregister WebSocket client
     */
    public void unregisterClient(String orderId, String sessionId) {
        orderSessionCount.compute(orderId, (key, count) ->
                count == null || count <= 1 ? null : count - 1);

        if (!hasActiveClients(orderId)) {
            log.debug("No more clients for order: {}", orderId);
        }
    }

    /**
     * Broadcast status update to ALL connected clients for order
     */
    public void broadcastStatus(String orderId, OrderStatus status) {
        broadcastStatus(orderId, status, null);
    }

    /**
     * Broadcast status with transaction hash
     */
    public void broadcastStatus(String orderId, OrderStatus status, String txHash) {
        if (hasActiveClients(orderId)) {
            StatusMessage message = new StatusMessage(orderId, status, txHash);
            broadcaster.send(orderId, status, txHash);
            log.debug("Broadcast {}: {} (tx: {}) to {} clients", orderId, status, txHash, orderSessionCount.get(orderId));
        }
    }

    /**
     * Send heartbeat to keep connection alive
     */
    public void sendHeartbeat(String orderId) {
        if (hasActiveClients(orderId)) {
            StatusMessage heartbeat = new StatusMessage(orderId, OrderStatus.PENDING, "heartbeat");
            broadcaster.send(orderId, OrderStatus.PENDING, "heartbeat");
        }
    }

    /**
     * Check if order has active WebSocket clients
     */
    public boolean hasActiveClients(String orderId) {
        Integer count = orderSessionCount.get(orderId);
        return count != null && count > 0;
    }

    /**
     * Get number of connected clients per order
     */
    public Map<String, Integer> getConnectedClients() {
        return new java.util.HashMap<>(orderSessionCount);
    }

    /**
     * Force disconnect all clients for order (emergency)
     */
    public void disconnectAll(String orderId) {
        orderSessionCount.remove(orderId);
        log.warn("Force disconnected all clients for order: {}", orderId);
    }

    /**
     * Cleanup stale sessions (older than 5 minutes)
     */
    public void cleanupStaleSessions() {
        orderSessionCount.entrySet().removeIf(entry ->
                !hasActiveClients(entry.getKey()) // Already cleaned
        );
        log.debug("Cleaned {} stale WebSocket sessions", orderSessionCount.size());
    }

}
