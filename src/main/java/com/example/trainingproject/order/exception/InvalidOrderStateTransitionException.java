package com.example.trainingproject.order.exception;

import com.example.trainingproject.openapi.dto.OrderEvent;
import com.example.trainingproject.openapi.dto.OrderStatus;

public final class InvalidOrderStateTransitionException extends OrderException {

    public InvalidOrderStateTransitionException(OrderStatus currentStatus, OrderEvent event) {
        super(String.format("Cannot apply event '%s' to order in status '%s'.", event, currentStatus));
    }
}
