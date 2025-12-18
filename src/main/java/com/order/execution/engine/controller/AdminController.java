package com.order.execution.engine.controller;

import com.order.execution.engine.service.OrderProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@Slf4j
public class AdminController {

    @Autowired
    private OrderProcessor processor;

    @PostMapping("/start-processor")
    public String startProcessor() {
        processor.processOrdersAsync();
        return "Order processor STARTED!";
    }

}
