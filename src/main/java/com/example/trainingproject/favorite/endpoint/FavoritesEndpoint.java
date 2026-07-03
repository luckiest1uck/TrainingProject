package com.example.trainingproject.favorite.endpoint;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trainingproject.common.http.ApiPaths;
import com.example.trainingproject.favorite.service.FavoriteService;
import com.example.trainingproject.openapi.dto.ListOfFavoriteProducts;
import com.example.trainingproject.openapi.dto.ListOfFavoriteProductsDto;
import com.example.trainingproject.openapi.favorite.api.FavoriteProductsApi;
import com.example.trainingproject.security.api.CurrentUserProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(FavoritesEndpoint.FAVORITES_URL)
public class FavoritesEndpoint implements FavoriteProductsApi {

    public static final String FAVORITES_URL = ApiPaths.FAVORITES;

    private final CurrentUserProvider currentUserProvider;
    private final FavoriteService favoriteService;

    @Override
    @PostMapping
    public ResponseEntity<ListOfFavoriteProductsDto> addListOfFavoriteProducts(
            @Valid @RequestBody final ListOfFavoriteProducts request) {
        var userId = currentUserProvider.getUserId();
        var response = favoriteService.add(request, userId);
        log.debug("favourites.added: count={}", request.getProductIds().size());
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping
    public ResponseEntity<ListOfFavoriteProductsDto> getListOfFavoriteProducts() {
        var userId = currentUserProvider.getUserId();
        var response = favoriteService.getEnrichedFavoriteList(userId);
        String logMessage = "favourites.retrieved: count={}, userId={}";
        log.debug(logMessage, response.getProducts().size(), userId);
        return ResponseEntity.ok(response);
    }

    @Override
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeProductFromFavorite(@PathVariable final UUID productId) {
        var userId = currentUserProvider.getUserId();
        favoriteService.delete(productId, userId);
        log.debug("favourites.removed: productId={}", productId);
        return ResponseEntity.ok().build();
    }
}
