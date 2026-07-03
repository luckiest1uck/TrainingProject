package com.example.trainingproject.security.websocket;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import com.example.trainingproject.security.jwt.blacklist.JwtTokenBlacklist;
import com.example.trainingproject.security.jwt.provider.JwtAccountStatusValidator;
import com.example.trainingproject.security.jwt.resolver.JwtBearerTokenResolver;
import com.example.trainingproject.security.jwt.resolver.JwtTokenClaims;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportChatWebSocketAuthenticationService {

    private final JwtBearerTokenResolver jwtBearerTokenResolver;
    private final JwtTokenClaims jwtTokenClaims;
    private final UserDetailsService userDetailsService;
    private final JwtTokenBlacklist jwtTokenBlacklist;

    public Authentication authenticate(String authorizationHeader) {
        String jwtToken = jwtBearerTokenResolver.extract(authorizationHeader);
        jwtTokenBlacklist.validateNotBlacklisted(jwtToken);

        String userEmail = jwtTokenClaims.extractSupportChatWebSocketTicketEmail(jwtToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
        JwtAccountStatusValidator.requireActive(userDetails);

        log.debug("auth.websocket.support_chat.success");
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
