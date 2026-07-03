package com.example.trainingproject.review.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.trainingproject.review.entity.ProductReviewLike;

public interface ProductReviewLikeRepository extends JpaRepository<ProductReviewLike, UUID> {

    Optional<ProductReviewLike> findByUserIdAndProductReviewId(UUID userId, UUID reviewId);
}
