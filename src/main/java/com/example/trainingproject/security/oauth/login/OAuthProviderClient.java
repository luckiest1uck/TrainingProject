package com.example.trainingproject.security.oauth.login;

import java.net.URI;

import com.example.trainingproject.security.oauth.config.OAuthProvider;
import com.example.trainingproject.security.oauth.dto.OAuthProfile;

public interface OAuthProviderClient {

    OAuthProvider provider();

    URI buildAuthorizationUri(String state);

    OAuthProfile exchangeCode(String authorizationCode);
}
