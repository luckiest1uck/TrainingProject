package com.example.trainingproject.order.api;

import java.util.UUID;

import com.example.trainingproject.cart.api.dto.CartSnapshot;
import com.example.trainingproject.order.api.dto.CheckoutOrderRequest;

public interface OrderCheckoutApi {

    OrderSnapshot createPendingPaymentOrderSnapshot(UUID userId, CheckoutOrderRequest request, CartSnapshot cart);
}
