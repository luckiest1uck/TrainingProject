package com.example.trainingproject.payment.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trainingproject.openapi.dto.CheckoutStatusDto;
import com.example.trainingproject.openapi.dto.OrderStatus;
import com.example.trainingproject.order.api.OrderPaymentApi;
import com.example.trainingproject.order.api.OrderSnapshot;
import com.example.trainingproject.payment.entity.Payment;
import com.example.trainingproject.payment.exception.PaymentAccessDeniedException;
import com.example.trainingproject.payment.repository.PaymentRepository;
import com.example.trainingproject.security.api.CurrentUserProvider;

import lombok.RequiredArgsConstructor;

/**
 * Status polling for the success page.
 *
 * <p>Primary path: the Stripe webhook updates payment/order status before the success page polls. This is how real
 * payment systems work.
 *
 * <p>Fallback path: if the webhook hasn't arrived yet (common in local dev without Stripe CLI), this service calls
 * Session.retrieve() directly and updates the status. Real payment systems always have a reconciliation fallback —
 * never rely on a single delivery mechanism for money.
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class PaymentStatusService {

    private final OrderPaymentApi orderPaymentApi;
    private final PaymentRepository paymentRepository;
    private final CurrentUserProvider currentUserProvider;
    private final PaymentReconciliationService paymentReconciliationService;

    public CheckoutStatusDto getStatus(UUID orderId) {
        OrderSnapshot order = orderPaymentApi.getSnapshot(orderId);
        var currentUser = currentUserProvider.get();
        requireOwnedOrder(order, currentUser.id());

        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);

        // Fallback: if webhook hasn't arrived yet, check Stripe directly.
        if (payment != null && !payment.getStatus().isTerminal() && payment.getProviderSessionId() != null) {
            paymentReconciliationService.trySyncPaidStatus(payment);
            // Re-read after potential update
            order = orderPaymentApi.getSnapshot(orderId);
            payment = paymentRepository.findByOrderId(orderId).orElse(payment);
        }

        CheckoutStatusDto dto = new CheckoutStatusDto()
                .orderId(order.id())
                .orderStatus(OrderStatus.valueOf(order.status().name()));

        if (payment != null) {
            String status = payment.getStatus().name();
            dto.paymentStatus(CheckoutStatusDto.PaymentStatusEnum.fromValue(status));
        }

        return dto;
    }

    private static void requireOwnedOrder(OrderSnapshot order, UUID userId) {
        if (!order.userId().equals(userId)) {
            throw new PaymentAccessDeniedException();
        }
    }
}
