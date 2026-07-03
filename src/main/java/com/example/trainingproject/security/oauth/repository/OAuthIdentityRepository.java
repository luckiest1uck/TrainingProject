package com.example.trainingproject.security.oauth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.trainingproject.security.oauth.config.OAuthProvider;
import com.example.trainingproject.security.oauth.entity.OAuthIdentityEntity;

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentityEntity, UUID> {

    Optional<OAuthIdentityEntity> findByProviderAndProviderSubject(OAuthProvider provider, String providerSubject);
}
