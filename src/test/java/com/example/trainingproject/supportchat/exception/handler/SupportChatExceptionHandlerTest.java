package com.example.trainingproject.supportchat.exception.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import com.example.trainingproject.common.exception.ProblemType;
import com.example.trainingproject.common.exception.handler.ProblemDetailFactory;
import com.example.trainingproject.common.exception.handler.ProblemTypeUriFactory;
import com.example.trainingproject.common.turnstile.TurnstileVerificationException;
import com.example.trainingproject.supportchat.exception.DuplicateSupportChatMessageException;
import com.example.trainingproject.supportchat.exception.InvalidSupportChatMessageException;
import com.example.trainingproject.supportchat.exception.SupportChatAccessRestrictedException;
import com.example.trainingproject.supportchat.exception.SupportChatConversationNotFoundException;
import com.example.trainingproject.supportchat.exception.SupportChatDisabledException;
import com.example.trainingproject.supportchat.exception.SupportChatEmailVerificationRequiredException;
import com.example.trainingproject.supportchat.exception.SupportChatOwnerDeliveryFailedException;
import com.example.trainingproject.supportchat.exception.SupportChatRateLimitExceededException;

@DisplayName("SupportChatExceptionHandler unit tests")
class SupportChatExceptionHandlerTest {

    private final SupportChatExceptionHandler handler = new SupportChatExceptionHandler(
            new ProblemDetailFactory(new ProblemTypeUriFactory("https://example.test/training-project/errors")));

    @Test
    @DisplayName("Maps disabled support chat to 404")
    void disabled_mapsToNotFound() {
        var response = handler.handleSupportChatException(new SupportChatDisabledException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertProblemType(response, ProblemType.SUPPORT_CHAT_DISABLED);
    }

    @Test
    @DisplayName("Maps email verification requirement to 403")
    void emailVerificationRequired_mapsToForbidden() {
        var response = handler.handleSupportChatException(new SupportChatEmailVerificationRequiredException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertProblemType(response, ProblemType.SUPPORT_CHAT_EMAIL_VERIFICATION_REQUIRED);
    }

    @Test
    @DisplayName("Maps restricted support chat access to 403")
    void accessRestricted_mapsToForbidden() {
        var response = handler.handleSupportChatException(new SupportChatAccessRestrictedException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertProblemType(response, ProblemType.SUPPORT_CHAT_ACCESS_RESTRICTED);
    }

    @Test
    @DisplayName("Maps conversation not found to 404")
    void conversationNotFound_mapsToNotFound() {
        var response = handler.handleSupportChatException(new SupportChatConversationNotFoundException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertProblemType(response, ProblemType.SUPPORT_CHAT_CONVERSATION_NOT_FOUND);
    }

    @Test
    @DisplayName("Maps invalid messages to 400")
    void invalidMessage_mapsToBadRequest() {
        var response =
                handler.handleSupportChatException(new InvalidSupportChatMessageException("Message body is too long."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertProblemType(response, ProblemType.SUPPORT_CHAT_INVALID_MESSAGE);
    }

    @Test
    @DisplayName("Maps duplicate messages to 409")
    void duplicateMessage_mapsToConflict() {
        var response = handler.handleSupportChatException(new DuplicateSupportChatMessageException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertProblemType(response, ProblemType.SUPPORT_CHAT_DUPLICATE_MESSAGE);
    }

    @Test
    @DisplayName("Maps support chat rate limits to 429")
    void rateLimited_mapsToTooManyRequests() {
        var response = handler.handleSupportChatException(new SupportChatRateLimitExceededException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertProblemType(response, ProblemType.SUPPORT_CHAT_RATE_LIMITED);
    }

    @Test
    @DisplayName("Maps owner delivery failure to generic temporary unavailability")
    void ownerDeliveryFailed_mapsToServiceUnavailable() {
        var response = handler.handleSupportChatException(new SupportChatOwnerDeliveryFailedException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertProblemType(response, ProblemType.SUPPORT_CHAT_TEMPORARILY_UNAVAILABLE);
    }

    @Test
    @DisplayName("Maps Turnstile failures to support chat verification problem")
    void turnstileFailure_mapsToSupportChatVerificationProblem() {
        var response =
                handler.handleTurnstileVerificationException(new TurnstileVerificationException("token rejected"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertProblemType(response, ProblemType.SUPPORT_CHAT_TURNSTILE_FAILED);
    }

    private static void assertProblemType(ResponseEntity<ProblemDetail> response, String expectedTypeSlug) {
        ProblemDetail problemDetail = Objects.requireNonNull(response.getBody());

        assertThat(problemDetail.getType())
                .isEqualTo(URI.create("https://example.test/training-project/errors/" + expectedTypeSlug));
    }
}
