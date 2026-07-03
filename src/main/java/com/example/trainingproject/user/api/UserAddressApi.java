package com.example.trainingproject.user.api;

import java.util.UUID;

public interface UserAddressApi {

    UserAddressSnapshot getDeliveryAddress(UUID userId, UUID deliveryAddressId);
}
