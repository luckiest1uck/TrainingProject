package com.example.trainingproject.payment.service.webhook;

import static com.example.trainingproject.payment.service.webhook.StripeWebhookEventObjects.extractOrderId;
import static com.example.trainingproject.payment.service.webhook.StripeWebhookEventObjects.requireSession;
import static com.example.trainingproject.payment.service.webhook.StripeWebhookEventType.CHECKOUT_SESSION_EXPIRED;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.stripe.model.Event;
import com.example.trainingproject.order.api.OrderPaymentApi;
import com.example.trainingproject.payment.entity.Payment;
import com.example.trainingproject.payment.entity.PaymentStatus;
import com.example.trainingproject.payment.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
class CheckoutSessionExpiredWebhookHandler implements StripeWebhookEventHandler {

    private final OrderPaymentApi orderPaymentApi;
    private final PaymentRepository paymentRepository;

    @Override
    public boolean supports(Event event) {
        return CHECKOUT_SESSION_EXPIRED.matches(event.getType());
    }

    @Override
    public void handle(Event event) {
        UUID orderId = extractOrderId(requireSession(event));

        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId).orElse(null);
        if (payment == null || payment.getStatus().isTerminal()) {
            String logMessage = "payment.expired.skipped: orderId={}, status={}";
            log.info(logMessage, orderId, paymentStatusOrMissing(payment));
            return;
        }

        payment.setStatus(PaymentStatus.EXPIRED);
        paymentRepository.save(payment);

        if (!orderPaymentApi.expirePayment(orderId, "Stripe session expired")) {
            log.warn("order.expire.transition_failed: orderId={}", orderId);
        }
    }

    private static String paymentStatusOrMissing(Payment payment) {
        return payment != null ? payment.getStatus().name() : "missing";
    }
}
