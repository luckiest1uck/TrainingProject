package com.example.trainingproject.order.exception;

public final class OrderNotFoundException extends OrderException {

    public OrderNotFoundException() {
        super("Order not found.");
    }

    public OrderNotFoundException(Object orderId) {
        super("Order not found: " + orderId);
    }
}
