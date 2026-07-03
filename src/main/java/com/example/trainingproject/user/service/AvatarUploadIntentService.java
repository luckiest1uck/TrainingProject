package com.example.trainingproject.user.service;

import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.example.trainingproject.common.exception.BadRequestException;
import com.example.trainingproject.common.turnstile.TurnstileProperties;
import com.example.trainingproject.common.turnstile.TurnstileVerificationRequest;
import com.example.trainingproject.common.turnstile.TurnstileVerifier;
import com.example.trainingproject.openapi.dto.AvatarUploadIntentResponse;
import com.example.trainingproject.openapi.dto.AvatarUploadStatus;
import com.example.trainingproject.openapi.dto.AvatarUploadStatusResponse;
import com.example.trainingproject.openapi.dto.CreateAvatarUploadRequest;
import com.example.trainingproject.user.config.AvatarUploadMode;
import com.example.trainingproject.user.config.AvatarUploadProperties;
import com.example.trainingproject.user.entity.UserAvatarUpload;
import com.example.trainingproject.user.exception.InvalidAvatarFileTypeException;
import com.example.trainingproject.user.exception.UserAvatarUploadException;
import com.example.trainingproject.user.repository.UserAvatarUploadRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvatarUploadIntentService {

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;

    private final AvatarUploadProperties properties;
    private final AvatarUploadLifecycleService lifecycleService;
    private final UserAvatarUploadRepository repository;
    private final ObjectProvider<AvatarUploadPresigner> presignerProvider;
    private final TurnstileVerifier turnstileVerifier;
    private final TurnstileProperties turnstileProperties;

    public AvatarUploadIntentResponse createUploadIntent(
            UUID userId,
            @Nullable CreateAvatarUploadRequest request,
            @Nullable String idempotencyKey,
            @Nullable String remoteIp) {
        if (properties.uploadMode() != AvatarUploadMode.PRESIGNED) {
            log.info("avatar.upload_intent.rejected: reason=backend_mode, userId={}", userId);
            throw new UserAvatarUploadException(userId, "avatar-upload-intent");
        }
        AvatarUploadPresigner presigner = presignerProvider.getIfAvailable();
        if (presigner == null) {
            log.info("avatar.upload_intent.rejected: reason=presigner_unavailable, userId={}", userId);
            throw new UserAvatarUploadException(userId, "avatar-upload-intent");
        }
        String validatedIdempotencyKey = validateIdempotencyKey(idempotencyKey);
        CreateAvatarUploadRequest validatedRequest = validateRequest(userId, request);
        verifyTurnstile(validatedRequest.getTurnstileToken(), remoteIp);

        UserAvatarUpload upload = lifecycleService.createPendingUpload(
                userId, contentType(validatedRequest), validatedRequest.getSizeBytes(), validatedIdempotencyKey);
        return new AvatarUploadIntentResponse(
                        upload.getId(), status(upload), upload.getExpiresAt().atOffset(java.time.ZoneOffset.UTC))
                .upload(presigner.presign(upload));
    }

    public Optional<AvatarUploadStatusResponse> findUploadStatus(UUID userId, UUID uploadId) {
        return repository
                .findById(uploadId)
                .filter(upload -> upload.getUserId().equals(userId))
                .map(upload -> new AvatarUploadStatusResponse(
                                upload.getId(),
                                status(upload),
                                upload.getExpiresAt().atOffset(java.time.ZoneOffset.UTC))
                        .failureCode(upload.getFailureCode()));
    }

    public void cancelUpload(UUID userId, UUID uploadId) {
        lifecycleService.cancelUpload(userId, uploadId);
    }

    private String validateIdempotencyKey(@Nullable String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Idempotency-Key header is required and must not be blank.");
        }
        if (idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new BadRequestException("Idempotency-Key must be at most 100 characters.");
        }
        return idempotencyKey;
    }

    private CreateAvatarUploadRequest validateRequest(UUID userId, @Nullable CreateAvatarUploadRequest request) {
        if (request == null || request.getContentType() == null || request.getSizeBytes() == null) {
            throw new BadRequestException("Avatar upload contentType and sizeBytes are required.");
        }
        String contentType = contentType(request);
        if (!AvatarContentTypes.ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidAvatarFileTypeException(contentType, AvatarContentTypes.ALLOWED_CONTENT_TYPES);
        }
        if (request.getSizeBytes() < 1) {
            throw new BadRequestException("Avatar upload sizeBytes must be positive.");
        }
        if (request.getSizeBytes() > properties.maxBytes()) {
            log.warn("avatar.upload_intent.rejected: reason=file_too_large, userId={}", userId);
            throw new BadRequestException("Avatar upload must be at most " + properties.maxBytes() + " bytes.");
        }
        return request;
    }

    private void verifyTurnstile(@Nullable String turnstileToken, @Nullable String remoteIp) {
        if (turnstileProperties.avatarEnabled()) {
            turnstileVerifier.verify(TurnstileVerificationRequest.forAction(turnstileToken, remoteIp, "avatar"));
        }
    }

    private String contentType(CreateAvatarUploadRequest request) {
        return request.getContentType().getValue();
    }

    private AvatarUploadStatus status(UserAvatarUpload upload) {
        return AvatarUploadStatus.fromValue(upload.getStatus().name());
    }
}
