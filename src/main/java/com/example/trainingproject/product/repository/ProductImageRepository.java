package com.example.trainingproject.product.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.trainingproject.product.entity.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    List<ProductImage> findByProductIdOrderByPositionAscIdAsc(UUID productId);

    List<ProductImage> findByProductIdInOrderByProductIdAscPositionAscIdAsc(List<UUID> productIds);
}
