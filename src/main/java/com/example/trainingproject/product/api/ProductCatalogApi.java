package com.example.trainingproject.product.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.example.trainingproject.product.api.dto.ProductSnapshot;

/**
 * Stable contract for product catalog operations exposed to other modules. Other modules should depend on this
 * interface, not on concrete ProductService.
 */
public interface ProductCatalogApi {

    List<ProductSnapshot> getProductsByIds(List<UUID> ids);

    boolean existsById(UUID productId);

    Set<UUID> findExistingProductIds(Set<UUID> productIds);
}
