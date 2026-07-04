package com.example.trainingproject.security.signup.verification;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.trainingproject.common.exception.BadRequestException;
import com.example.trainingproject.common.util.EmailNormalizer;
import com.example.trainingproject.openapi.dto.UserRegistrationRequest;
import com.example.trainingproject.security.service.cache.ExpiringKeyValueStore;
import com.example.trainingproject.security.session.dto.TokenPurpose;
import com.example.trainingproject.security.signup.exception.TimeTokenException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailTokenService {

    private static final String TOKEN_KEY_PREFIX = "email:token:";
    private static final String COOLDOWN_KEY_PREFIX = "email:rate:";
    private static final String TOKEN_HASH_ALGORITHM = "SHA-256";
    private static final int MAX_TOKEN_GENERATION_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ExpiringKeyValueStore temporaryStore;
    private final PasswordEncoder passwordEncoder;
    private final EmailTokenPayloadProtector tokenPayloadProtector;
    private final EmailTokenProperties emailTokenProperties;
    private final TemporaryTokenProperties temporaryTokenProperties;

    public String generateEmailVerificationToken(UserRegistrationRequest request) {
        String email = EmailNormalizer.normalize(request.getEmail());
        Duration ttl = tokenTtl();
        reserveCooldown(email, TokenPurpose.EMAIL_VERIFICATION, ttl);
        try {
            String encodedPassword = passwordEncoder.encode(request.getPassword());
            var registration = new EmailRegistrationPayload(request.getFirstName(), request.getLastName(), email);
            var payload = new EmailVerificationTokenPayload(email, registration, encodedPassword);
            return generate(email, TokenPurpose.EMAIL_VERIFICATION, tokenPayloadProtector.protect(payload), ttl);
        } catch (RuntimeException ex) {
            temporaryStore.remove(cooldownKey(TokenPurpose.EMAIL_VERIFICATION, email));
            throw ex;
        }
    }

    public String generatePasswordResetToken(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        Duration ttl = tokenTtl();
        reserveCooldown(normalizedEmail, TokenPurpose.PASSWORD_RESET, ttl);
        try {
            var payload = new PasswordResetTokenPayload(normalizedEmail);
            return generate(normalizedEmail, TokenPurpose.PASSWORD_RESET, tokenPayloadProtector.protect(payload), ttl);
        } catch (RuntimeException ex) {
            temporaryStore.remove(cooldownKey(TokenPurpose.PASSWORD_RESET, normalizedEmail));
            throw ex;
        }
    }

    public EmailVerificationTokenPayload consumeEmailVerificationToken(String token) {
        return consume(token, TokenPurpose.EMAIL_VERIFICATION, EmailVerificationTokenPayload.class);
    }

    public PasswordResetTokenPayload consumePasswordResetToken(String token) {
        return consume(token, TokenPurpose.PASSWORD_RESET, PasswordResetTokenPayload.class);
    }

    private String generate(String email, TokenPurpose purpose, String protectedPayload, Duration ttl) {
        for (int attempt = 0; attempt < MAX_TOKEN_GENERATION_ATTEMPTS; attempt++) {
            String token = nextToken();
            String tokenKey = tokenKey(purpose, token);

            if (temporaryStore.putIfAbsent(tokenKey, protectedPayload, ttl)) {
                return token;
            }
        }

        throw new IllegalStateException("Failed to allocate unique email token");
    }

    private <T extends EmailTokenPayload> T consume(String token, TokenPurpose purpose, Class<T> payloadType) {
        validateTokenFormat(token);
        T payload = temporaryStore
                .take(tokenKey(purpose, token))
                .map(protectedEntry -> tokenPayloadProtector.unprotect(protectedEntry, payloadType))
                .orElseThrow(() -> new BadRequestException("Incorrect token"));
        temporaryStore.remove(cooldownKey(purpose, payload.email()));
        return payload;
    }

    private void reserveCooldown(String email, TokenPurpose purpose, Duration ttl) {
        OffsetDateTime expiry = OffsetDateTime.now().plus(ttl);
        String key = cooldownKey(purpose, email);
        if (temporaryStore.putIfAbsent(key, expiry.toString(), ttl)) {
            return;
        }

        OffsetDateTime existingExpiry =
                temporaryStore.get(key).map(OffsetDateTime::parse).orElse(expiry);
        throw new TimeTokenException(existingExpiry);
    }

    private String nextToken() {
        int tokenLength = emailTokenProperties.verificationTokenLength();
        byte[] randomBytes = new byte[(int) Math.ceil(tokenLength * 6 / 8.0)];
        RANDOM.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return token.length() > tokenLength ? token.substring(0, tokenLength) : token;
    }

    private void validateTokenFormat(String token) {
        int tokenLength = emailTokenProperties.verificationTokenLength();
        if (token == null || token.length() != tokenLength || !token.chars().allMatch(this::isUrlSafeTokenChar)) {
            throw new BadRequestException("Incorrect token format");
        }
    }

    private boolean isUrlSafeTokenChar(int value) {
        return Character.isLetterOrDigit(value) || value == '-' || value == '_';
    }

    private Duration tokenTtl() {
        return Duration.ofMinutes(temporaryTokenProperties.time().token());
    }

    private static String tokenKey(TokenPurpose purpose, String token) {
        return TOKEN_KEY_PREFIX + purpose.name().toLowerCase(Locale.ROOT) + ":" + hashToken(token);
    }

    private static String hashToken(String token) {
        try {
            byte[] bytes = token.getBytes(StandardCharsets.UTF_8);
            byte[] digest = MessageDigest.getInstance(TOKEN_HASH_ALGORITHM).digest(bytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(TOKEN_HASH_ALGORITHM + " is not available", e);
        }
    }

    private static String cooldownKey(TokenPurpose purpose, String email) {
        return COOLDOWN_KEY_PREFIX + purpose.name().toLowerCase(Locale.ROOT) + ":" + hashToken(email);
    }
}
