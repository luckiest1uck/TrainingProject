package com.example.trainingproject.payment.converter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.trainingproject.cart.api.dto.CartItemSnapshot;
import com.example.trainingproject.order.api.OrderSnapshot;
import com.example.trainingproject.payment.config.StripeProperties;
import com.stripe.param.checkout.SessionCreateParams;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@SuppressWarnings("unused") // Spring injects this converter into checkout services.
public class StripeSessionLineItemListConverter {

    private final StripeProperties stripeProperties;

    public List<SessionCreateParams.LineItem> toLineItems(List<CartItemSnapshot> shoppingCartItems) {
        return shoppingCartItems.stream().map(this::toLineItem).toList();
    }

    public SessionCreateParams.LineItem toLineItem(CartItemSnapshot shoppingCartItem) {
        return SessionCreateParams.LineItem.builder()
                .setQuantity((long) shoppingCartItem.productQuantity())
                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(stripeProperties.currency())
                        .setUnitAmount(
                                toStripeUnitAmount(shoppingCartItem.product().price()))
                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                .setName(shoppingCartItem.product().name())
                                .build())
                        .build())
                .build();
    }

    public SessionCreateParams.LineItem toLineItem(OrderSnapshot.OrderItemSnapshot item) {
        return SessionCreateParams.LineItem.builder()
                .setQuantity((long) item.productsQuantity())
                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(stripeProperties.currency())
                        .setUnitAmount(toStripeUnitAmount(item.productPrice()))
                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                .setName(item.productName())
                                .build())
                        .build())
                .build();
    }

    private Long toStripeUnitAmount(final BigDecimal price) {
        return price.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();
    }
}
