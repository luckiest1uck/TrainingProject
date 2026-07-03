package com.example.trainingproject.order.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trainingproject.openapi.dto.OrderEvent;
import com.example.trainingproject.openapi.dto.OrderStatus;
import com.example.trainingproject.order.api.OrderPaymentApi;
import com.example.trainingproject.order.api.OrderSnapshot;
import com.example.trainingproject.order.converter.OrderDtoConverter;
import com.example.trainingproject.order.entity.Order;
import com.example.trainingproject.order.exception.InvalidOrderStateTransitionException;
import com.example.trainingproject.order.exception.OrderNotFoundException;
import com.example.trainingproject.order.repository.OrderRepository;
import com.example.trainingproject.order.service.lifecycle.OrderStatusTransitioner;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderPaymentService implements OrderPaymentApi {

    private final OrderRepository orderRepository;
    private final OrderDtoConverter orderDtoConverter;
    private final OrderStatusTransitioner orderStatusTransitioner;

    @Override
    @Transactional(readOnly = true)
    public OrderSnapshot getSnapshot(UUID orderId) {
        Order order = findById(orderId);
        return orderDtoConverter.toSnapshot(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderSnapshot getSnapshotWithItems(UUID orderId) {
        Order order = orderRepository.findByIdWithItems(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        return orderDtoConverter.toSnapshot(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderSnapshot> findByStripePaymentIntentId(String paymentIntentId) {
        return orderRepository.findByStripePaymentIntentId(paymentIntentId).map(orderDtoConverter::toSnapshot);
    }

    @Override
    @Transactional
    public boolean confirmPayment(UUID orderId, String reason) {
        return transitionPaymentOrder(orderId, OrderEvent.PENDING_PAYMENT_CONFIRMED, OrderStatus.PAID, reason);
    }

    @Override
    @Transactional
    public boolean expirePayment(UUID orderId, String reason) {
        return transitionPaymentOrder(orderId, OrderEvent.PAYMENT_EXPIRED_EVENT, OrderStatus.PAYMENT_EXPIRED, reason);
    }

    @Override
    @Transactional
    public boolean failPayment(UUID orderId, String reason) {
        return transitionPaymentOrder(orderId, OrderEvent.PAYMENT_FAILED_EVENT, OrderStatus.PAYMENT_FAILED, reason);
    }

    @Override
    @Transactional
    public void assignPaymentIntent(UUID orderId, String stripePaymentIntentId) {
        Order order = findById(orderId);
        order.setStripePaymentIntentId(stripePaymentIntentId);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public boolean confirmRefund(UUID orderId, String reason) {
        return transitionPaymentOrder(orderId, OrderEvent.REFUND_CONFIRMED, OrderStatus.REFUNDED, reason);
    }

    private Order findById(UUID orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private boolean transitionPaymentOrder(UUID orderId, OrderEvent event, OrderStatus targetStatus, String reason) {
        try {
            orderStatusTransitioner.transition(orderId, event, null, reason);
            return true;
        } catch (InvalidOrderStateTransitionException _) {
            return orderRepository
                    .findById(orderId)
                    .map(Order::getStatus)
                    .filter(targetStatus::equals)
                    .isPresent();
        }
    }
}
