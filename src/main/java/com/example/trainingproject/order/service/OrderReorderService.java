package com.example.trainingproject.order.service;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trainingproject.cart.api.CartCheckoutApi;
import com.example.trainingproject.cart.api.dto.AddCartItemRequest;
import com.example.trainingproject.openapi.dto.ReorderResponseDto;
import com.example.trainingproject.openapi.dto.UnavailableItemDto;
import com.example.trainingproject.order.entity.Order;
import com.example.trainingproject.order.entity.OrderItem;
import com.example.trainingproject.order.exception.OrderAccessDeniedException;
import com.example.trainingproject.order.exception.OrderNotFoundException;
import com.example.trainingproject.order.repository.OrderRepository;
import com.example.trainingproject.product.api.ProductCatalogApi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderReorderService {

    private final OrderRepository orderRepository;
    private final ProductCatalogApi productCatalogApi;
    private final CartCheckoutApi cartCheckoutApi;

    @Transactional
    public ReorderResponseDto reorder(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getUserId().equals(userId)) {
            throw new OrderAccessDeniedException();
        }

        Set<UUID> requestedProductIds =
                order.getItems().stream().map(OrderItem::getProductId).collect(Collectors.toSet());
        Set<UUID> existingProductIds = productCatalogApi.findExistingProductIds(requestedProductIds);

        Set<AddCartItemRequest> itemsToAdd = new LinkedHashSet<>();
        List<UnavailableItemDto> unavailable = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            if (existingProductIds.contains(item.getProductId())) {
                itemsToAdd.add(new AddCartItemRequest(item.getProductId(), item.getProductsQuantity()));
            } else {
                unavailable.add(new UnavailableItemDto()
                        .productName(item.getProductName())
                        .reason("Product no longer available"));
            }
        }

        UUID cartId = null;
        if (!itemsToAdd.isEmpty()) {
            var cart = cartCheckoutApi.addItems(userId, itemsToAdd);
            cartId = cart.id();
        }

        log.info("order.reorder: orderId={}, added={}, unavailable={}", orderId, itemsToAdd.size(), unavailable.size());

        return new ReorderResponseDto()
                .cartId(cartId)
                .addedItems(itemsToAdd.size())
                .unavailableItems(unavailable);
    }
}
