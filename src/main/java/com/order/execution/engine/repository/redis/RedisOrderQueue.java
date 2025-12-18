package com.order.execution.engine.repository.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisOrderQueue {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    private static final String QUEUE_KEY = "order:queue";

    public void enqueue(String orderId) {
        redisTemplate.opsForList().rightPush(QUEUE_KEY, orderId);
    }

    public String dequeue() {
        return redisTemplate.opsForList().leftPop(QUEUE_KEY);
    }

}
