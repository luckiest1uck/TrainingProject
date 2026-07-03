package com.example.trainingproject.payment.endpoint;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.trainingproject.common.http.ApiPaths;
import com.example.trainingproject.common.util.ClientIpExtractor;
import com.example.trainingproject.openapi.dto.CheckoutResponseDto;
import com.example.trainingproject.openapi.dto.CheckoutStatusDto;
import com.example.trainingproject.openapi.dto.CreateCheckoutRequestDto;
import com.example.trainingproject.payment.service.PaymentStatusService;
import com.example.trainingproject.payment.service.checkout.CheckoutPaymentService;
import com.example.trainingproject.payment.service.webhook.StripeWebhookService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Stripe Hosted Checkout endpoints (test mode only — no real money). */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(ApiPaths.PAYMENT)
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
@SuppressWarnings("unused") // Spring MVC invokes endpoint methods via reflection.
public class PaymentEndpoint implements com.example.trainingproject.openapi.payment.api.PaymentApi {

    private final CheckoutPaymentService checkoutPaymentService;
    private final PaymentStatusService paymentStatusService;
    private final StripeWebhookService stripeWebhookService;
    private final HttpServletRequest httpRequest;
    private final ClientIpExtractor clientIpExtractor;

    @Override
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponseDto> createCheckout(
            @NotNull @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateCheckoutRequestDto request) {
        CheckoutResponseDto response = checkoutPaymentService.checkout(request, idempotencyKey, clientIp());
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/checkout/{orderId}/status")
    public ResponseEntity<CheckoutStatusDto> getCheckoutStatus(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentStatusService.getStatus(orderId));
    }

    @Override
    @PostMapping("/stripe/webhook")
    public ResponseEntity<Void> processStripeWebhook(
            @NotNull @RequestHeader("Stripe-Signature") String stripeSignature, @Valid @RequestBody String body) {
        stripeWebhookService.processWebhook(body, stripeSignature);
        return ResponseEntity.ok().build();
    }

    private String clientIp() {
        return clientIpExtractor.extract(httpRequest);
    }
}
