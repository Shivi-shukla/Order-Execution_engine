package com.order.execution.engine.exception;

public class OrderExecutionException extends RuntimeException {

    public OrderExecutionException(String message) {
        super(message);
    }

    public OrderExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

}
