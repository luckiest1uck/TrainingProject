package com.example.trainingproject.user.exception;

public final class DeliveryAddressNotFoundException extends UserException {

    public DeliveryAddressNotFoundException() {
        super("Delivery address not found.");
    }
}
