package com.example.trainingproject.order.exception;

public final class OrderDeliveryAddressNotFoundException extends OrderException {

    public OrderDeliveryAddressNotFoundException() {
        super("Delivery address not found.");
    }
}
