package com.example.trainingproject.cart.api.dto;

import java.util.UUID;

import com.example.trainingproject.product.api.dto.ProductSnapshot;

public record CartItemSnapshot(UUID id, ProductSnapshot product, int productQuantity) {}
