package com.example.trainingproject.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.trainingproject.openapi.dto.OrderEvent;
import com.example.trainingproject.openapi.dto.OrderStatus;
import com.example.trainingproject.order.converter.OrderDtoConverter;
import com.example.trainingproject.order.entity.Order;
import com.example.trainingproject.order.exception.InvalidOrderStateTransitionException;
import com.example.trainingproject.order.repository.OrderRepository;
import com.example.trainingproject.order.service.lifecycle.OrderStatusTransitioner;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderPaymentService")
class OrderPaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    @SuppressWarnings("unused")
    private OrderDtoConverter orderDtoConverter;

    @Mock
    private OrderStatusTransitioner orderStatusTransitioner;

    @InjectMocks
    private OrderPaymentService orderPaymentService;

    @Test
    @DisplayName("confirmPayment is idempotent when order is already paid")
    void confirmPaymentReturnsTrueWhenOrderAlreadyPaid() {
        UUID orderId = UUID.randomUUID();
        when(orderStatusTransitioner.transition(
                        orderId, OrderEvent.PENDING_PAYMENT_CONFIRMED, null, "Stripe payment confirmed"))
                .thenThrow(new InvalidOrderStateTransitionException(
                        OrderStatus.PAID, OrderEvent.PENDING_PAYMENT_CONFIRMED));
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(
                        Order.builder().id(orderId).status(OrderStatus.PAID).build()));

        boolean result = orderPaymentService.confirmPayment(orderId, "Stripe payment confirmed");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("assignPaymentIntent updates the order")
    void assignPaymentIntentUpdatesOrder() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder().id(orderId).build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderPaymentService.assignPaymentIntent(orderId, "pi_test");

        assertThat(order.getStripePaymentIntentId()).isEqualTo("pi_test");
        verify(orderRepository).save(order);
    }
}
