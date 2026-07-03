package com.example.trainingproject.payment.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.model.checkout.Session;
import com.example.trainingproject.cart.api.CartCheckoutApi;
import com.example.trainingproject.order.api.OrderPaymentApi;
import com.example.trainingproject.payment.entity.Payment;
import com.example.trainingproject.payment.entity.PaymentStatus;
import com.example.trainingproject.payment.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unused") // Spring injects this service into payment flows.
public class PaymentConfirmationService {

    private final OrderPaymentApi orderPaymentApi;
    private final PaymentRepository paymentRepository;
    private final CartCheckoutApi cartCheckoutApi;

    @Transactional
    public void confirmPaid(UUID orderId, Session stripeSession, PaymentConfirmationSource source) {
        String eventType = source.eventType();
        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId).orElse(null);
        if (payment == null || payment.getStatus().isTerminal()) {
            String logMessage = "payment.paid.skipped: orderId={}, status={}, source={}";
            String paymentStatus = payment != null ? payment.getStatus().name() : "missing";
            log.info(logMessage, orderId, paymentStatus, eventType);
            return;
        }

        Long stripeAmount = stripeSession.getAmountTotal();
        String stripeCurrency = stripeSession.getCurrency();
        String paymentIntent = stripeSession.getPaymentIntent();
        if (stripeAmount == null
                || stripeCurrency == null
                || paymentIntent == null
                || !stripeAmount.equals(payment.getAmountMinor())
                || !stripeCurrency.equalsIgnoreCase(payment.getCurrency())) {
            String logMessage =
                    "payment.reconciliation_failed: orderId={}, expected={}_{}, stripe={}_{}, paymentIntentPresent={}, source={}";
            boolean paymentIntentPresent = paymentIntent != null;
            log.error(
                    logMessage,
                    orderId,
                    payment.getAmountMinor(),
                    payment.getCurrency(),
                    stripeAmount,
                    stripeCurrency,
                    paymentIntentPresent,
                    eventType);
            markReconciliationFailed(payment, source, eventType);
            return;
        }

        payment.setProviderPaymentIntentId(paymentIntent);
        payment.setRawEventId(source.eventId());
        payment.setLatestEventType(eventType);

        if (!orderPaymentApi.confirmPayment(orderId, source.confirmationReason())) {
            payment.setStatus(PaymentStatus.RECONCILIATION_FAILED);
            paymentRepository.save(payment);
            log.warn("payment.order_transition_failed: orderId={}, source={}", orderId, eventType);
            return;
        }

        payment.setStatus(PaymentStatus.PAID);
        paymentRepository.save(payment);
        orderPaymentApi.assignPaymentIntent(orderId, paymentIntent);
        cartCheckoutApi.deleteCartForUser(payment.getUserId());
        String logMessage = "payment.confirmed: orderId={}, paymentIntentId={}, source={}";
        log.info(logMessage, orderId, paymentIntent, eventType);
    }

    private void markReconciliationFailed(Payment payment, PaymentConfirmationSource source, String eventType) {
        payment.setStatus(PaymentStatus.RECONCILIATION_FAILED);
        payment.setRawEventId(source.eventId());
        payment.setLatestEventType(eventType);
        paymentRepository.save(payment);
    }
}
