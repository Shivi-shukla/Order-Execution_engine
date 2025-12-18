package com.order.execution.engine.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component("redisOrderQueueQueue")
@Slf4j
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

    public void process(String s) {
    }

    public void enqueueWithDelay(String orderId, long delay) {
    }
}
