package com.order.execution.engine.controller;

import com.order.execution.engine.model.dto.QueueHealth;
import com.order.execution.engine.model.dto.QueueMetrics;
import com.order.execution.engine.queue.QueueManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/queue")
public class QueueController {

    @Autowired
    private QueueManager queueManager;

    @GetMapping("/metrics")
    public QueueMetrics getMetrics() {
        return queueManager.getQueueMetrics();
    }

    @GetMapping("/health")
    public ResponseEntity<QueueHealth> health() {
        QueueMetrics metrics = queueManager.getQueueMetrics();
        return metrics.health().healthy()
                ? ResponseEntity.ok(metrics.health())
                : ResponseEntity.status(HttpStatus.OK).body(metrics.health());
    }

}
