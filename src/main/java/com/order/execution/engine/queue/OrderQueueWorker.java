package com.order.execution.engine.queue;

import com.order.execution.engine.model.OrderStatus;
import com.order.execution.engine.model.SwapResult;
import com.order.execution.engine.model.dto.DexQuote;
import com.order.execution.engine.model.entity.Order;
import com.order.execution.engine.repository.jpa.OrderRepository;
import com.order.execution.engine.repository.redis.ActiveOrderRepository;
import com.order.execution.engine.service.DexRouterService;
import com.order.execution.engine.websocket.StatusBroadcaster;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderQueueWorker {

    @Autowired
    private DexRouterService dexRouter;
    @Autowired
    private RedisOrderQueue queue;
    @Autowired
    private OrderRepository orderRepo;
    @Autowired
    private ActiveOrderRepository activeOrderRepo;
    @Autowired
    private StatusBroadcaster broadcaster;

    @PostConstruct  // ✅ Runs automatically on startup
    public void startWorker() {
        queue.process("market-orders");
    }

    private void executeOrder(String orderId) {
        try {
            Order order = activeOrderRepo.getActiveOrder(orderId);  // ✅ Direct Order
            if (order == null) return;

            broadcaster.send(orderId, OrderStatus.ROUTING);

            // ✅ Synchronous DexQuote calls
            DexQuote raydium = dexRouter.getRaydiumQuote(order);
            DexQuote meteora = dexRouter.getMeteoraQuote(order);
            DexQuote best = selectBestQuote(raydium, meteora);  // ✅ Direct DexQuote

            broadcaster.send(orderId, OrderStatus.BUILDING);

            SwapResult result = dexRouter.execute(best.dexName(), order);  // ✅ Direct Order

            order.setStatus(OrderStatus.CONFIRMED);
            order.setTxHash(result.txHash());
            orderRepo.save(order);

            broadcaster.send(orderId, OrderStatus.CONFIRMED, String.valueOf(result));

        } catch (Exception e) {
            handleRetry(orderId, e);
        }
    }

    private DexQuote selectBestQuote(DexQuote r, DexQuote m) {  // ✅ Fixed params
        return r.netPrice().compareTo(m.netPrice()) > 0 ? r : m;
    }

    private void handleRetry(String orderId, Exception e) {
        int attempts = activeOrderRepo.getRetryCount(orderId);
        if (attempts < 3) {
            activeOrderRepo.incrementRetry(orderId);
            long delay = (long) Math.pow(2, attempts) * 1000; // 1s, 2s, 4s
            queue.enqueueWithDelay(orderId, delay);
        } else {
            broadcaster.send(orderId, OrderStatus.FAILED, e.getMessage());
        }
    }


}
