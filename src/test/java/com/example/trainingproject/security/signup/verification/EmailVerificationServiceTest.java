package com.example.trainingproject.security.signup.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.trainingproject.openapi.dto.UserRegistrationRequest;
import com.example.trainingproject.security.email.sender.AuthTokenEmailSender;
import com.example.trainingproject.security.jwt.config.JwtProperties;
import com.example.trainingproject.security.service.cache.ExpiringKeyValueStore;
import com.example.trainingproject.security.service.cache.InMemoryExpiringKeyValueStore;
import com.example.trainingproject.security.session.management.AuthSessionRequestMetadata;
import com.example.trainingproject.security.session.token.AuthenticationTokens;
import com.example.trainingproject.security.signin.exception.UserRegistrationException;
import com.example.trainingproject.security.signup.exception.TimeTokenException;
import com.example.trainingproject.security.signup.registration.UserRegistrationService;
import com.example.trainingproject.user.api.UserAccessControlApi;
import com.example.trainingproject.user.api.UserLookupApi;
import com.example.trainingproject.user.api.dto.UserLookupSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailVerificationService unit tests")
class EmailVerificationServiceTest {

    @Mock
    private AuthTokenEmailSender emailConfirmation;

    @Mock
    private UserRegistrationService userRegistrationService;

    @Mock
    private UserLookupApi userLookupApi;

    @Mock
    private UserAccessControlApi userAccessControlApi;

    @Mock
    private PasswordEncoder passwordEncoder;

    private EmailVerificationService service;
    private EmailTokenService emailTokenService;
    private static final AuthSessionRequestMetadata REQUEST_METADATA =
            new AuthSessionRequestMetadata("TestAgent", "127.0.0.1");

    @BeforeEach
    void setUp() {
        emailTokenService = tokenService(
                new InMemoryExpiringKeyValueStore(new com.example.trainingproject.common.config.CaffeineSizeProperties(
                        1_000, 5_000, 10_000, 1_000, 10_000)));
        service = new EmailVerificationService(
                emailConfirmation, emailTokenService, userRegistrationService, userLookupApi, userAccessControlApi);
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
    }

    private EmailTokenService tokenService(ExpiringKeyValueStore store) {
        ObjectMapper objectMapper = new ObjectMapper();
        return new EmailTokenService(
                store,
                passwordEncoder,
                tokenPayloadProtector(objectMapper),
                new EmailTokenProperties(43, ""),
                new TemporaryTokenProperties(new TemporaryTokenProperties.Time(15)));
    }

    @Nested
    @DisplayName("sendEmailVerificationCode")
    class SendEmailVerificationCode {

        @Test
        @DisplayName("stores verification token and sends it to request email")
        void storesVerificationTokenAndSendsItToRequestEmail() {
            UserRegistrationRequest request =
                    new UserRegistrationRequest("John", "Doe", "john@example.com", "pass123!");

            service.sendEmailVerificationCode(request);

            verify(userRegistrationService).ensureRegistrationAllowed(request, null);
            verify(emailConfirmation)
                    .sendTemporaryCode(eq("john@example.com"), argThat(EmailVerificationServiceTest::isOpaqueToken));
        }

        @Test
        @DisplayName("does not send token when email is already registered")
        void doesNotSendTokenWhenEmailIsAlreadyRegistered() {
            UserRegistrationRequest request =
                    new UserRegistrationRequest("John", "Doe", "john@example.com", "pass123!");
            doThrow(new UserRegistrationException("duplicate"))
                    .when(userRegistrationService)
                    .ensureRegistrationAllowed(request, null);

            assertThatThrownBy(() -> service.sendEmailVerificationCode(request))
                    .isInstanceOf(UserRegistrationException.class);

            verifyNoInteractions(emailConfirmation);
        }
    }

    @Nested
    @DisplayName("sendPasswordResetCode")
    class SendPasswordResetCode {

        @Test
        @DisplayName("creates password reset request with email and sends generated token")
        void createsPasswordResetRequestWithEmailAndSendsGeneratedToken() {
            service.sendPasswordResetCode("user@example.com");

            verify(emailConfirmation)
                    .sendTemporaryCode(eq("user@example.com"), argThat(EmailVerificationServiceTest::isOpaqueToken));
        }
    }

    @Nested
    @DisplayName("confirmEmailByCode")
    class ConfirmEmailByCode {

        @Test
        @DisplayName("consumes email verification token and registers user")
        void consumesEmailVerificationTokenAndRegistersUser() {
            UserRegistrationRequest registrationRequest =
                    new UserRegistrationRequest("John", "Doe", "john@example.com", "pass!");
            AuthenticationTokens authResponse = new AuthenticationTokens("access-token", "refresh-token");
            String token = emailTokenService.generateEmailVerificationToken(registrationRequest);
            when(userRegistrationService.completeEmailVerifiedRegistration(
                            argThat(request ->
                                    request.getEmail().equals("john@example.com") && request.getPassword() == null),
                            eq("encoded-password"),
                            eq(REQUEST_METADATA)))
                    .thenReturn(authResponse);

            AuthenticationTokens result = service.confirmEmailByCode(token, REQUEST_METADATA);

            assertThat(result).isSameAs(authResponse);
            verify(userRegistrationService)
                    .completeEmailVerifiedRegistration(
                            argThat(request ->
                                    request.getEmail().equals("john@example.com") && request.getPassword() == null),
                            eq("encoded-password"),
                            eq(REQUEST_METADATA));
        }
    }

    @Nested
    @DisplayName("confirmResetPasswordEmailByCode")
    class ConfirmResetPasswordEmailByCode {

        @Test
        @DisplayName("resolves user from password reset token and changes password")
        void resolvesUserFromPasswordResetTokenAndChangesPassword() {
            UUID userId = UUID.randomUUID();
            var user = new UserLookupSnapshot(userId, "Ada", "Lovelace", "user@example.com");
            String token = emailTokenService.generatePasswordResetToken("user@example.com");
            when(userLookupApi.getUserByEmail("user@example.com")).thenReturn(user);

            service.confirmResetPasswordEmailByCode(token, "newPass123!");

            verify(userLookupApi).getUserByEmail("user@example.com");
            verify(userAccessControlApi).changePassword(userId, "newPass123!");
        }
    }

    @Test
    @DisplayName("generateToken returns an opaque URL-safe token")
    void generateTokenReturnsOpaqueUrlSafeToken() {
        UserRegistrationRequest request =
                new UserRegistrationRequest("Alice", "Smith", "alice@example.com", "Password1!");

        String token = emailTokenService.generateEmailVerificationToken(request);

        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]{43}");
    }

    @Test
    @DisplayName("validateToken rejects invalid token format")
    void validateTokenRejectsInvalidTokenFormat() {
        assertThatThrownBy(() -> emailTokenService.consumeEmailVerificationToken("12345"))
                .isInstanceOf(com.example.trainingproject.common.exception.BadRequestException.class);
    }

    @Test
    @DisplayName("generateToken rejects weak token length configuration")
    void generateTokenRejectsWeakTokenLengthConfiguration() {
        assertThatThrownBy(() -> new EmailTokenProperties(9, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be at least 32");
    }

    @Test
    @DisplayName("email token properties reject missing token length")
    void emailTokenPropertiesRejectMissingTokenLength() {
        assertThatThrownBy(() -> new EmailTokenProperties(null, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email.verification-token-length must not be null");
    }

    @Test
    @DisplayName("temporary token properties reject missing time configuration")
    void temporaryTokenPropertiesRejectMissingTimeConfiguration() {
        assertThatThrownBy(() -> new TemporaryTokenProperties(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("temporary-cache.time must not be null");
    }

    @Test
    @DisplayName("temporary token properties reject missing token ttl")
    void temporaryTokenPropertiesRejectMissingTokenTtl() {
        assertThatThrownBy(() -> new TemporaryTokenProperties(new TemporaryTokenProperties.Time(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("temporary-cache.time.token must not be null");
    }

    @Test
    @DisplayName("generateToken normalizes email and scopes hashed token key by purpose")
    void generateTokenNormalizesEmailAndScopesHashedTokenKeyByPurpose() {
        ExpiringKeyValueStore store = mock(ExpiringKeyValueStore.class);
        EmailTokenService serviceWithMockStore = tokenServiceWithStore(store);
        UserRegistrationRequest request =
                new UserRegistrationRequest("Ada", "Lovelace", " User@Example.COM ", "Password1!");
        when(store.putIfAbsent(
                        argThat(key -> key != null && key.startsWith("email:rate:")),
                        any(),
                        eq(Duration.ofMinutes(15))))
                .thenReturn(true);
        when(store.putIfAbsent(
                        argThat(key -> key.startsWith("email:token:email_verification:")),
                        argThat(value -> !value.contains("user@example.com")
                                && !value.contains("Ada")
                                && !value.contains("Lovelace")
                                && !value.contains("encoded-password")
                                && !value.contains("Password1!")),
                        eq(Duration.ofMinutes(15))))
                .thenReturn(true);

        String token = serviceWithMockStore.generateEmailVerificationToken(request);

        assertThat(request.getEmail()).isEqualTo(" User@Example.COM ");
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(store, times(2)).putIfAbsent(keyCaptor.capture(), valueCaptor.capture(), eq(Duration.ofMinutes(15)));
        int tokenEntryIndex = -1;
        for (int i = 0; i < keyCaptor.getAllValues().size(); i++) {
            if (keyCaptor.getAllValues().get(i).startsWith("email:token:email_verification:")) {
                tokenEntryIndex = i;
                break;
            }
        }
        assertThat(tokenEntryIndex).isGreaterThanOrEqualTo(0);
        assertThat(keyCaptor.getAllValues().get(tokenEntryIndex)).doesNotEndWith(token);
        assertThat(valueCaptor.getAllValues().get(tokenEntryIndex))
                .doesNotContain("user@example.com")
                .doesNotContain("Ada")
                .doesNotContain("Lovelace")
                .doesNotContain("encoded-password")
                .doesNotContain("Password1!");
        verify(store)
                .putIfAbsent(
                        argThat(key -> key.startsWith("email:rate:") && !key.endsWith("user@example.com")),
                        any(),
                        eq(Duration.ofMinutes(15)));
    }

    @Test
    @DisplayName("generateToken retries instead of overwriting an existing same-purpose token")
    void generateTokenRetriesInsteadOfOverwritingExistingSamePurposeToken() {
        ExpiringKeyValueStore store = mock(ExpiringKeyValueStore.class);
        EmailTokenService serviceWithMockStore = tokenServiceWithStore(store);
        UserRegistrationRequest request =
                new UserRegistrationRequest("Ada", "Lovelace", "user@example.com", "Password1!");
        when(store.putIfAbsent(
                        argThat(key -> key != null && key.startsWith("email:rate:")),
                        any(),
                        eq(Duration.ofMinutes(15))))
                .thenReturn(true);
        when(store.putIfAbsent(
                        argThat(key -> key.startsWith("email:token:password_reset:")),
                        any(),
                        eq(Duration.ofMinutes(15))))
                .thenReturn(false)
                .thenReturn(true);

        serviceWithMockStore.generatePasswordResetToken(request.getEmail());

        verify(store, times(2))
                .putIfAbsent(
                        argThat(key -> key.startsWith("email:token:password_reset:")),
                        any(),
                        eq(Duration.ofMinutes(15)));
        verify(store)
                .putIfAbsent(
                        argThat(key -> key.startsWith("email:rate:") && !key.endsWith("user@example.com")),
                        any(),
                        eq(Duration.ofMinutes(15)));
    }

    @Test
    @DisplayName("generateToken reserves cooldown atomically before generating verification tokens")
    void generateTokenReservesCooldownAtomicallyBeforeGeneratingVerificationTokens() {
        ExpiringKeyValueStore store = mock(ExpiringKeyValueStore.class);
        EmailTokenService serviceWithMockStore = tokenServiceWithStore(store);
        UserRegistrationRequest request =
                new UserRegistrationRequest("Ada", "Lovelace", "user@example.com", "Password1!");
        OffsetDateTime expiry = OffsetDateTime.now().plusMinutes(15);
        when(store.putIfAbsent(
                        argThat(key -> key != null && key.startsWith("email:rate:")),
                        any(),
                        eq(Duration.ofMinutes(15))))
                .thenReturn(false);
        when(store.get(argThat(key -> key != null && key.startsWith("email:rate:"))))
                .thenReturn(Optional.of(expiry.toString()));

        assertThatThrownBy(() -> serviceWithMockStore.generateEmailVerificationToken(request))
                .isInstanceOf(TimeTokenException.class);

        verify(store)
                .putIfAbsent(
                        argThat(key -> key != null && key.startsWith("email:rate:")),
                        any(),
                        eq(Duration.ofMinutes(15)));
        verify(passwordEncoder, never()).encode(anyString());
        verify(store, never())
                .putIfAbsent(
                        argThat(key -> key != null && key.startsWith("email:token:email_verification:")), any(), any());
    }

    @Test
    @DisplayName("generateToken removes reserved cooldown when token allocation fails")
    void generateTokenRemovesReservedCooldownWhenTokenAllocationFails() {
        ExpiringKeyValueStore store = mock(ExpiringKeyValueStore.class);
        EmailTokenService serviceWithMockStore = tokenServiceWithStore(store);
        UserRegistrationRequest request =
                new UserRegistrationRequest("Ada", "Lovelace", "user@example.com", "Password1!");
        when(store.putIfAbsent(
                        argThat(key -> key != null && key.startsWith("email:rate:")),
                        any(),
                        eq(Duration.ofMinutes(15))))
                .thenReturn(true);
        when(store.putIfAbsent(
                        argThat(key -> key != null && key.startsWith("email:token:email_verification:")),
                        any(),
                        eq(Duration.ofMinutes(15))))
                .thenReturn(false);

        assertThatThrownBy(() -> serviceWithMockStore.generateEmailVerificationToken(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("allocate unique email token");

        verify(store).remove(argThat(key -> key != null && key.startsWith("email:rate:")));
    }

    private EmailTokenService tokenServiceWithStore(ExpiringKeyValueStore store) {
        ObjectMapper objectMapper = new ObjectMapper();
        return new EmailTokenService(
                store,
                passwordEncoder,
                tokenPayloadProtector(objectMapper),
                new EmailTokenProperties(43, ""),
                new TemporaryTokenProperties(new TemporaryTokenProperties.Time(15)));
    }

    private static EmailTokenPayloadProtector tokenPayloadProtector(ObjectMapper objectMapper) {
        return new EmailTokenPayloadProtector(objectMapper, jwtProperties(), new EmailTokenProperties(43, ""));
    }

    private static JwtProperties jwtProperties() {
        return new JwtProperties(
                "Authorization",
                "NDA0RTYzNTI2NjU1NkE1ODZFMzI3MjM1NzUzODc4MkY0MTNBNDQ0Mjg0NzJCNEI2MjUwNjQ1MzY3NTY2QjU5NzA=",
                "NDA0RTYzNTI2NjU1NkE1ODZFMzI3MjM1NzUzODc4MkY0MTNBNDQ0Mjg0NzJCNEI2MjUwNjQ1MzY3NTY2QjU5NzA0MDRFNTM1MjY2NTU2QTU4NkUzMjcyMzU3NTM4NzgyRjQxM0E0NDQyODQ3MkI0QjYyNTA2NDUzNjc1NjZCNTk3MA==",
                Duration.ofMinutes(30),
                Duration.ofHours(24),
                "training-project",
                "training-project-users",
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private static boolean isOpaqueToken(String token) {
        return token != null && token.matches("[A-Za-z0-9_-]{43}");
    }
}
